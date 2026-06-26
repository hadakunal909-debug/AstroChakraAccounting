// API client for the Java (Spring Boot) backend.
//
// This is being built as a DROP-IN replacement for src/supabase.js: it exports
// the SAME function names so the call sites in App.jsx change as little as
// possible during the migration. Phase 0 covers balance; later phases add the
// rest of the domains (auth, transactions, projects, documents, inventory, ...).

const BASE = import.meta.env.VITE_API_BASE || '/api';

function token() {
  return localStorage.getItem('ac_token') || '';
}

async function req(path, { method = 'GET', body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token() ? { Authorization: 'Bearer ' + token() } : {}),
    },
    body: body != null ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) {
    // Session expired / not logged in — clear and let the auth flow redirect.
    localStorage.removeItem('ac_token');
    localStorage.removeItem('ac_user');
    throw new Error('Unauthorized');
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error('API ' + res.status + (text ? ': ' + text : ''));
  }
  if (res.status === 204) return null;
  const ct = res.headers.get('content-type') || '';
  return ct.includes('application/json') ? res.json() : res.text();
}

// ===== AUTH ===== (mirrors supabase.js loginUser + user management)
export async function loginUser(username, password) {
  const res = await req('/auth/login', { method: 'POST', body: { username, password } });
  if (res && res.token) localStorage.setItem('ac_token', res.token);
  const u = res && res.user;
  // Map to the camelCase shape App.jsx already expects from loginUser.
  return u ? { id: u.id, username: u.username, displayName: u.display_name, email: u.email, role: u.role } : null;
}
export function logoutUser() {
  localStorage.removeItem('ac_token');
}
export async function getUsers() {
  // Returns rows in the same snake_case shape the Users screen reads today.
  return req('/users');
}
export async function createUser(user) {
  return req('/users', {
    method: 'POST',
    body: { username: user.username, password: user.password, display_name: user.displayName, email: user.email, role: user.role || 'regular' },
  });
}
export async function updateUser(id, updates) {
  return req('/users/' + id, {
    method: 'PUT',
    body: { display_name: updates.displayName, email: updates.email, role: updates.role, is_active: updates.isActive, password: updates.password },
  });
}
export async function deleteUser(id) {
  return req('/users/' + id, { method: 'DELETE' });
}

// ===== BALANCE ===== (mirrors supabase.js getBalance / updateBalance)
export async function getBalance() {
  return req('/balance'); // -> { balance, liquid_reserve }
}
export async function updateBalance(balance, liquid) {
  // null means "leave unchanged" (same semantics as the Supabase version)
  return req('/balance', { method: 'PUT', body: { balance, liquid_reserve: liquid } });
}

// ===== PROJECTS =====
export async function getProjects() {
  return req('/projects'); // [{ id, code, name, fixed_budget, allocated, color, ... }]
}
export async function addProject(p) {
  return req('/projects', { method: 'POST', body: { code: p.code, name: p.name, fixed_budget: p.fixed, allocated: p.alloc, color: p.color } });
}
export async function updateProject(id, p) {
  return req('/projects/' + id, { method: 'PUT', body: { name: p.name, fixed_budget: p.fixed, allocated: p.alloc, color: p.color } });
}
export async function deleteProject(id) {
  return req('/projects/' + id, { method: 'DELETE' });
}

// ===== PEOPLE =====
export async function getPeople() {
  return req('/people');
}
export async function addPerson(p) {
  return req('/people', { method: 'POST', body: { name: p.name, role: p.role || 'Member' } });
}
export async function updatePerson(id, p) {
  return req('/people/' + id, { method: 'PUT', body: { name: p.name, role: p.role } });
}
export async function deletePerson(id) {
  return req('/people/' + id, { method: 'DELETE' });
}

// ===== INCOME SOURCES =====
export async function getIncomeSources() {
  return req('/income-sources');
}
export async function addIncomeSource(name) {
  return req('/income-sources', { method: 'POST', body: { name } });
}

// ===== EXPENSE CATEGORIES =====
export async function getExpenseCategories() {
  return req('/expense-categories');
}
export async function addExpenseCategory(name) {
  return req('/expense-categories', { method: 'POST', body: { name } });
}
export async function deleteExpenseCategory(id) {
  return req('/expense-categories/' + id, { method: 'DELETE' });
}

