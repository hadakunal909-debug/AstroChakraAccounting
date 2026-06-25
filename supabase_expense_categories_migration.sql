-- ============================================================
-- Expense (Debit) Categories — admin-managed list
-- Run this in your Supabase SQL Editor
-- ============================================================
-- Mirrors the existing income_sources pattern so admins can
-- add/remove their own debit categories from the app.
--
-- No schema changes are needed for the transaction-editing or
-- bank-reconciliation features — they reuse the existing
-- `transactions` and `balance` tables.
-- ============================================================

CREATE TABLE IF NOT EXISTS expense_categories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  created_at timestamptz DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_expense_categories_name ON expense_categories(lower(name));

-- Seed with the categories that were previously hardcoded (GCATS),
-- so nothing disappears for existing data. ON CONFLICT keeps this idempotent.
INSERT INTO expense_categories (name)
VALUES
  ('Office Supplies'),
  ('Travel'),
  ('Food & Meals'),
  ('Utilities'),
  ('Rent'),
  ('Insurance'),
  ('Marketing'),
  ('Legal'),
  ('Maintenance'),
  ('Miscellaneous')
ON CONFLICT (lower(name)) DO NOTHING;
