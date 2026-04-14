import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

const fmt = n => 'Rs. ' + Number(n || 0).toLocaleString('en-IN');

function addHeader(doc, title, number, company, color) {
  doc.setFillColor(color[0], color[1], color[2]);
  doc.rect(0, 0, 210, 10, 'F');

  doc.setFontSize(24);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(color[0], color[1], color[2]);
  doc.text(title, 20, 30);

  doc.setFontSize(11);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(120);
  doc.text(number, 20, 38);

  const co = company || {};
  doc.setFontSize(16);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(30, 30, 30);
  doc.text(co.name || 'Astrochakra', 190, 24, { align: 'right' });

  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(100);
  let ry = 30;
  if (co.address) { doc.text(co.address, 190, ry, { align: 'right' }); ry += 5; }
  if (co.gstin) { doc.text('GSTIN: ' + co.gstin, 190, ry, { align: 'right' }); ry += 5; }
  if (co.email) { doc.text(co.email, 190, ry, { align: 'right' }); ry += 5; }
  if (co.phone) { doc.text('Ph: ' + co.phone, 190, ry, { align: 'right' }); ry += 5; }

  doc.setDrawColor(220, 220, 220);
  doc.line(20, 44, 190, 44);
  return 50;
}

function addPartyInfo(doc, y, label, party) {
  doc.setFontSize(8);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(150);
  doc.text(label.toUpperCase(), 20, y);
  y += 7;

  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(30, 30, 30);
  doc.text(party.name || '', 20, y);
  y += 6;

  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(80);
  if (party.address) { doc.text(party.address, 20, y); y += 5; }
  if (party.gstin) { doc.text('GSTIN: ' + party.gstin, 20, y); y += 5; }
  if (party.email) { doc.text(party.email, 20, y); y += 5; }
  if (party.phone) { doc.text('Ph: ' + party.phone, 20, y); y += 5; }
  return y + 6;
}

function addDetailsRight(doc, date, dueDate, dueLabel, status, statusColor) {
  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(100);
  doc.text('Date: ' + date, 190, 52, { align: 'right' });
  let dy = 58;
  if (dueDate) {
    doc.text((dueLabel || 'Due') + ': ' + dueDate, 190, dy, { align: 'right' });
    dy += 6;
  }
  if (status) {
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(statusColor[0], statusColor[1], statusColor[2]);
    doc.text(status.toUpperCase(), 190, dy, { align: 'right' });
  }
}

function addItemsTable(doc, y, items) {
  const rows = items.map(it => {
    const amt = it.qty * it.rate;
    const tax = Math.round(amt * it.gst / 100 * 100) / 100;
    return [
      it.desc || 'Item',
      String(it.qty),
      fmt(it.rate),
      it.gst + '%',
      fmt(tax),
      fmt(amt + tax)
    ];
  });

  autoTable(doc, {
    startY: y,
    head: [['Description', 'Qty', 'Rate', 'GST', 'Tax', 'Total']],
    body: rows,
    margin: { left: 20, right: 20 },
    styles: { fontSize: 9, cellPadding: 4, textColor: [50, 50, 50] },
    headStyles: {
      fillColor: [245, 246, 250],
      textColor: [80, 80, 80],
      fontStyle: 'bold',
      lineWidth: 0,
    },
    alternateRowStyles: { fillColor: [252, 252, 255] },
    columnStyles: {
      0: { cellWidth: 'auto' },
      1: { halign: 'center', cellWidth: 18 },
      2: { halign: 'right', cellWidth: 30 },
      3: { halign: 'center', cellWidth: 20 },
      4: { halign: 'right', cellWidth: 28 },
      5: { halign: 'right', cellWidth: 30 },
    },
  });

  return doc.lastAutoTable.finalY + 8;
}