// ===== TRANSACTIONS =====
export async function getTransactions() {
  return req('/transactions'); // raw snake_case rows; App.jsx maps project_code/fund_source/etc.
}
export async function addTransaction(t) {
  return req('/transactions', {
    method: 'POST',
    body: {
      date: t.date, project_code: t.project || null, person: t.person, amount: t.amount,
      kind: t.kind, category: t.category || '', note: t.note || '',
      bill_url: t.billUrl || null, bill_name: t.billName || null,
      is_reversal: t.isReversal || false, original_id: t.originalId || null,
      reverses_kind: t.reversesKind || null, fund_source: t.fundSource || 'available',
    },
  });
}
export async function settleTransaction(id) {
  return req('/transactions/' + id + '/settle', { method: 'PUT' });
}
export async function updateTransactionsPerson(oldName, newName) {
  return req('/transactions/rename-person', { method: 'POST', body: { old_name: oldName, new_name: newName } });
}
export async function updateTransactionMeta(id, t) {
  return req('/transactions/' + id, {
    method: 'PATCH',
    body: { date: t.date, person: t.person, category: t.category || '', project_code: t.project || null, note: t.note || '' },
  });
}
export async function updateTransactionFundSource(id, fundSource) {
  return req('/transactions/' + id + '/fund-source', { method: 'PATCH', body: { fund_source: fundSource } });
}
export async function addTransactionsBulk(rows) {
  return req('/transactions/bulk', {
    method: 'POST',
    body: rows.map(t => ({
      date: t.date, project_code: t.project || null, person: t.person, amount: t.amount,
      kind: t.kind, category: t.category || '', note: t.note || '', fund_source: t.fundSource || 'available',
    })),
  });
}

// ===== ATOMIC MONEY OPERATIONS (single server transaction) =====
// These supersede the browser-side multi-call flows (addTx/doSettle/doReconcile/
// doBankImport). App.jsx will switch to these at cutover.
export async function recordTransaction(t) {
  // Insert + balance/reserve/allocation effect, all atomic. `kind` and `fund_source`
  // must already be resolved by the caller (income / project_expense / general_expense).
  return req('/money/transaction', {
    method: 'POST',
    body: {
      date: t.date, project_code: t.project || null, person: t.person, amount: t.amount,
      kind: t.kind, category: t.category || '', note: t.note || '', fund_source: t.fundSource || 'available',
    },
  });
}
export async function settleTransactionAtomic(id, note) {
  return req('/money/transaction/' + id + '/settle', { method: 'POST', body: { note: note || '' } });
}
export async function reconcileBank(trueBalance) {
  return req('/money/reconcile', { method: 'POST', body: { true_balance: trueBalance } });
}
export async function importBankStatement(rows, closingBalance) {
  return req('/money/import', {
    method: 'POST',
    body: {
      closing_balance: (closingBalance === '' || closingBalance == null) ? null : closingBalance,
      rows: rows.map(t => ({
        date: t.date, project_code: t.project || null, person: t.person, amount: t.amount,
        kind: t.kind, category: t.category || '', note: t.note || '', fund_source: 'available',
      })),
    },
  });
}

// ===== RESOURCES / PAYROLL =====
export async function getResources() {
  return req('/resources');
}
export async function addResource(r) {
  return req('/resources', {
    method: 'POST',
    body: { name: r.name, role: r.role, pay_type: r.payType, pay_amount: r.payAmount, commission_percent: r.commissionPercent, job_label: r.jobLabel },
  });
}
export async function updateResource(id, r) {
  return req('/resources/' + id, {
    method: 'PUT',
    body: { name: r.name, role: r.role, pay_type: r.payType, pay_amount: r.payAmount, commission_percent: r.commissionPercent, job_label: r.jobLabel },
  });
}
export async function deleteResource(id) {
  return req('/resources/' + id, { method: 'DELETE' });
}
export async function getSalaryPayments() {
  return req('/salary-payments');
}
// Atomic: records the salary expense (tx + balance effect) AND logs the payment.
export async function addSalaryPayment(p) {
  return req('/salary-payments', {
    method: 'POST',
    body: {
      resource_id: p.resourceId, resource_name: p.resourceName, pay_type: p.payType,
      amount: p.amount, units: p.units, fund_source: p.fundSource, fund_label: p.fundLabel,
      period: p.period, note: p.note, paid_by: p.paidBy,
    },
  });
}

