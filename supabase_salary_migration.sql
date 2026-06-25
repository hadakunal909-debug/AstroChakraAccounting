-- ============================================================
-- Salary / Resource Management System Migration
-- Run this in your Supabase SQL Editor
-- ============================================================

-- RESOURCES table: register team members with their pay structure
CREATE TABLE IF NOT EXISTS resources (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  role text DEFAULT '',
  pay_type text NOT NULL CHECK (pay_type IN ('fixed', 'per_job', 'commission')),
  pay_amount numeric DEFAULT 0,         -- monthly amount (fixed) OR per-unit rate (per_job)
  commission_percent numeric DEFAULT 0, -- % for commission type
  job_label text DEFAULT 'per job',     -- e.g. "per video", "per consultation"
  is_active boolean DEFAULT true,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

-- SALARY PAYMENTS table: log each salary payment
CREATE TABLE IF NOT EXISTS salary_payments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  resource_id uuid REFERENCES resources(id) ON DELETE SET NULL,
  resource_name text NOT NULL,
  pay_type text,
  amount numeric NOT NULL,
  units numeric,          -- jobs done (per_job) or revenue amount (commission)
  fund_source text DEFAULT 'available',
  fund_label text DEFAULT 'Available',
  period text DEFAULT '',  -- e.g. "2026-04" (YYYY-MM)
  note text DEFAULT '',
  paid_by text DEFAULT '',
  created_at timestamptz DEFAULT now()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_salary_payments_resource_id ON salary_payments(resource_id);
CREATE INDEX IF NOT EXISTS idx_salary_payments_created_at ON salary_payments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_resources_is_active ON resources(is_active);

-- ============================================================
-- Optional: Sample data to get started
-- ============================================================
-- INSERT INTO resources (name, role, pay_type, pay_amount, job_label)
-- VALUES
--   ('Ravi Kumar', 'Animator', 'per_job', 2000, 'per video'),
--   ('Priya Sharma', 'Astrologer', 'commission', 0, 'per consultation'),
--   ('Amit Singh', 'Admin', 'fixed', 25000, 'per month');
-- UPDATE resources SET commission_percent = 15 WHERE role = 'Astrologer';
