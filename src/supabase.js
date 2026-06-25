import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://oazvkbvqbarghmcxiagc.supabase.co';
const supabaseKey = 'sb_publishable_fVOLllorOliQTe7b7jF0sw_YT6paYXs';
const supabase = createClient(supabaseUrl, supabaseKey);

const pc = v => (v && v.trim() !== '') ? v : null;

// ===== BALANCE =====
export async function getBalance() {
  const { data } = await supabase.from('balance').select('*').single();
  return data || { balance: 0, liquid_reserve: 0 };
}
export async function updateBalance(balance, liquid) {
  const u = { updated_at: new Date().toISOString() };
  if (balance !== null && balance !== undefined) u.balance = balance;
  if (liquid !== null && liquid !== undefined) u.liquid_reserve = liquid;
  const { data: ex } = await supabase.from('balance').select('id').limit(1);
  if (ex && ex.length > 0) await supabase.from('balance').update(u).eq('id', ex[0].id);
  else await supabase.from('balance').insert({ balance: balance || 0, liquid_reserve: liquid || 0 });
}

// ===== COMPANY INFO =====
export async function getCompanyInfo() {
  const { data } = await supabase.from('company_info').select('*').single();
  return data || { name: 'Astrochakra' };
}
export async function updateCompanyInfo(info) {
  const { data: ex } = await supabase.from('company_info').select('id').limit(1);
  const row = { name: info.name || '', address: info.address || '', gstin: info.gstin || '', email: info.email || '', phone: info.phone || '', updated_at: new Date().toISOString() };
  if (ex && ex.length > 0) await supabase.from('company_info').update(row).eq('id', ex[0].id);
  else await supabase.from('company_info').insert(row);
}

// ===== PROJECTS =====
export async function getProjects() {
  const { data } = await supabase.from('projects').select('*').order('created_at');
  return data || [];
}
export async function addProject(p) {
  const { data } = await supabase.from('projects').insert({ name: p.name, code: p.code, fixed_budget: p.fixed, allocated: p.alloc, color: p.color || '#6366f1' }).select().single();
  return data;
}
export async function updateProject(id, p) {
  await supabase.from('projects').update({ name: p.name, fixed_budget: p.fixed, allocated: p.alloc, color: p.color, updated_at: new Date().toISOString() }).eq('id', id);
}
export async function deleteProject(id) {
  await supabase.from('projects').delete().eq('id', id);
}

// ===== PEOPLE =====
export async function getPeople() {
  const { data } = await supabase.from('people').select('*').order('created_at');
  return data || [];
}
export async function addPerson(p) {
  const { data } = await supabase.from('people').insert({ name: p.name, role: p.role || 'Member' }).select().single();
  return data;
}
export async function updatePerson(id, p) {
  await supabase.from('people').update({ name: p.name, role: p.role, updated_at: new Date().toISOString() }).eq('id', id);
}
export async function deletePerson(id) {
  await supabase.from('people').delete().eq('id', id);
}

// ===== INCOME SOURCES =====
export async function getIncomeSources() {
  const { data } = await supabase.from('income_sources').select('*').order('created_at');
  return data || [];
}
export async function addIncomeSource(name) {
  const { data } = await supabase.from('income_sources').insert({ name }).select().single();
  return data;
}

// ===== EXPENSE CATEGORIES (admin-managed debit categories) =====
export async function getExpenseCategories() {
  const { data } = await supabase.from('expense_categories').select('*').order('name');
  return data || [];
}
export async function addExpenseCategory(name) {
  const { data, error } = await supabase.from('expense_categories').insert({ name }).select().single();
  if (error) console.error('Expense category error:', error);
  return data;
}
export async function deleteExpenseCategory(id) {
  await supabase.from('expense_categories').delete().eq('id', id);
}