// ===== PLATFORM: company info, activity log, removals, change requests =====
export async function getCompanyInfo() {
  return req('/company');
}
export async function updateCompanyInfo(info) {
  return req('/company', { method: 'PUT', body: { name: info.name, address: info.address, gstin: info.gstin, email: info.email, phone: info.phone } });
}
export async function getActivityLog(limit) {
  return req('/activity-log?limit=' + (limit || 10));
}
export async function logActivity(userName, userRole, action, entityType, entityId, description) {
  return req('/activity-log', {
    method: 'POST',
    body: { user_name: userName, user_role: userRole, action, entity_type: entityType || '', entity_id: entityId || null, description: description || '' },
  });
}
export async function getRemovals() {
  return req('/removals');
}
export async function addRemoval(r) {
  return req('/removals', { method: 'POST', body: { date: r.date, time: r.time, type: r.type, label: r.label, detail: r.detail, reason: r.reason } });
}
export async function getChangeRequests() {
  return req('/change-requests');
}
export async function getMyChangeRequests(username) {
  return req('/change-requests/mine?username=' + encodeURIComponent(username || ''));
}
export async function createChangeRequest(reqData) {
  return req('/change-requests', {
    method: 'POST',
    body: { requested_by: reqData.requestedBy, entity_type: reqData.entityType, entity_id: reqData.entityId || null, change_type: reqData.changeType, description: reqData.description },
  });
}
export async function reviewChangeRequest(id, status, reviewedBy, note) {
  return req('/change-requests/' + id + '/review', { method: 'PUT', body: { status, reviewed_by: reviewedBy, note: note || '' } });
}

// ===== INVENTORY =====
export async function getInventoryCategories() {
  return req('/inventory/categories');
}
export async function addInventoryCategory(cat) {
  return req('/inventory/categories', { method: 'POST', body: { name: cat.name, description: cat.description, color: cat.color } });
}
export async function updateInventoryCategory(id, cat) {
  return req('/inventory/categories/' + id, { method: 'PUT', body: { name: cat.name, description: cat.description, color: cat.color } });
}
export async function deleteInventoryCategory(id) {
  return req('/inventory/categories/' + id, { method: 'DELETE' });
}
function invProductBody(p) {
  return {
    name: p.name, sku: p.sku, category_id: p.categoryId || null, category_name: p.categoryName,
    unit: p.unit, buying_price: p.buyingPrice, selling_price: p.sellingPrice, min_stock: p.minStock,
    description: p.description, hsn_code: p.hsnCode, tax_percent: p.taxPercent, product_type: p.productType,
  };
}
export async function getInventoryProducts() {
  return req('/inventory/products');
}
export async function addInventoryProduct(p) {
  return req('/inventory/products', { method: 'POST', body: invProductBody(p) });
}
export async function updateInventoryProduct(id, p) {
  return req('/inventory/products/' + id, { method: 'PUT', body: invProductBody(p) });
}
export async function updateInventoryStock(id, newStock) {
  return req('/inventory/products/' + id + '/stock', { method: 'PUT', body: { current_stock: newStock } });
}
export async function deleteInventoryProduct(id) {
  return req('/inventory/products/' + id, { method: 'DELETE' });
}
export async function getInventoryTransactions(limit) {
  return req('/inventory/transactions?limit=' + (limit || 50));
}
export async function getInventoryTransactionsByProduct(productId) {
  return req('/inventory/transactions/by-product/' + productId);
}
// Atomic stock movement: stock_in also reduces the bank balance.
export async function stockMovement(m) {
  return req('/inventory/stock-movement', {
    method: 'POST',
    body: {
      product_id: m.productId, type: m.type, quantity: m.quantity, unit_cost: m.unitCost,
      total_cost: m.totalCost, fund_source: m.fundSource, fund_label: m.fundLabel,
      notes: m.notes, performed_by: m.performedBy,
    },
  });
}

