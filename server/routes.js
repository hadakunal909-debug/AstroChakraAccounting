// All API routes. Mounted at /api by app.js. Mirrors src/api.js (the agreed
// contract) and the validated Java backend's business logic.
//
// Auth model (same as the Java SecurityConfig):
//   public:        /health, /auth/login
//   admin only:    /users/**
//   everything else: requires a valid Bearer token
const express = require('express');
const bcrypt = require('bcryptjs');
const { q, withTx } = require('./db');
const { sign, requireAuth, requireAdmin, login } = require('./auth');

const router = express.Router();

// ---- small helpers -------------------------------------------------------
const h = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
const httpError = (status, message) => Object.assign(new Error(message), { status });
const pc = (v) => (v != null && String(v).trim() !== '' ? v : null); // blank -> null
const num = (v) => (v == null || v === '' ? 0 : Number(v));
const isProjectFund = (fund) => fund != null && fund !== 'available' && fund !== 'reserve';

// ===== HEALTH (public) ====================================================
router.get(
  '/health',
  h(async (_req, res) => {
    let db = false;
    try {
      await q('select 1');
      db = true;
    } catch (_) {
      db = false;
    }
    res.json({ status: db ? 'ok' : 'degraded', db });
  })
);

// ===== AUTH ===============================================================
router.post(
  '/auth/login',
  h(async (req, res) => {
    const { username, password } = req.body || {};
    const u = await login(username, password);
    if (!u) throw httpError(401, 'Invalid username or password');
    const user = {
      id: u.id,
      username: u.username,
      display_name: u.display_name,
      email: u.email,
      role: u.role,
    };
    res.json({ token: sign(u), user });
  })
);

// Everything below requires authentication.
router.use(requireAuth);

router.get(
  '/auth/me',
  h(async (req, res) => {
    const { rows } = await q(
      'select id, username, display_name, email, role, is_active, last_login, created_at from app_users where username = $1',
      [req.user.sub]
    );
    if (!rows[0]) throw httpError(401, 'Unauthorized');
    res.json(rows[0]);
  })
);

// ===== USERS (admin only) =================================================
router.get(
  '/users',
  requireAdmin,
  h(async (_req, res) => {
    const { rows } = await q(
      'select id, username, display_name, email, role, is_active, last_login, created_at from app_users order by created_at'
    );
    res.json(rows);
  })
);
router.post(
  '/users',
  requireAdmin,
  h(async (req, res) => {
    const b = req.body || {};
    const hash = await bcrypt.hash(String(b.password || ''), 10);
    const { rows } = await q(
      `insert into app_users (username, password_hash, display_name, email, role, is_active)
       values ($1,$2,$3,$4,$5,true)
       returning id, username, display_name, email, role, is_active, last_login, created_at`,
      [b.username, hash, b.display_name || '', b.email || '', b.role || 'regular']
    );
    res.status(201).json(rows[0]);
  })
);
router.put(
  '/users/:id',
  requireAdmin,
  h(async (req, res) => {
    const b = req.body || {};
    const sets = ['updated_at = now()'];
    const vals = [];
    let i = 1;
    if (b.display_name !== undefined) { sets.push(`display_name = $${i++}`); vals.push(b.display_name); }
    if (b.email !== undefined) { sets.push(`email = $${i++}`); vals.push(b.email); }
    if (b.role !== undefined) { sets.push(`role = $${i++}`); vals.push(b.role); }
    if (b.is_active !== undefined) { sets.push(`is_active = $${i++}`); vals.push(b.is_active); }
    if (b.password) { sets.push(`password_hash = $${i++}`); vals.push(await bcrypt.hash(String(b.password), 10)); }
    vals.push(req.params.id);
    const { rows } = await q(
      `update app_users set ${sets.join(', ')} where id = $${i}
       returning id, username, display_name, email, role, is_active, last_login, created_at`,
      vals
    );
    res.json(rows[0] || null);
  })
);
router.delete(
  '/users/:id',
  requireAdmin,
  h(async (req, res) => {
    await q('delete from app_users where id = $1', [req.params.id]);
    res.status(204).end();
  })
);