function addTotals(doc, y, subtotal, tax, total, gstType, color) {
  const x = 190;
  const isIntra = gstType === 'intra';
  const boxH = isIntra ? 48 : 38;

  doc.setFillColor(248, 249, 252);
  doc.roundedRect(108, y - 4, 84, boxH, 3, 3, 'F');

  let ty = y + 4;

  // Subtotal
  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(120);
  doc.text('Subtotal:', 118, ty);
  doc.setTextColor(50);
  doc.text(fmt(subtotal), x - 4, ty, { align: 'right' });
  ty += 7;

  // Tax breakdown
  if (isIntra) {
    const half = Math.round(tax / 2 * 100) / 100;
    doc.setTextColor(120);
    doc.text('CGST:', 118, ty);
    doc.setTextColor(50);
    doc.text(fmt(half), x - 4, ty, { align: 'right' });
    ty += 7;

    doc.setTextColor(120);
    doc.text('SGST:', 118, ty);
    doc.setTextColor(50);
    doc.text(fmt(half), x - 4, ty, { align: 'right' });
    ty += 7;
  } else {
    doc.setTextColor(120);
    doc.text('IGST:', 118, ty);
    doc.setTextColor(50);
    doc.text(fmt(tax), x - 4, ty, { align: 'right' });
    ty += 7;
  }

  // Divider
  doc.setDrawColor(200);
  doc.line(114, ty - 2, x - 2, ty - 2);
  ty += 4;

  // Grand Total
  doc.setFontSize(14);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(color[0], color[1], color[2]);
  doc.text('Total:', 118, ty);
  doc.text(fmt(total), x - 4, ty, { align: 'right' });

  return y + boxH + 8;
}

function addFooter(doc, company) {
  const co = company || {};
  const h = doc.internal.pageSize.height;

  doc.setDrawColor(220);
  doc.line(20, h - 22, 190, h - 22);

  doc.setFontSize(7);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(160);

  const line1 = [co.name || 'Astrochakra.co'];
  if (co.gstin) line1.push('GSTIN: ' + co.gstin);
  if (co.email) line1.push(co.email);
  if (co.phone) line1.push('Ph: ' + co.phone);
  doc.text(line1.join('  |  '), 105, h - 16, { align: 'center' });

  if (co.address) {
    doc.text(co.address, 105, h - 12, { align: 'center' });
  }

  doc.setTextColor(190);
  doc.text('Powered by Astrochakra.co Accounting  |  Generated ' + new Date().toLocaleDateString('en-IN'), 105, h - 7, { align: 'center' });
}

// ===== INVOICE PDF =====
export function generateInvoicePDF(inv, items, company) {
  const doc = new jsPDF();
  const color = [79, 70, 229];

  let y = addHeader(doc, 'INVOICE', inv.number, company, color);
  y = addPartyInfo(doc, y, 'Bill To', inv.client);

  const sc = inv.status === 'paid' ? [5, 150, 105] : [217, 119, 6];
  addDetailsRight(doc, inv.date, inv.dueDate, 'Due Date', inv.status, sc);

  y = addItemsTable(doc, y, items);
  y = addTotals(doc, y, inv.subtotal, inv.tax, inv.total, inv.type || 'intra', color);

  if (inv.notes) {
    doc.setFontSize(9);
    doc.setFont('helvetica', 'italic');
    doc.setTextColor(120);
    doc.text('Notes: ' + inv.notes, 20, y + 2);
  }

  addFooter(doc, company);
  doc.save(inv.number + '.pdf');
}

// ===== PURCHASE ORDER PDF =====
export function generatePOPDF(po, items, company) {
  const doc = new jsPDF();
  const color = [234, 88, 12];

  let y = addHeader(doc, 'PURCHASE ORDER', po.number, company, color);
  y = addPartyInfo(doc, y, 'Vendor', po.vendor);

  addDetailsRight(doc, po.date, po.deliveryDate, 'Delivery', po.status, [100, 100, 100]);

  y = addItemsTable(doc, y, items);
  y = addTotals(doc, y, po.subtotal, po.tax, po.total, po.type || 'intra', color);

  if (po.notes) {
    doc.setFontSize(9);
    doc.setFont('helvetica', 'italic');
    doc.setTextColor(120);
    doc.text('Notes: ' + po.notes, 20, y + 2);
  }

  addFooter(doc, company);
  doc.save(po.number + '.pdf');
}