// Non-atomic inventory transaction insert (mirrors supabase.addInventoryTransaction).
// The atomic stockMovement() supersedes the browser-side multi-call flow.
export async function addInventoryTransaction(t) {
  return req('/inventory/transactions', {
    method: 'POST',
    body: {
      product_id: t.productId, product_name: t.productName || '', type: t.type || 'stock_in',
      quantity: t.quantity, unit_cost: t.unitCost || 0, total_cost: t.totalCost || 0,
      fund_source: t.fundSource || '', fund_label: t.fundLabel || '',
      notes: t.notes || '', performed_by: t.performedBy || '',
    },
  });
}

// ===== PRODUCTS / PRODUCT TYPES (legacy — document item search) =====
function productBody(p) {
  return {
    name: p.name, product_type: p.productType, category: p.category || '', unit: p.unit || 'pcs',
    price: p.price || 0, hsn_code: p.hsnCode || '', tax_percent: p.taxPercent || 18, description: p.description || '',
  };
}
export async function getProducts() {
  return req('/products');
}
export async function addProduct(p) {
  return req('/products', { method: 'POST', body: productBody(p) });
}
export async function updateProduct(id, p) {
  return req('/products/' + id, { method: 'PUT', body: productBody(p) });
}
export async function deleteProduct(id) {
  return req('/products/' + id, { method: 'DELETE' });
}
export async function getProductTypes() {
  return req('/product-types');
}
export async function addProductType(name) {
  return req('/product-types', { method: 'POST', body: { name } });
}

// ===== DOCUMENTS (quotes / requisitions / POs / invoices) =====
// App.jsx passes objects with nested vendor/client and items [{desc,hsn,qty,rate,gst}];
// here we flatten to the snake_case shape the backend expects.
const mapItems = (items) => (items || []).map((it) => ({
  description: it.desc || 'Item', hsn: it.hsn || '', qty: it.qty, rate: it.rate, gst_percent: it.gst,
}));

// --- requisitions ---
export async function getRequisitions() {
  return req('/requisitions');
}
export async function addRequisition(r, items) {
  return req('/requisitions', {
    method: 'POST',
    body: {
      requisition: {
        number: r.number, date: r.date, requested_by: r.requestedBy,
        vendor_name: r.vendor.name, vendor_email: r.vendor.email || '', vendor_phone: r.vendor.phone || '',
        vendor_address: r.vendor.address || '', vendor_gstin: r.vendor.gstin || '',
        gst_type: r.type || 'intra', project_code: r.project || null,
        subtotal: r.subtotal, tax: r.tax, total: r.total, notes: r.notes || '',
      },
      items: mapItems(items),
    },
  });
}
export async function getRequisitionItems(reqId) {
  return req('/requisitions/' + reqId + '/items');
}
export async function updateRequisitionStatus(id, status, approvedBy) {
  return req('/requisitions/' + id + '/status', { method: 'PUT', body: { status, approved_by: approvedBy || null } });
}
export async function deleteRequisition(id) {
  return req('/requisitions/' + id, { method: 'DELETE' });
}

// --- purchase orders ---
export async function getPurchaseOrders() {
  return req('/purchase-orders');
}
export async function addPurchaseOrder(po, items) {
  return req('/purchase-orders', {
    method: 'POST',
    body: {
      purchase_order: {
        number: po.number, date: po.date, delivery_date: po.deliveryDate || null,
        vendor_name: po.vendor.name, vendor_email: po.vendor.email || '', vendor_phone: po.vendor.phone || '',
        vendor_address: po.vendor.address || '', vendor_gstin: po.vendor.gstin || '',
        gst_type: po.type || 'intra', project_code: po.project || null,
        subtotal: po.subtotal, tax: po.tax, total: po.total, notes: po.notes || '',
        requisition_id: po.requisitionId || null,
      },
      items: mapItems(items),
    },
  });
}
export async function getPOItems(poId) {
  return req('/purchase-orders/' + poId + '/items');
}
export async function updatePOStatus(id, status) {
  return req('/purchase-orders/' + id + '/status', { method: 'PUT', body: { status } });
}

