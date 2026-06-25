// Minimal, dependency-free CSV parsing + bank-field normalization helpers.

// Parse CSV text into an array of rows (each an array of string cells).
// Handles quoted fields, escaped quotes (""), CRLF/LF line endings, and a leading BOM.
export function parseCSV(text) {
  if (!text) return [];
  if (text.charCodeAt(0) === 0xFEFF) text = text.slice(1); // strip BOM
  const rows = [];
  let row = [], field = '', inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') {
        if (text[i + 1] === '"') { field += '"'; i++; }
        else inQuotes = false;
      } else field += c;
    } else {
      if (c === '"') inQuotes = true;
      else if (c === ',') { row.push(field); field = ''; }
      else if (c === '\n') { row.push(field); rows.push(row); row = []; field = ''; }
      else if (c === '\r') { /* handled at \n */ }
      else field += c;
    }
  }
  if (field !== '' || row.length > 0) { row.push(field); rows.push(row); }
  // Drop fully-empty rows (common trailing blank lines in bank exports)
  return rows.filter(r => r.length && !r.every(c => (c || '').trim() === ''));
}

// Normalize a bank date string to YYYY-MM-DD. Assumes day-first for ambiguous slash/dash
// formats (typical for Indian bank statements). Returns '' if unparseable.
export function normDate(s) {
  if (!s) return '';
  s = String(s).trim();
  let m = s.match(/^(\d{4})-(\d{2})-(\d{2})/); // already ISO
  if (m) return `${m[1]}-${m[2]}-${m[3]}`;
  m = s.match(/^(\d{1,2})[\/\-.](\d{1,2})[\/\-.](\d{2,4})$/); // DD/MM/YYYY (day-first)
  if (m) {
    let [, d, mo, y] = m;
    if (y.length === 2) y = (Number(y) > 50 ? '19' : '20') + y;
    return `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }
  // DD-Mon-YYYY (e.g. 05-Apr-2026) or similar
  const d2 = new Date(s);
  if (!isNaN(d2.getTime())) return d2.toISOString().slice(0, 10);
  return '';
}

// Parse a bank amount cell into a Number. Strips currency symbols, commas and spaces;
// supports (parentheses) and trailing Dr/Cr markers as negative/positive hints.
export function parseAmt(s) {
  if (s === null || s === undefined) return 0;
  let t = String(s).trim();
  if (!t) return 0;
  let sign = 1;
  if (/\(.*\)/.test(t)) sign = -1;            // (1,234.00) => negative
  if (/dr$/i.test(t)) sign = -1;              // "1,234 Dr" => debit
  if (/cr$/i.test(t)) sign = 1;               // "1,234 Cr" => credit
  t = t.replace(/[^0-9.\-]/g, '');            // keep digits, dot, minus
  const n = parseFloat(t);
  if (isNaN(n)) return 0;
  // Combine an explicit leading minus with any Dr/parenthesis marker.
  return Math.abs(n) * sign * (n < 0 ? -1 : 1);
}
