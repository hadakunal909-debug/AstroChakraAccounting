// Postgres connection to Supabase (Session pooler). Set DATABASE_URL in the
// cPanel Node app environment, e.g.:
//   postgresql://postgres.<ref>:<password>@aws-1-us-east-1.pooler.supabase.com:5432/postgres
const { Pool, types } = require('pg');

// Make pg return values shaped like the old supabase-js client did, so the
// React app (which reads raw snake_case rows) doesn't need to change:
//   numeric (1700) -> JS number   (money columns)
//   int8/bigint (20) -> JS number (auto-increment ids — all small, safe)
//   date (1082) -> 'YYYY-MM-DD' string (not a Date/ISO timestamp)
types.setTypeParser(1700, (v) => (v == null ? null : parseFloat(v)));
types.setTypeParser(20, (v) => (v == null ? null : parseInt(v, 10)));
types.setTypeParser(1082, (v) => v); // leave date as the raw 'YYYY-MM-DD' text

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }, // Supabase requires SSL
  max: Number(process.env.DB_POOL_MAX || 5),
});

// Simple query helper.
function q(text, params) {
  return pool.query(text, params);
}

// Run fn inside a transaction (for atomic money operations).
async function withTx(fn) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (e) {
    try { await client.query('ROLLBACK'); } catch (_) { /* ignore */ }
    throw e;
  } finally {
    client.release();
  }
}

module.exports = { pool, q, withTx };