// --- invoices ---
export async function getInvoices() {
  return req('/invoices');
}
export async function addInvoice(inv, items) {
  return req('/invoices', {
    method: 'POST',
    body: {
      invoice: {
        number: inv.number, date: inv.date, due_date: inv.dueDate || null,
        client_name: inv.client.name, client_email: inv.client.email || '', client_phone: inv.client.phone || '',
        client_address: inv.client.address || '', client_gstin: inv.client.gstin || '',
        gst_type: inv.type || 'intra', project_code: inv.project || null,
        subtotal: inv.subtotal, tax: inv.tax, total: inv.total, notes: inv.notes || '',
        status: inv.status || 'unpaid', po_id: inv.poId || null, locked: inv.locked || false,
        quote_id: inv.quoteId || null, source: inv.source || 'b2b', editable: inv.editable || false,
      },
      items: mapItems(items),
    },
  });
}
export async function getInvoiceItems(invoiceId) {
  return req('/invoices/' + invoiceId + '/items');
}
export async function markInvoicePaid(id) {
  return req('/invoices/' + id + '/paid', { method: 'PUT' });
}
export async function deleteInvoice(id) {
  return req('/invoices/' + id, { method: 'DELETE' });
}
export async function updateInvoice(id, inv, items) {
  return req('/invoices/' + id, {
    method: 'PUT',
    body: {
      invoice: {
        date: inv.date, due_date: inv.dueDate || null,
        client_name: inv.client.name, client_email: inv.client.email || '', client_phone: inv.client.phone || '',
        client_address: inv.client.address || '', client_gstin: inv.client.gstin || '',
        gst_type: inv.type || 'intra', project_code: inv.project || null,
        subtotal: inv.subtotal, tax: inv.tax, total: inv.total, notes: inv.notes || '',
      },
      items: items ? mapItems(items) : null,
    },
  });
}

// --- quotes ---
export async function getQuotes() {
  return req('/quotes');
}
export async function addQuote(qt, items) {
  return req('/quotes', {
    method: 'POST',
    body: {
      quote: {
        number: qt.number, date: qt.date, valid_until: qt.validUntil || null,
        client_name: qt.client.name, client_email: qt.client.email || '', client_phone: qt.client.phone || '',
        client_address: qt.client.address || '', client_gstin: qt.client.gstin || '',
        gst_type: qt.type || 'intra', project_code: qt.project || null,
        subtotal: qt.subtotal, tax: qt.tax, total: qt.total, notes: qt.notes || '', terms: qt.terms || '',
      },
      items: mapItems(items),
    },
  });
}
export async function getQuoteItems(quoteId) {
  return req('/quotes/' + quoteId + '/items');
}
export async function updateQuoteStatus(id, status) {
  return req('/quotes/' + id + '/status', { method: 'PUT', body: { status } });
}

export default {
  getBalance, updateBalance, loginUser, logoutUser, getUsers, createUser, updateUser, deleteUser,
  getProjects, addProject, updateProject, deleteProject,
  getPeople, addPerson, updatePerson, deletePerson,
  getIncomeSources, addIncomeSource, getExpenseCategories, addExpenseCategory, deleteExpenseCategory,
  getTransactions, addTransaction, settleTransaction, updateTransactionsPerson,
  updateTransactionMeta, updateTransactionFundSource, addTransactionsBulk,
  recordTransaction, settleTransactionAtomic, reconcileBank, importBankStatement,
  getResources, addResource, updateResource, deleteResource, getSalaryPayments, addSalaryPayment,
  getCompanyInfo, updateCompanyInfo, getActivityLog, logActivity, getRemovals, addRemoval,
  getChangeRequests, getMyChangeRequests, createChangeRequest, reviewChangeRequest,
  getInventoryCategories, addInventoryCategory, updateInventoryCategory, deleteInventoryCategory,
  getInventoryProducts, addInventoryProduct, updateInventoryProduct, updateInventoryStock, deleteInventoryProduct,
  getInventoryTransactions, getInventoryTransactionsByProduct, stockMovement, addInventoryTransaction,
  getProducts, addProduct, updateProduct, deleteProduct, getProductTypes, addProductType,
  getRequisitions, addRequisition, getRequisitionItems, updateRequisitionStatus, deleteRequisition,
  getPurchaseOrders, addPurchaseOrder, getPOItems, updatePOStatus,
  getInvoices, addInvoice, getInvoiceItems, markInvoicePaid, deleteInvoice, updateInvoice,
  getQuotes, addQuote, getQuoteItems, updateQuoteStatus,
};
