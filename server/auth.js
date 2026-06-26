const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { q } = require('./db');

const SECRET = process.env.JWT_SECRET || 'dev-only-insecure-secret-change-me-please-32+-bytes';
const EXPIRES_IN = process.env.JWT_EXPIRES_IN || '1d';

function sign(user) {
  return jwt.sign(
    { sub: user.username, role: user.role || 'regular', name: user.display_name || '' },
    SECRET,
    { expiresIn: EXPIRES_IN }
  );
}

// Populate req.user if a valid Bearer token is present (no-op otherwise).
function authMiddleware(req, _res, next) {
  const h = req.headers.authorization || '';
  if (h.startsWith('Bearer ')) {
    try { req.user = jwt.verify(h.slice(7), SECRET); } catch (_) { /* invalid/expired */ }
  }
  next();
}

function requireAuth(req, res, next) {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized' });
  next();
}

function requireAdmin(req, res, next) {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized' });
  if ((req.user.role || '') !== 'admin') return res.status(403).json({ error: 'Forbidden' });
  next();
}

/**
 * Verify credentials. Existing rows store plaintext passwords (legacy app); on a
 * successful plaintext match we transparently re-hash with bcrypt.
 */
async function login(username, password) {
  const { rows } = await q('select * from app_users where username = $1 and is_active = true', [username]);
  const u = rows[0];
  if (!u || password == null) return null;
  const stored = u.password_hash || '';
  let ok = false;
  if (stored.startsWith('$2')) {
    ok = await bcrypt.compare(password, stored);
  } else if (stored === password) {
    ok = true;
    const newHash = await bcrypt.hash(password, 10);
    await q('update app_users set password_hash = $1, updated_at = now() where id = $2', [newHash, u.id]);
  }
  if (!ok) return null;
  await q('update app_users set last_login = now() where id = $1', [u.id]);
  return u;
}

module.exports = { sign, authMiddleware, requireAuth, requireAdmin, login };