// ===== QUOTE PDF =====
export function generateQuotePDF(qt, items, company) {
  const doc = new jsPDF();
  const color = [8, 145, 178];

  let y = addHeader(doc, 'QUOTATION', qt.number, company, color);
  y = addPartyInfo(doc, y, 'To', qt.client);

  addDetailsRight(doc, qt.date, qt.validUntil, 'Valid Until', qt.status, [100, 100, 100]);

  y = addItemsTable(doc, y, items);
  y = addTotals(doc, y, qt.subtotal, qt.tax, qt.total, qt.type || 'intra', color);

  if (qt.notes) {
    doc.setFontSize(9);
    doc.setFont('helvetica', 'italic');
    doc.setTextColor(120);
    doc.text('Notes: ' + qt.notes, 20, y + 2);
    y += 10;
  }
  if (qt.terms) {
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(100);
    doc.text('Terms & Conditions: ' + qt.terms, 20, y + 2);
  }

  addFooter(doc, company);
  doc.save(qt.number + '.pdf');
}

// ===== REPORT PDF =====
export function generateReportPDF(data) {
  const { bal, expenses, income, projects, period, companyInfo } = data;
  const doc = new jsPDF();
  const co = companyInfo || {};

  doc.setFillColor(79, 70, 229);
  doc.rect(0, 0, 210, 10, 'F');

  doc.setFontSize(22);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(79, 70, 229);
  doc.text((co.name || 'Astrochakra') + ' Report', 20, 26);

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(150);
  doc.text(period + '  |  Generated ' + new Date().toLocaleDateString('en-IN'), 20, 34);

  // Summary boxes
  const boxes = [
    { label: 'BALANCE', value: fmt(bal), color: [79, 70, 229] },
    { label: 'EXPENSES', value: fmt(expenses), color: [239, 68, 68] },
    { label: 'INCOME', value: fmt(income), color: [16, 185, 129] },
    { label: 'NET', value: fmt(income - expenses), color: income - expenses >= 0 ? [16, 185, 129] : [239, 68, 68] },
  ];

  let bx = 20;
  boxes.forEach(b => {
    doc.setFillColor(248, 249, 252);
    doc.roundedRect(bx, 42, 40, 22, 3, 3, 'F');
    doc.setFontSize(7);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(150);
    doc.text(b.label, bx + 4, 50);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(b.color[0], b.color[1], b.color[2]);
    doc.text(b.value, bx + 4, 58);
    bx += 44;
  });

  // Project table
  const rows = projects.map(p => {
    const s = p.spent || 0;
    const rem = (p.alloc || 0) - s;
    const pct = p.alloc > 0 ? Math.round((s / p.alloc) * 100) : 0;
    return [p.code, p.name, fmt(p.alloc), fmt(s), fmt(rem), pct + '%'];
  });

  autoTable(doc, {
    startY: 72,
    head: [['Code', 'Project', 'Allocated', 'Spent', 'Remaining', 'Used']],
    body: rows,
    margin: { left: 20, right: 20 },
    styles: { fontSize: 9, cellPadding: 4, textColor: [50, 50, 50] },
    headStyles: { fillColor: [79, 70, 229], textColor: [255, 255, 255], fontStyle: 'bold' },
    alternateRowStyles: { fillColor: [252, 252, 255] },
    columnStyles: {
      0: { cellWidth: 24 },
      2: { halign: 'right' },
      3: { halign: 'right' },
      4: { halign: 'right' },
      5: { halign: 'center' },
    },
  });

  addFooter(doc, companyInfo);
  doc.save('Report_' + period.replace(/\s/g, '_') + '.pdf');
}