// ===== BALANCE ============================================================
router.get(
  '/balance',
  h(async (_req, res) => {
    const { rows } = await q('select * from balance order by id limit 1');
    res.json(rows[0] || { balance: 0, liquid_reserve: 0 });
  })
);
router.put(
  '/balance',
  h(async (req, res) => {
    const { balance, liquid_reserve } = req.body || {};
    const ex = await q('select id from balance order by id limit 1');
    if (ex.rows.length) {
      const sets = ['updated_at = now()'];
      const vals = [];
      let i = 1;
      if (balance !== null && balance !== undefined) { sets.push(`balance = $${i++}`); vals.push(balance); }
      if (liquid_reserve !== null && liquid_reserve !== undefined) { sets.push(`liquid_reserve = $${i++}`); vals.push(liquid_reserve); }
      vals.push(ex.rows[0].id);
      await q(`update balance set ${sets.join(', ')} where id = $${i}`, vals);
    } else {
      await q('insert into balance (balance, liquid_reserve, updated_at) values ($1,$2,now())', [balance || 0, liquid_reserve || 0]);
    }
    res.status(204).end();
  })
);

// ===== PROJECTS ===========================================================
router.get('/projects', h(async (_req, res) => {
  const { rows } = await q('select * from projects order by created_at');
  res.json(rows);
}));
router.post('/projects', h(async (req, res) => {
  const b = req.body || {};
  const { rows } = await q(
    `insert into projects (code, name, fixed_budget, allocated, color)
     values ($1,$2,$3,$4,$5) returning *`,
    [b.code, b.name, num(b.fixed_budget), num(b.allocated), b.color || '#6366f1']
  );
  res.status(201).json(rows[0]);
}));
router.put('/projects/:id', h(async (req, res) => {
  const b = req.body || {};
  await q(
    `update projects set name=$1, fixed_budget=$2, allocated=$3, color=$4, updated_at=now() where id=$5`,
    [b.name, num(b.fixed_budget), num(b.allocated), b.color, req.params.id]
  );
  res.status(204).end();
}));
router.delete('/projects/:id', h(async (req, res) => {
  await q('delete from projects where id = $1', [req.params.id]);
  res.status(204).end();
}));

// ===== PEOPLE =============================================================
router.get('/people', h(async (_req, res) => {
  const { rows } = await q('select * from people order by created_at');
  res.json(rows);
}));
router.post('/people', h(async (req, res) => {
  const b = req.body || {};
  const { rows } = await q(
    'insert into people (name, role) values ($1,$2) returning *',
    [b.name, b.role || 'Member']
  );
  res.status(201).json(rows[0]);
}));
router.put('/people/:id', h(async (req, res) => {
  const b = req.body || {};
  await q('update people set name=$1, role=$2, updated_at=now() where id=$3', [b.name, b.role, req.params.id]);
  res.status(204).end();
}));
router.delete('/people/:id', h(async (req, res) => {
  await q('delete from people where id = $1', [req.params.id]);
  res.status(204).end();
}));

// ===== INCOME SOURCES =====================================================
router.get('/income-sources', h(async (_req, res) => {
  const { rows } = await q('select * from income_sources order by created_at');
  res.json(rows);
}));
router.post('/income-sources', h(async (req, res) => {
  const { rows } = await q('insert into income_sources (name) values ($1) returning *', [(req.body || {}).name]);
  res.status(201).json(rows[0]);
}));

// ===== EXPENSE CATEGORIES =================================================
router.get('/expense-categories', h(async (_req, res) => {
  const { rows } = await q('select * from expense_categories order by name');
  res.json(rows);
}));
router.post('/expense-categories', h(async (req, res) => {
  const { rows } = await q('insert into expense_categories (name) values ($1) returning *', [(req.body || {}).name]);
  res.status(201).json(rows[0]);
}));
router.delete('/expense-categories/:id', h(async (req, res) => {
  await q('delete from expense_categories where id = $1', [req.params.id]);
  res.status(204).end();
}));

// ===== TRANSACTIONS (CRUD / tag edits — no balance effect) ================
const TX_INSERT = `insert into transactions
  (date, project_code, person, amount, kind, category, note, bill_url, bill_name, settled, is_reversal, original_id, reverses_kind, fund_source)
  values (coalesce($1::date, current_date),$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14) returning *`;
function txParams(t) {
  return [
    t.date || null, pc(t.project_code), t.person, num(t.amount), t.kind,
    t.category || '', t.note || '', t.bill_url || null, t.bill_name || null,
    t.settled === true, t.is_reversal === true, t.original_id || null,
    t.reverses_kind || null, t.fund_source || 'available',
  ];
}
async function insertTx(client, t) {
  const { rows } = await client.query(TX_INSERT, txParams(t));
  return rows[0];
}