// ===== TRANSACTIONS =====
export async function getTransactions() {
  const { data } = await supabase.from('transactions').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addTransaction(t) {
  const { data, error } = await supabase.from('transactions').insert({
    date: t.date, project_code: pc(t.project), person: t.person, amount: t.amount,
    kind: t.kind, category: t.category || '', note: t.note || '',
    bill_url: t.billUrl || null, bill_name: t.billName || null,
    settled: false, is_reversal: t.isReversal || false,
    original_id: t.originalId || null, reverses_kind: t.reversesKind || null,
    fund_source: t.fundSource || 'available'
  }).select().single();
  if (error) console.error('Transaction error:', error);
  return data;
}
export async function settleTransaction(id) {
  await supabase.from('transactions').update({ settled: true }).eq('id', id);
}
export async function updateTransactionsPerson(oldName, newName) {
  await supabase.from('transactions').update({ person: newName }).eq('person', oldName);
}
// Edit ONLY metadata/tags of a transaction — never amount/kind/fund_source/balance.
export async function updateTransactionMeta(id, t) {
  await supabase.from('transactions').update({
    date: t.date, person: t.person, category: t.category || '',
    project_code: pc(t.project), note: t.note || ''
  }).eq('id', id);
}
// Normalize a transaction's fund source (used by reconciliation)
export async function updateTransactionFundSource(id, fundSource) {
  await supabase.from('transactions').update({ fund_source: fundSource }).eq('id', id);
}
// Insert many transactions at once (bank statement import)
export async function addTransactionsBulk(rows) {
  const payload = rows.map(t => ({
    date: t.date, project_code: pc(t.project), person: t.person, amount: t.amount,
    kind: t.kind, category: t.category || '', note: t.note || '',
    settled: false, is_reversal: false, fund_source: t.fundSource || 'available'
  }));
  const { data, error } = await supabase.from('transactions').insert(payload).select();
  if (error) console.error('Bulk transaction error:', error);
  return data || [];
}

// ===== FILE UPLOAD =====
export async function uploadFile(file, path) {
  const { error } = await supabase.storage.from('bills').upload(path, file);
  if (error) { console.error('Upload error:', error); return null; }
  const { data: urlData } = supabase.storage.from('bills').getPublicUrl(path);
  return urlData?.publicUrl || null;
}

// ===== PRODUCTS =====
export async function getProducts() {
  const { data } = await supabase.from('products').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addProduct(p) {
  const { data, error } = await supabase.from('products').insert({
    name: p.name, product_type: p.productType, category: p.category || '',
    unit: p.unit || 'pcs', price: p.price || 0, hsn_code: p.hsnCode || '',
    tax_percent: p.taxPercent || 18, description: p.description || ''
  }).select().single();
  if (error) console.error('Product error:', error);
  return data;
}
export async function updateProduct(id, p) {
  await supabase.from('products').update({
    name: p.name, product_type: p.productType, category: p.category || '',
    unit: p.unit || 'pcs', price: p.price || 0, hsn_code: p.hsnCode || '',
    tax_percent: p.taxPercent || 18, description: p.description || '',
    updated_at: new Date().toISOString()
  }).eq('id', id);
}
export async function deleteProduct(id) {
  await supabase.from('products').delete().eq('id', id);
}
export async function getProductTypes() {
  const { data } = await supabase.from('product_types').select('*').order('name');
  return data || [];
}
export async function addProductType(name) {
  const { data } = await supabase.from('product_types').insert({ name }).select().single();
  return data;
}

// ===== REQUISITIONS =====
export async function getRequisitions() {
  const { data } = await supabase.from('requisitions').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addRequisition(r, items) {
  const { data, error } = await supabase.from('requisitions').insert({
    number: r.number, date: r.date, requested_by: r.requestedBy,
    vendor_name: r.vendor.name, vendor_email: r.vendor.email || '',
    vendor_phone: r.vendor.phone || '', vendor_address: r.vendor.address || '',
    vendor_gstin: r.vendor.gstin || '', gst_type: r.type || 'intra',
    project_code: pc(r.project), subtotal: r.subtotal, tax: r.tax, total: r.total,
    notes: r.notes || '', status: 'draft'
  }).select().single();
  if (error || !data) { console.error('Requisition error:', error); return null; }
  if (items && items.length > 0) {
    await supabase.from('requisition_items').insert(items.map(it => ({
      requisition_id: data.id, description: it.desc || 'Item', hsn: it.hsn || '',
      qty: it.qty, rate: it.rate, gst_percent: it.gst
    })));
  }
  return data;
}
export async function getRequisitionItems(reqId) {
  const { data } = await supabase.from('requisition_items').select('*').eq('requisition_id', reqId);
  return data || [];
}
export async function updateRequisitionStatus(id, status, approvedBy) {
  const u = { status };
  if (approvedBy) { u.approved_by = approvedBy; u.approved_at = new Date().toISOString(); }
  await supabase.from('requisitions').update(u).eq('id', id);
}
export async function deleteRequisition(id) {
  await supabase.from('requisition_items').delete().eq('requisition_id', id);
  await supabase.from('requisitions').delete().eq('id', id);
}

// ===== PURCHASE ORDERS =====
export async function getPurchaseOrders() {
  const { data } = await supabase.from('purchase_orders').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addPurchaseOrder(po, items) {
  const { data, error } = await supabase.from('purchase_orders').insert({
    number: po.number, date: po.date, delivery_date: po.deliveryDate || null,
    vendor_name: po.vendor.name, vendor_email: po.vendor.email || '',
    vendor_phone: po.vendor.phone || '', vendor_address: po.vendor.address || '',
    vendor_gstin: po.vendor.gstin || '', gst_type: po.type || 'intra',
    project_code: pc(po.project), subtotal: po.subtotal, tax: po.tax, total: po.total,
    notes: po.notes || '', status: 'draft', requisition_id: po.requisitionId || null
  }).select().single();
  if (error || !data) { console.error('PO error:', error); return null; }
  if (items && items.length > 0) {
    await supabase.from('po_items').insert(items.map(it => ({
      po_id: data.id, description: it.desc || 'Item', hsn: it.hsn || '',
      qty: it.qty, rate: it.rate, gst_percent: it.gst
    })));
  }
  return data;
}
export async function getPOItems(poId) {
  const { data } = await supabase.from('po_items').select('*').eq('po_id', poId);
  return data || [];
}
export async function updatePOStatus(id, status) {
  await supabase.from('purchase_orders').update({ status }).eq('id', id);
}

// ===== INVOICES =====
export async function getInvoices() {
  const { data } = await supabase.from('invoices').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addInvoice(inv, items) {
  const { data, error } = await supabase.from('invoices').insert({
    number: inv.number, date: inv.date, due_date: inv.dueDate || null,
    client_name: inv.client.name, client_email: inv.client.email || '',
    client_phone: inv.client.phone || '', client_address: inv.client.address || '',
    client_gstin: inv.client.gstin || '', gst_type: inv.type || 'intra',
    project_code: pc(inv.project), subtotal: inv.subtotal, tax: inv.tax, total: inv.total,
    notes: inv.notes || '', status: inv.status || 'unpaid',
    po_id: inv.poId || null, locked: inv.locked || false,
    quote_id: inv.quoteId || null, source: inv.source || 'b2b',
    editable: inv.editable || false
  }).select().single();
  if (error || !data) { console.error('Invoice error:', error); return null; }
  if (items && items.length > 0) {
    await supabase.from('invoice_items').insert(items.map(it => ({
      invoice_id: data.id, description: it.desc || 'Item', hsn: it.hsn || '',
      qty: it.qty, rate: it.rate, gst_percent: it.gst
    })));
  }
  return data;
}
export async function getInvoiceItems(invoiceId) {
  const { data } = await supabase.from('invoice_items').select('*').eq('invoice_id', invoiceId);
  return data || [];
}
export async function markInvoicePaid(id) {
  await supabase.from('invoices').update({ status: 'paid', locked: true, editable: false }).eq('id', id);
}
export async function deleteInvoice(id) {
  await supabase.from('invoice_items').delete().eq('invoice_id', id);
  await supabase.from('invoices').delete().eq('id', id);
}
export async function updateInvoice(id, inv, items) {
  await supabase.from('invoices').update({
    date: inv.date, due_date: inv.dueDate || null,
    client_name: inv.client.name, client_email: inv.client.email || '',
    client_phone: inv.client.phone || '', client_address: inv.client.address || '',
    client_gstin: inv.client.gstin || '', gst_type: inv.type || 'intra',
    project_code: pc(inv.project), subtotal: inv.subtotal, tax: inv.tax, total: inv.total,
    notes: inv.notes || '', updated_at: new Date().toISOString()
  }).eq('id', id);
  if (items) {
    await supabase.from('invoice_items').delete().eq('invoice_id', id);
    await supabase.from('invoice_items').insert(items.map(it => ({
      invoice_id: id, description: it.desc || 'Item', hsn: it.hsn || '',
      qty: it.qty, rate: it.rate, gst_percent: it.gst
    })));
  }
}

// ===== QUOTES =====
export async function getQuotes() {
  const { data } = await supabase.from('quotes').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addQuote(qt, items) {
  const { data, error } = await supabase.from('quotes').insert({
    number: qt.number, date: qt.date, valid_until: qt.validUntil || null,
    client_name: qt.client.name, client_email: qt.client.email || '',
    client_phone: qt.client.phone || '', client_address: qt.client.address || '',
    client_gstin: qt.client.gstin || '', gst_type: qt.type || 'intra',
    project_code: pc(qt.project), subtotal: qt.subtotal, tax: qt.tax, total: qt.total,
    notes: qt.notes || '', terms: qt.terms || '', status: 'draft'
  }).select().single();
  if (error || !data) { console.error('Quote error:', error); return null; }
  if (items && items.length > 0) {
    await supabase.from('quote_items').insert(items.map(it => ({
      quote_id: data.id, description: it.desc || 'Item', hsn: it.hsn || '',
      qty: it.qty, rate: it.rate, gst_percent: it.gst
    })));
  }
  return data;
}
export async function getQuoteItems(quoteId) {
  const { data } = await supabase.from('quote_items').select('*').eq('quote_id', quoteId);
  return data || [];
}
export async function updateQuoteStatus(id, status) {
  await supabase.from('quotes').update({ status }).eq('id', id);
}

// ===== REMOVALS =====
export async function getRemovals() {
  const { data } = await supabase.from('removals').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addRemoval(r) {
  await supabase.from('removals').insert({ date: r.date, time: r.time, type: r.type, label: r.label, detail: r.detail || '', reason: r.reason || '' });
}

// ===== APP USERS =====
export async function loginUser(username, password) {
  const { data, error } = await supabase.from('app_users').select('*').eq('username', username).eq('password_hash', password).eq('is_active', true).single();
  if (error || !data) return null;
  await supabase.from('app_users').update({ last_login: new Date().toISOString() }).eq('id', data.id);
  return { id: data.id, username: data.username, displayName: data.display_name, email: data.email, role: data.role };
}
export async function getUsers() {
  const { data } = await supabase.from('app_users').select('id, username, display_name, email, role, is_active, last_login, created_at').order('created_at');
  return data || [];
}
export async function createUser(user) {
  const { data, error } = await supabase.from('app_users').insert({ username: user.username, password_hash: user.password, display_name: user.displayName, email: user.email || '', role: user.role || 'regular' }).select().single();
  if (error) console.error('createUser error:', error);
  return data;
}
export async function updateUser(id, updates) {
  const u = { updated_at: new Date().toISOString() };
  if (updates.displayName !== undefined) u.display_name = updates.displayName;
  if (updates.email !== undefined) u.email = updates.email;
  if (updates.role !== undefined) u.role = updates.role;
  if (updates.isActive !== undefined) u.is_active = updates.isActive;
  if (updates.password) u.password_hash = updates.password;
  await supabase.from('app_users').update(u).eq('id', id);
}
export async function deleteUser(id) {
  await supabase.from('app_users').delete().eq('id', id);
}

// ===== CHANGE REQUESTS =====
export async function getChangeRequests() {
  const { data } = await supabase.from('change_requests').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function getMyChangeRequests(username) {
  const { data } = await supabase.from('change_requests').select('*').eq('requested_by', username).order('created_at', { ascending: false });
  return data || [];
}
export async function createChangeRequest(req) {
  const { data } = await supabase.from('change_requests').insert({
    requested_by: req.requestedBy, entity_type: req.entityType,
    entity_id: req.entityId || null, change_type: req.changeType,
    description: req.description
  }).select().single();
  return data;
}
export async function reviewChangeRequest(id, status, reviewedBy, note) {
  await supabase.from('change_requests').update({ status, reviewed_by: reviewedBy, review_note: note || '', reviewed_at: new Date().toISOString() }).eq('id', id);
}

// ===== ACTIVITY LOG =====
export async function logActivity(userName, userRole, action, entityType, entityId, description) {
  await supabase.from('activity_log').insert({ user_name: userName, user_role: userRole, action, entity_type: entityType || '', entity_id: entityId || null, description: description || '' });
}
export async function getActivityLog(limit) {
  const { data } = await supabase.from('activity_log').select('*').order('created_at', { ascending: false }).limit(limit || 10);
  return data || [];
}

// ===== INVENTORY CATEGORIES =====
export async function getInventoryCategories() {
  const { data, error } = await supabase.from('inventory_categories').select('*').order('name');
  if (error) console.error('getInventoryCategories error:', error.message, error.code);
  return data || [];
}
export async function addInventoryCategory(cat) {
  const { data, error } = await supabase.from('inventory_categories').insert({
    name: cat.name, description: cat.description || '', color: cat.color || '#6366f1'
  }).select().single();
  if (error) console.error('Inv category error:', error);
  return data;
}
export async function updateInventoryCategory(id, cat) {
  await supabase.from('inventory_categories').update({
    name: cat.name, description: cat.description || '', color: cat.color || '#6366f1',
    updated_at: new Date().toISOString()
  }).eq('id', id);
}
export async function deleteInventoryCategory(id) {
  await supabase.from('inventory_categories').delete().eq('id', id);
}

// ===== INVENTORY PRODUCTS =====
export async function getInventoryProducts() {
  const { data } = await supabase.from('inventory_products').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addInventoryProduct(p) {
  const { data, error } = await supabase.from('inventory_products').insert({
    name: p.name, sku: p.sku || '', category_id: p.categoryId || null,
    category_name: p.categoryName || '', unit: p.unit || 'pcs',
    buying_price: p.buyingPrice || 0, selling_price: p.sellingPrice || 0,
    min_stock: p.minStock || 0, current_stock: p.currentStock || 0,
    description: p.description || '', hsn_code: p.hsnCode || '',
    tax_percent: p.taxPercent || 18, product_type: p.productType || 'Product',
    is_active: true
  }).select().single();
  if (error) console.error('Inv product error:', error);
  return data;
}
export async function updateInventoryProduct(id, p) {
  await supabase.from('inventory_products').update({
    name: p.name, sku: p.sku || '', category_id: p.categoryId || null,
    category_name: p.categoryName || '', unit: p.unit || 'pcs',
    buying_price: p.buyingPrice || 0, selling_price: p.sellingPrice || 0,
    min_stock: p.minStock || 0, description: p.description || '',
    hsn_code: p.hsnCode || '', tax_percent: p.taxPercent || 18,
    product_type: p.productType || 'Product',
    updated_at: new Date().toISOString()
  }).eq('id', id);
}
export async function updateInventoryStock(id, newStock) {
  await supabase.from('inventory_products').update({
    current_stock: newStock, updated_at: new Date().toISOString()
  }).eq('id', id);
}
export async function deleteInventoryProduct(id) {
  await supabase.from('inventory_products').update({ is_active: false, updated_at: new Date().toISOString() }).eq('id', id);
}

// ===== INVENTORY TRANSACTIONS =====
export async function getInventoryTransactions(limit) {
  const { data } = await supabase.from('inventory_transactions').select('*')
    .order('created_at', { ascending: false }).limit(limit || 50);
  return data || [];
}
export async function getInventoryTransactionsByProduct(productId) {
  const { data } = await supabase.from('inventory_transactions').select('*')
    .eq('product_id', productId).order('created_at', { ascending: false });
  return data || [];
}
export async function addInventoryTransaction(t) {
  const { data, error } = await supabase.from('inventory_transactions').insert({
    product_id: t.productId, product_name: t.productName || '',
    type: t.type || 'stock_in', quantity: t.quantity,
    unit_cost: t.unitCost || 0, total_cost: t.totalCost || 0,
    fund_source: t.fundSource || '', fund_label: t.fundLabel || '',
    notes: t.notes || '', performed_by: t.performedBy || ''
  }).select().single();
  if (error) console.error('Inv transaction error:', error);
  return data;
}

// ===== RESOURCES (HR / Payroll) =====
export async function getResources() {
  const { data } = await supabase.from('resources').select('*').order('created_at');
  return data || [];
}
export async function addResource(r) {
  const { data, error } = await supabase.from('resources').insert({
    name: r.name, role: r.role || '', pay_type: r.payType,
    pay_amount: r.payAmount || 0, commission_percent: r.commissionPercent || 0,
    job_label: r.jobLabel || 'per job', is_active: true
  }).select().single();
  if (error) { console.error('Resource error:', error.message, error.code); return { error }; }
  return { data };
}
export async function updateResource(id, r) {
  await supabase.from('resources').update({
    name: r.name, role: r.role || '', pay_type: r.payType,
    pay_amount: r.payAmount || 0, commission_percent: r.commissionPercent || 0,
    job_label: r.jobLabel || 'per job', updated_at: new Date().toISOString()
  }).eq('id', id);
}
export async function deleteResource(id) {
  await supabase.from('resources').update({ is_active: false, updated_at: new Date().toISOString() }).eq('id', id);
}

// ===== SALARY PAYMENTS =====
export async function getSalaryPayments() {
  const { data } = await supabase.from('salary_payments').select('*').order('created_at', { ascending: false });
  return data || [];
}
export async function addSalaryPayment(p) {
  const { data, error } = await supabase.from('salary_payments').insert({
    resource_id: p.resourceId, resource_name: p.resourceName,
    pay_type: p.payType, amount: p.amount, units: p.units || null,
    fund_source: p.fundSource || 'available', fund_label: p.fundLabel || 'Available',
    period: p.period || '', note: p.note || '', paid_by: p.paidBy || ''
  }).select().single();
  if (error) console.error('Salary payment error:', error);
  return data;
}

export default supabase;