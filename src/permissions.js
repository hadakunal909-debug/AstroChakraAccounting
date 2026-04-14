const PERMISSIONS = {
  admin: {
    viewDashboard: true, editDashboard: true, viewFullAnalytics: true,
    addTransaction: true, settleTransaction: true, viewAllTransactions: true, attachBills: true,
    createProject: true, editProject: true, deleteProject: true,
    addPeople: true, editPeople: true, deletePeople: true,
    createInvoice: true, editInvoice: false, deleteInvoice: true, markInvoicePaid: true, downloadInvoicePDF: true,
    createPO: true, editPO: true, deletePO: true, updatePOStatus: true, approvePO: true, downloadPOPDF: true,
    createQuote: true, editQuote: true, deleteQuote: true, updateQuoteStatus: true, downloadQuotePDF: true,
    createRequisition: true, editRequisition: true, deleteRequisition: true, approveRequisition: true, convertRequisition: true,
    viewAllocation: true, allocateBudget: true, updateReserve: true,
    viewReports: true, viewAllTimeReports: true, exportReports: true,
    manageUsers: true, viewRemovalLog: true, viewActivityLog: true, approveChangeRequests: true, viewChangeRequests: true,
    editCompanyInfo: true, uploadFiles: true,
    viewInventory: true, manageInventory: true, addStock: true, removeStock: true,
  },
  regular: {
    viewDashboard: true, editDashboard: false, viewFullAnalytics: false,
    addTransaction: true, settleTransaction: false, viewAllTransactions: false, attachBills: false,
    createProject: false, editProject: false, deleteProject: false,
    addPeople: false, editPeople: false, deletePeople: false,
    createInvoice: false, editInvoice: false, deleteInvoice: false, markInvoicePaid: false, downloadInvoicePDF: true,
    createPO: false, editPO: true, deletePO: false, updatePOStatus: false, approvePO: false, downloadPOPDF: true,
    createQuote: true, editQuote: false, deleteQuote: false, updateQuoteStatus: false, downloadQuotePDF: true,
    createRequisition: true, editRequisition: true, deleteRequisition: false, approveRequisition: false, convertRequisition: false,
    viewAllocation: false, allocateBudget: false, updateReserve: false,
    viewReports: true, viewAllTimeReports: false, exportReports: false,
    manageUsers: false, viewRemovalLog: false, viewActivityLog: false, approveChangeRequests: false, viewChangeRequests: true,
    editCompanyInfo: false, uploadFiles: false,
    viewInventory: true, manageInventory: false, addStock: false, removeStock: false,
  }
};

export function can(role, action) {
  const perms = PERMISSIONS[role];
  if (!perms) return false;
  return perms[action] === true;
}

export function getVisibleTabs(role) { return []; }

export function getDateLimit(role) {
  if (role === 'admin') return null;
  const d = new Date();
  d.setDate(d.getDate() - 30);
  return d.toISOString().slice(0, 10);
}

export default PERMISSIONS;