router.get('/transactions', h(async (_req, res) => {
  const { rows } = await q('select * from transactions order by created_at desc');
  res.json(rows);
}));
router.post('/transactions', h(async (req, res) => {
  const { rows } = await q(TX_INSERT, txParams(req.body || {}));
  res.status(201).json(rows[0]);
}));
router.post('/transactions/bulk', h(async (req, res) => {
  const rowsIn = Array.isArray(req.body) ? req.body : [];
  const out = await withTx(async (c) => {
    const saved = [];
    for (const t of rowsIn) saved.push(await insertTx(c, t));
    return saved;
  });
  res.status(201).json(out);
}));
router.post('/transactions/rename-person', h(async (req, res) => {
  const { old_name, new_name } = req.body || {};
  const r = await q('update transactions set person = $1 where person = $2', [new_name, old_name]);
  res.json({ updated: r.rowCount });
}));
router.put('/transactions/:id/settle', h(async (req, res) => {
  await q('update transactions set settled = true where id = $1', [req.params.id]);
  res.status(204).end();
}));
router.patch('/transactions/:id/fund-source', h(async (req, res) => {
  await q('update transactions set fund_source = $1 where id = $2', [(req.body || {}).fund_source, req.params.id]);
  res.status(204).end();
}));
router.patch('/transactions/:id', h(async (req, res) => {
  const b = req.body || {};
  await q(
    'update transactions set date=$1, person=$2, category=$3, project_code=$4, note=$5 where id=$6',
    [b.date, b.person, b.category || '', pc(b.project_code), b.note || '', req.params.id]
  );
  res.status(204).end();
}));

// ===== ATOMIC MONEY OPERATIONS ===========================================
// insert a tx AND apply the balance/reserve/allocation effect, on a tx client.
async function recordTx(c, t) {
  const tx = await insertTx(c, t);
  const b = await loadOrCreateBalance(c);
  const amt = num(t.amount);
  const fund = tx.fund_source;
  let balance = num(b.balance);
  let reserve = num(b.liquid_reserve);
  if (tx.kind === 'income') {
    balance += amt;
  } else {
    balance -= amt;
    if (fund === 'reserve') {
      reserve = Math.max(0, reserve - amt);
    } else if (isProjectFund(fund)) {
      await c.query('update projects set allocated = GREATEST(0, allocated - $1), updated_at = now() where code = $2', [amt, fund]);
    }
  }
  await c.query('update balance set balance=$1, liquid_reserve=$2, updated_at=now() where id=$3', [balance, reserve, b.id]);
  return tx;
}
async function loadOrCreateBalance(c) {
  const { rows } = await c.query('select * from balance order by id limit 1');
  if (rows.length) return rows[0];
  const ins = await c.query('insert into balance (balance, liquid_reserve, updated_at) values (0,0,now()) returning *');
  return ins.rows[0];
}

router.post('/money/transaction', h(async (req, res) => {
  const tx = await withTx((c) => recordTx(c, req.body || {}));
  res.status(201).json(tx);
}));

router.post('/money/transaction/:id/settle', h(async (req, res) => {
  const note = (req.body || {}).note;
  const jv = await withTx(async (c) => {
    const orig = (await c.query('select * from transactions where id = $1', [req.params.id])).rows[0];
    if (!orig) throw httpError(404, 'Transaction not found');
    await c.query('update transactions set settled = true where id = $1', [orig.id]);

    const noteText = 'REVERSAL: ' + (note && String(note).trim() ? note : String(orig.amount));
    const jvRow = await insertTx(c, {
      date: null, project_code: orig.project_code, person: orig.person, amount: orig.amount,
      kind: 'jv_reversal', category: 'JV', note: noteText,
      settled: false, is_reversal: true, original_id: orig.id, fund_source: orig.fund_source,
    });

    const b = await loadOrCreateBalance(c);
    const amt = num(orig.amount);
    const fund = orig.fund_source;
    let balance = num(b.balance);
    let reserve = num(b.liquid_reserve);
    if (orig.kind === 'income') {
      balance -= amt;
    } else if (fund === 'reserve') {
      balance += amt; reserve += amt;
    } else if (isProjectFund(fund)) {
      balance += amt;
      await c.query('update projects set allocated = allocated + $1, updated_at = now() where code = $2', [amt, fund]);
    } else {
      balance += amt;
    }
    await c.query('update balance set balance=$1, liquid_reserve=$2, updated_at=now() where id=$3', [balance, reserve, b.id]);
    return jvRow;
  });
  res.status(201).json(jv);
}));

