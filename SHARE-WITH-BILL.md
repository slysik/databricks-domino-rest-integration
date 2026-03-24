# How to Share Sample Data with Bill

**Bill** — Here are 3 ways to get the sample data into your Databricks workspace for testing the Domino-Databricks integration:

---

## **Option 1: Run the SQL Script (Easiest)** ✅ Recommended

**Time**: 2 minutes  
**Steps**:

1. Open your Databricks workspace: https://dbc-[yours].cloud.databricks.com
2. Click **SQL** → **SQL Editor**
3. Copy the entire contents of this file:
   ```
   https://raw.githubusercontent.com/slysik/databricks-domino-rest-integration/blob/main/sql/create_oe_detail.sql
   ```
4. Or copy from local repo:
   ```bash
   cat sql/create_oe_detail.sql
   ```
5. Paste into SQL editor
6. Click **Run** → Done! Table created with 20 invoices

**Verify**:
```sql
SELECT COUNT(*) as row_count FROM prd_gold.facts.oe_detail;
-- Should return: 20
```

**Advantages**:
- ✅ Simplest (copy-paste)
- ✅ No setup needed
- ✅ You control the data location/permissions
- ✅ Easy to regenerate

---

## **Option 2: Use the Python Export Script**

**Time**: 5 minutes  
**For**: If you want to clone the exact data format

1. In your Databricks workspace, create a new **Notebook**
2. Copy the entire contents of:
   ```
   https://raw.githubusercontent.com/slysik/databricks-domino-rest-integration/main/scripts/export-sample-data.py
   ```
3. Paste into the notebook
4. Run the cell → Table `prd_gold.facts.oe_detail` is created

**Verify**:
```sql
SELECT * FROM prd_gold.facts.oe_detail LIMIT 5;
```

**Advantages**:
- ✅ Programmatic (easy to modify)
- ✅ Exact schema matching
- ✅ Reproducible

---

## **Option 3: Get a CSV/Parquet Export (If Sharing Across Workspaces)**

**Time**: 10 minutes  
**For**: If you want to export data to share with others

From your Databricks workspace:

```python
# In a notebook cell, run this:
df = spark.read.table("prd_gold.facts.oe_detail")

# Export as CSV
df.write.mode("overwrite").csv("/Volumes/[catalog]/[volume]/oe_detail_export.csv", header=True)

# Or export as Parquet
df.write.mode("overwrite").parquet("/Volumes/[catalog]/[volume]/oe_detail_export.parquet")
```

Then download from the UI and share with Bill.

---

## **⚠️ Important: Query by invoice_no (NOT custno)**

The Java agent queries by **invoice_no** (unique primary key):
```sql
SELECT custno, custname, orderdate, shipdate 
FROM prd_gold.facts.oe_detail 
WHERE invoice_no = :invoice_no  -- ← Bill's method (returns 1 row)
```

❌ **DO NOT query by custno** (not unique):
```sql
WHERE custno = 'CHEW-SC'  -- Returns 3 rows (not what Bill needs)
```

**Why?** Bill passes invoice numbers via Ajax, and each invoice should return exactly one order detail.

---

## **Quick Test: Verify the Data**

After creating the table, run these queries to verify:

```sql
-- Verify row count
SELECT COUNT(*) as total_rows FROM prd_gold.facts.oe_detail;
-- Expected: 20

-- Show sample records
SELECT * FROM prd_gold.facts.oe_detail LIMIT 5;

-- Test the lookup (what the Domino agent will query)
SELECT custno, custname, orderdate, shipdate 
FROM prd_gold.facts.oe_detail 
WHERE invoice_no = 'INV-2024-001';
-- Expected: CUST001, Acme Corporation, 2024-01-05, 2024-01-10

-- Test NULL handling (unshipped orders)
SELECT invoice_no, custname, shipdate 
FROM prd_gold.facts.oe_detail 
WHERE shipdate IS NULL;
-- Expected: 3 rows (INV-2024-004, INV-2024-009, INV-2024-020)

-- Test comma in name (JSON parsing test)
SELECT invoice_no, custname 
FROM prd_gold.facts.oe_detail 
WHERE custname LIKE '%,%';
-- Expected: 3 rows with "Smith, Jones & Co."
```

---

## **Sample Data Overview**

| Characteristic | Details |
|---|---|
| **Table Name** | `prd_gold.facts.oe_detail` |
| **Row Count** | 20 invoices |
| **Columns** | 9 (4 core + 5 placeholders) |
| **Date Range** | 2024-01-05 through 2024-06-25 |
| **Customers** | 8 unique South Carolina pun companies |
| **Unshipped** | 3 rows with NULL shipdate |
| **Invoice_no** | Unique primary key (INV-2024-001 through INV-2024-020) |

### **South Carolina Pun Company Names** 🎭

| Custno | Company Name |
|--------|--------------|
| **CHEW-SC** | Chew-leston Charms Trading Co |
| **MYRT-SC** | Myrtle Be Serious Supplies LLC |
| **LOWC-SC** | Low Country Laughs Logistics |
| **COLA-SC** | Columbia Cone-gressionals Inc |
| **GRNV-SC** | Greenville Grins and Wins Group |
| **HILN-SC** | Hilton Head Quarters Holdings |
| **PALM-SC** | Palmetto Puns Plus Productions |
| **SNTE-SC** | Santee Spins and Wins Solutions |

---

## **After Getting the Data: Next Steps**

1. **Configure Domino environment document** with your Databricks credentials:
   - `DATABRICKS_HOST`: Your workspace host
   - `DATABRICKS_TOKEN`: Your PAT
   - `WAREHOUSE_ID`: Your SQL warehouse ID

2. **Test the Domino agent** with a sample invoice:
   ```
   Invoice: INV-2024-001
   Expected customer: Acme Corporation
   ```

3. **Test the JavaScript** form lookup

4. **Reference the README** for troubleshooting:
   https://github.com/slysik/databricks-domino-rest-integration#readme

---

## **Questions or Issues?**

- ❓ **SQL errors**: Check catalog/schema names match your workspace
- ❓ **Missing warehouse**: Verify warehouse ID and that it's running
- ❓ **Permission issues**: Verify you have UC CREATE TABLE permissions
- ❓ **Need to modify**: All 20 rows are in `sql/create_oe_detail.sql` — easy to edit

---

**Recommended Order**:
1. **First**: Run Option 1 (SQL script) — simplest
2. **Then**: Test Domino agent with INV-2024-001
3. **Finally**: Extend with new fields if needed

Good luck! 🚀
