// Express app entry point. On cPanel "Setup Node.js App", set the startup file to
// app.js and configure env vars: DATABASE_URL, JWT_SECRET, CORS_ORIGINS, PORT.
const express = require('express');
const cors = require('cors');
const { authMiddleware } = require('./auth');
const { pool } = require('./db');
const routes = require('./routes');

const app = express();
app.disable('x-powered-by');

// CORS. CORS_ORIGINS is a comma-separated allow-list (e.g.
// "https://accounting.astrochakra.co"). If unset, reflect any origin (dev only).
const origins = (process.env.CORS_ORIGINS || '')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);
app.use(
  cors({
    origin: origins.length ? origins : true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true,
  })
);

app.use(express.json({ limit: process.env.JSON_LIMIT || '10mb' }));
app.use(authMiddleware); // populates req.user when a valid Bearer token is present

app.use('/api', routes);

// 404 for anything unmatched under /api or elsewhere
app.use((_req, res) => res.status(404).json({ error: 'Not found' }));

// Centralized error handler. Routes throw httpError(status, msg) for 4xx;
// anything else is treated as a 500.
// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  const status = err.status || 500;
  if (status >= 500) console.error(err);
  res.status(status).json({ error: err.message || 'Server error' });
});

const port = process.env.PORT || 3001;
const server = app.listen(port, () => {
  console.log('AstroChakra backend listening on :' + port);
});

// Graceful shutdown (cPanel/Passenger sends SIGTERM on restart).
function shutdown() {
  server.close(() => {
    pool.end().finally(() => process.exit(0));
  });
  // Force-exit if connections linger.
  setTimeout(() => process.exit(0), 5000).unref();
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

module.exports = app;