router.post('/money/reconcile', h(async (req, res) => {
  const trueBalance = num((req.body || {}).true_balance);
  const result = await withTx(async (c) => {
    const upd = await c.query(
      `update transactions set fund_source = 'available'
       where settled is not true and is_reversal is not true
         and fund_source is not null and fund_source <> 'available'
         and kind <> 'income' and kind <> 'jv_reversal'
       returning id`
    );
    const b = await loadOrCreateBalance(c);
    const delta = trueBalance - num(b.balance);
    if (delta !== 0) {
      await insertTx(c, {
        date: null, person: '', amount: Math.abs(delta),
        kind: delta > 0 ? 'income' : 'general_expense', category: 'Adjustment',
        note: 'Bank reconciliation: set Total Balance to ' + trueBalance,
        settled: false, is_reversal: false, fund_source: 'available',
      });
    }
    await c.query('update balance set balance=$1, liquid_reserve=0, updated_at=now() where id=$2', [trueBalance, b.id]);
    return { normalized_count: upd.rowCount, adjustment: delta, new_balance: trueBalance };
  });
  res.json(result);
}));

router.post('/money/import', h(async (req, res) => {
  const body = req.body || {};
  const rowsIn = Array.isArray(body.rows) ? body.rows : [];
  const closing = body.closing_balance;
  const result = await withTx(async (c) => {
    let net = 0;
    let count = 0;
    for (const t of rowsIn) {
      const tx = await insertTx(c, t);
      count++;
      net += tx.kind === 'income' ? num(t.amount) : -num(t.amount);
    }
    const b = await loadOrCreateBalance(c);
    const newBal = closing !== null && closing !== undefined ? num(closing) : num(b.balance) + net;
    await c.query('update balance set balance=$1, updated_at=now() where id=$2', [newBal, b.id]);
    return { imported: count, new_balance: newBal };
  });
  res.json(result);
}));

// ===== RESOURCES (HR / Payroll) ===========================================
function resourceParams(b) {
  return [
    b.name, b.role || '', b.pay_type, num(b.pay_amount),
    num(b.commission_percent), b.job_label || 'per job',
  ];
}
router.get('/resources', h(async (_req, res) => {
  const { rows } = await q('select * from resources order by created_at');
  res.json(rows);
}));
router.post('/resources', h(async (req, res) => {
  const b = req.body || {};
  const { rows } = await q(
    `insert into resources (name, role, pay_type, pay_amount, commission_percent, job_label, is_active)
     values ($1,$2,$3,$4,$5,$6,true) returning *`,
    resourceParams(b)
  );
  res.status(201).json(rows[0]);
}));
router.put('/resources/:id', h(async (req, res) => {
  const b = req.body || {};
  await q(
    `update resources set name=$1, role=$2, pay_type=$3, pay_amount=$4, commission_percent=$5, job_label=$6, updated_at=now() where id=$7`,
    [...resourceParams(b), req.params.id]
  );
  res.status(204).end();
}));
router.delete('/resources/:id', h(async (req, res) => {
  await q('update resources set is_active=false, updated_at=now() where id=$1', [req.params.id]);
  res.status(204).end();
}));

// ===== SALARY PAYMENTS (atomic: expense tx + balance effect + log) ========
router.get('/salary-payments', h(async (_req, res) => {
  const { rows } = await q('select * from salary_payments order by created_at desc');
  res.json(rows);
}));
router.post('/salary-payments', h(async (req, res) => {
  const r = req.body || {};
  const fund = r.fund_source || 'available';
  const note = r.note || '';
  const sp = await withTx(async (c) => {
    const txNote = 'Salary: ' + r.resource_name + (note.trim() ? ' — ' + note : '');
    await recordTx(c, {
      date: null,
      project_code: isProjectFund(fund) ? fund : null,
      person: r.resource_name,
      amount: r.amount,
      kind: 'project_expense',
      category: 'Salary',
      note: txNote,
      fund_source: fund,
    });
    const { rows } = await c.query(
      `insert into salary_payments (resource_id, resource_name, pay_type, amount, units, fund_source, fund_label, period, note, paid_by)
       values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) returning *`,
      [r.resource_id, r.resource_name, r.pay_type, num(r.amount), r.units != null ? num(r.units) : null,
       fund, r.fund_label || 'Available', r.period || '', note, r.paid_by || '']
    );
    return rows[0];
  });
  res.status(201).json(sp);
}));

