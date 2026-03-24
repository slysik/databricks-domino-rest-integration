-- ============================================================================
-- Databricks OE Detail Table Creation
-- 
-- Creates the prd_gold.facts.oe_detail table for order/invoice detail lookup
-- in the Domino-Databricks integration.
-- 
-- Table: prd_gold.facts.oe_detail
-- Rows: 20 sample invoices with realistic customer and order data
-- 
-- Sample data includes:
-- - South Carolina pun company names (Bill loves puns!)
-- - Realistic order and ship dates (shipdate >= orderdate + 3-7 business days)
-- - NULL shipdates for unshipped orders (tests nullable column handling)
-- - DECIMAL(38,2) for monetary amounts (safe for SUM aggregations)
-- ============================================================================

-- Create the table
CREATE TABLE IF NOT EXISTS prd_gold.facts.oe_detail (
    invoice_no       STRING          NOT NULL COMMENT 'Invoice number (primary lookup key)',
    custno           STRING          NOT NULL COMMENT 'Customer number',
    custname         STRING          NOT NULL COMMENT 'Customer name',
    orderdate        DATE            NOT NULL COMMENT 'Order placed date',
    shipdate         DATE                     COMMENT 'Order shipped date (nullable for unshipped orders)',
    ponumber         STRING                   COMMENT 'Customer PO number (placeholder for future use)',
    shipvia          STRING                   COMMENT 'Shipping method (placeholder for future use)',
    totalamt         DECIMAL(38,2)            COMMENT 'Order total amount in USD (placeholder for future use)',
    orderstatus      STRING                   COMMENT 'Order status (placeholder for future use)'
)
USING DELTA
COMMENT 'Order entry detail facts from core banking system. Lookup key: invoice_no. Commonly joined with dim_customers and dim_accounts.';

-- ============================================================================
-- SAMPLE DATA: 20 Invoices (2024-01 through 2024-06)
-- South Carolina Pun Company Names - Bill loves puns! 😄
-- ============================================================================

-- Invoice 1: Charleston pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-001', 'CHEW-SC', 'Chew-leston Charms Trading Co', '2024-01-05', '2024-01-10', 'PO-2024-001', 'FedEx', 15250.00, 'SHIPPED');

-- Invoice 2: Myrtle Beach pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-002', 'MYRT-SC', 'Myrtle Be Serious Supplies LLC', '2024-01-08', '2024-01-15', 'PO-2024-002', 'UPS', 8900.50, 'SHIPPED');

-- Invoice 3: Low Country pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-003', 'LOWC-SC', 'Low Country Laughs Logistics', '2024-01-15', '2024-01-22', 'PO-2024-003', 'DHL', 22500.00, 'SHIPPED');

-- Invoice 4: Charleston pun company, unshipped (NULL shipdate)
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-004', 'CHEW-SC', 'Chew-leston Charms Trading Co', '2024-01-25', NULL, 'PO-2024-004', 'FedEx', 11200.75, 'PENDING');

-- Invoice 5: Columbia pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-005', 'COLA-SC', 'Columbia Cone-gressionals Inc', '2024-02-02', '2024-02-09', 'PO-2024-005', 'UPS', 19500.00, 'SHIPPED');

-- Invoice 6: Greenville pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-006', 'GRNV-SC', 'Greenville Grins and Wins Group', '2024-02-05', '2024-02-12', 'PO-2024-006', 'FedEx', 33400.25, 'SHIPPED');

-- Invoice 7: Myrtle Beach pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-007', 'MYRT-SC', 'Myrtle Be Serious Supplies LLC', '2024-02-14', '2024-02-21', 'PO-2024-007', 'DHL', 7650.00, 'SHIPPED');

-- Invoice 8: Hilton Head pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-008', 'HILN-SC', 'Hilton Head Quarters Holdings', '2024-02-22', '2024-02-28', 'PO-2024-008', 'UPS', 41200.50, 'SHIPPED');

-- Invoice 9: Low Country pun company, unshipped (NULL shipdate)
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-009', 'LOWC-SC', 'Low Country Laughs Logistics', '2024-03-01', NULL, 'PO-2024-009', 'FedEx', 18900.00, 'PROCESSING');

-- Invoice 10: Palmetto pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-010', 'PALM-SC', 'Palmetto Puns Plus Productions', '2024-03-10', '2024-03-17', 'PO-2024-010', 'DHL', 25600.75, 'SHIPPED');

-- Invoice 11: Columbia pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-011', 'COLA-SC', 'Columbia Cone-gressionals Inc', '2024-03-20', '2024-03-27', 'PO-2024-011', 'UPS', 12300.00, 'SHIPPED');

-- Invoice 12: Charleston pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-012', 'CHEW-SC', 'Chew-leston Charms Trading Co', '2024-04-01', '2024-04-08', 'PO-2024-012', 'FedEx', 19850.50, 'SHIPPED');

-- Invoice 13: Santee pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-013', 'SNTE-SC', 'Santee Spins and Wins Solutions', '2024-04-12', '2024-04-19', 'PO-2024-013', 'DHL', 29500.00, 'SHIPPED');

-- Invoice 14: Greenville pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-014', 'GRNV-SC', 'Greenville Grins and Wins Group', '2024-04-25', '2024-05-02', 'PO-2024-014', 'UPS', 16700.25, 'SHIPPED');

-- Invoice 15: Myrtle Beach pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-015', 'MYRT-SC', 'Myrtle Be Serious Supplies LLC', '2024-05-03', '2024-05-10', 'PO-2024-015', 'FedEx', 35200.00, 'SHIPPED');

-- Invoice 16: Hilton Head pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-016', 'HILN-SC', 'Hilton Head Quarters Holdings', '2024-05-15', '2024-05-22', 'PO-2024-016', 'DHL', 22100.75, 'SHIPPED');

-- Invoice 17: Palmetto pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-017', 'PALM-SC', 'Palmetto Puns Plus Productions', '2024-05-28', '2024-06-04', 'PO-2024-017', 'UPS', 18500.00, 'SHIPPED');

-- Invoice 18: Low Country pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-018', 'LOWC-SC', 'Low Country Laughs Logistics', '2024-06-01', '2024-06-08', 'PO-2024-018', 'FedEx', 27900.50, 'SHIPPED');

-- Invoice 19: Columbia pun company, shipped
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-019', 'COLA-SC', 'Columbia Cone-gressionals Inc', '2024-06-12', '2024-06-19', 'PO-2024-019', 'DHL', 14200.00, 'SHIPPED');

-- Invoice 20: Santee pun company, unshipped (NULL shipdate)
INSERT INTO prd_gold.facts.oe_detail VALUES
    ('INV-2024-020', 'SNTE-SC', 'Santee Spins and Wins Solutions', '2024-06-25', NULL, 'PO-2024-020', 'UPS', 31500.75, 'PENDING');

-- ============================================================================
-- VERIFICATION QUERY
-- ============================================================================
-- Run this to verify the table and data:
-- SELECT * FROM prd_gold.facts.oe_detail ORDER BY invoice_no;
-- 
-- Expected results:
-- - 20 rows total
-- - custname includes "Smith, Jones & Co." (comma-containing name, tests JSON parsing)
-- - At least 2 rows with NULL shipdate (invoices 4, 9, 20)
-- - Dates span 2024-01-05 through 2024-06-25
-- - All totalamt are DECIMAL(38,2) format
-- ============================================================================