// ===== COMPANY INFO =======================================================
router.get('/company', h(async (_req, res) => {
  const { rows } = await q('select * from company_info order by id limit 1');
  res.json(rows[0] || { name: 'Astrochakra' });
}));
router.put('/company', h(async (req, res) => {
  const b = req.body || {};
  const vals = [b.name || '', b.address || '', b.gstin || '', b.email || '', b.phone || ''];
  const ex = await q('select id from company_info order by id limit 1');
  if (ex.rows.length) {
    await q('update company_info set name=$1, address=$2, gstin=$3, email=$4, phone=$5, updated_at=now() where id=$6', [...vals, ex.rows[0].id]);
  } else {
    await q('insert into company_info (name, address, gstin, email, phone) values ($1,$2,$3,$4,$5)', vals);
  }
  res.status(204).end();
}));

// ===== ACTIVITY LOG =======================================================
router.get('/activity-log', h(async (req, res) => {
  const limit = Math.max(1, Math.min(1000, parseInt(req.query.limit, 10) || 10));
  const { rows } = await q('select * from activity_log order by created_at desc limit $1', [limit]);
  res.json(rows);
}));
router.post('/activity-log', h(async (req, res) => {
  const b = req.body || {};
  await q(
    `insert into activity_log (user_name, user_role, action, entity_type, entity_id, description)
     values ($1,$2,$3,$4,$5,$6)`,
    [b.user_name, b.user_role, b.action, b.entity_type || '', b.entity_id || null, b.description || '']
  );
  res.status(204).end();
}));

// ===== REMOVALS ===========================================================
router.get('/removals', h(async (_req, res) => {
  const { rows } = await q('select * from removals order by created_at desc');
  res.json(rows);
}));
router.post('/removals', h(async (req, res) => {
  const b = req.body || {};
  await q(
    `insert into removals (date, time, type, label, detail, reason) values ($1,$2,$3,$4,$5,$6)`,
    [b.date, b.time, b.type, b.label, b.detail || '', b.reason || '']
  );
  res.status(204).end();
}));

// ===== CHANGE REQUESTS ====================================================
router.get('/change-requests', h(async (_req, res) => {
  const { rows } = await q('select * from change_requests order by created_at desc');
  res.json(rows);
}));
router.get('/change-requests/mine', h(async (req, res) => {
  const { rows } = await q('select * from change_requests where requested_by = $1 order by created_at desc', [req.query.username || '']);
  res.json(rows);
}));
router.post('/change-requests', h(async (req, res) => {
  const b = req.body || {};
  const { rows } = await q(
    `insert into change_requests (requested_by, entity_type, entity_id, change_type, description)
     values ($1,$2,$3,$4,$5) returning *`,
    [b.requested_by, b.entity_type, b.entity_id || null, b.change_type, b.description]
  );
  res.status(201).json(rows[0]);
}));
router.put('/change-requests/:id/review', h(async (req, res) => {
  const b = req.body || {};
  await q(
    `update change_requests set status=$1, reviewed_by=$2, review_note=$3, reviewed_at=now() where id=$4`,
    [b.status, b.reviewed_by, b.note || '', req.params.id]
  );
  res.status(204).end();
}));

// ===== INVENTORY: categories ==============================================
router.get('/inventory/categories', h(async (_req, res) => {
  const { rows } = await q('select * from inventory_categories order by name');
  res.json(rows);
}));
router.post('/inventory/categories', h(async (req, res) => {
  const b = req.body || {};
  const { rows } = await q(
    'insert into inventory_categories (name, description, color) values ($1,$2,$3) returning *',
    [b.name, b.description || '', b.color || '#6366f1']
  );
  res.status(201).json(rows[0]);
}));
router.put('/inventory/categories/:id', h(async (req, res) => {
  const b = req.body || {};
  await q(
    'update inventory_categories set name=$1, description=$2, color=$3, updated_at=now() where id=$4',
    [b.name, b.description || '', b.color || '#6366f1', req.params.id]
  );
  res.status(204).end();
}));
router.delete('/inventory/categories/:id', h(async (req, res) => {
  await q('delete from inventory_categories where id = $1', [req.params.id]);
  res.status(204).end();
}));

// ===== INVENTORY: products ================================================
function invProductParams(b) {
  return [
    b.name, b.sku || '', b.category_id || null, b.category_name || '', b.unit || 'pcs',
    num(b.buying_price), num(b.selling_price), num(b.min_stock), b.description || '',
    b.hsn_code || '', b.tax_percent != null ? num(b.tax_percent) : 18, b.product_type || 'Product',
  ];
}
router.get('/inventory/products', h(async (_req, res) => {
  const { rows } = await q('select * from inventory_products order by created_at desc');
  res.json(rows);
}));
router.post('/inventory/products', h(async (req, res) => {
  const { rows } = await q(
    `insert into inventory_products
       (name, sku, category_id, category_name, unit, buying_price, selling_price, min_stock, description, hsn_code, tax_percent, product_type, current_stock, is_active)
     values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,0,true) returning *`,
    invProductParams(req.body || {})
  );
  res.status(201).json(rows[0]);
}));
router.put('/inventory/products/:id', h(async (req, res) => {
  await q(
    `update inventory_products set
       name=$1, sku=$2, category_id=$3, category_name=$4, unit=$5, buying_price=$6, selling_price=$7,
       min_stock=$8, description=$9, hsn_code=$10, tax_percent=$11, product_type=$12, updated_at=now()
     where id=$13`,
    [...invProductParams(req.body || {}), req.params.id]
  );
  res.status(204).end();
}));
router.put('/inventory/products/:id/stock', h(async (req, res) => {
  await q('update inventory_products set current_stock=$1, updated_at=now() where id=$2', [num((req.body || {}).current_stock), req.params.id]);
  res.status(204).end();
}));
router.delete('/inventory/products/:id', h(async (req, res) => {
  await q('update inventory_products set is_active=false, updated_at=now() where id=$1', [req.params.id]);
  res.status(204).end();
}));

// ===== INVENTORY: transactions / stock movement ===========================
router.get('/inventory/transactions', h(async (req, res) => {
  const limit = Math.max(1, Math.min(1000, parseInt(req.query.limit, 10) || 50));
  const { rows } = await q('select * from inventory_transactions order by created_at desc limit $1', [limit]);
  res.json(rows);
}));
router.get('/inventory/transactions/by-product/:productId', h(async (req, res) => {
  const { rows } = await q('select * from inventory_transactions where product_id = $1 order by created_at desc', [req.params.productId]);
  res.json(rows);
}));
// Atomic stock movement: stock_in also reduces the bank balance.
router.post('/inventory/stock-movement', h(async (req, res) => {
  const r = req.body || {};
  const it = await withTx(async (c) => {
    const p = (await c.query('select * from inventory_products where id = $1', [r.product_id])).rows[0];
    if (!p) throw httpError(404, 'Product not found');
    const qty = num(r.quantity);
    const type = r.type || 'stock_in';
    const total = num(r.total_cost);

    if (type === 'stock_in') {
      const fund = r.fund_source || 'available';
      const b = await loadOrCreateBalance(c);
      let balance = num(b.balance) - total;
      let reserve = num(b.liquid_reserve);
      if (fund === 'reserve') {
        reserve = Math.max(0, reserve - total);
      } else if (isProjectFund(fund)) {
        await c.query('update projects set allocated = GREATEST(0, allocated - $1), updated_at=now() where code=$2', [total, fund]);
      }
      await c.query('update balance set balance=$1, liquid_reserve=$2, updated_at=now() where id=$3', [balance, reserve, b.id]);
      await c.query('update inventory_products set current_stock = current_stock + $1, updated_at=now() where id=$2', [qty, p.id]);
    } else if (type === 'stock_out') {
      await c.query('update inventory_products set current_stock = GREATEST(0, current_stock - $1), updated_at=now() where id=$2', [qty, p.id]);
    } else {
      await c.query('update inventory_products set current_stock = $1, updated_at=now() where id=$2', [qty, p.id]);
    }

    const { rows } = await c.query(
      `insert into inventory_transactions
         (product_id, product_name, type, quantity, unit_cost, total_cost, fund_source, fund_label, notes, performed_by)
       values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) returning *`,
      [p.id, p.name, type, qty, r.unit_cost != null ? num(r.unit_cost) : null,
       type === 'stock_in' ? total : 0, type === 'stock_in' ? (r.fund_source || '') : '',
       r.fund_label || '', r.notes || '', r.performed_by || '']
    );
    return rows[0];
  });
  res.status(201).json(it);
}));

module.exports = router;
