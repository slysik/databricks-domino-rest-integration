# How to Share Sample Data with Bill

**Bill** — This guide shows you how to load the 20 sample invoices into your Databricks workspace (takes 2 minutes).

---

## **⚠️ IMPORTANT: Before You Start**

The Java agent queries by **`invoice_no`** (unique primary key):
```sql
SELECT custno, custname, orderdate, shipdate 
FROM prd_gold.facts.oe_detail 
WHERE invoice_no = :invoice_no  -- Your invoice number → 1 row returned
```

This is exactly what Bill's Domino form will do. Each invoice number returns exactly one row.

---

## **Quick Setup (2 Minutes)**

---

## **Option 1: Run the SQL Script (Easiest)** ✅ Recommended

**Time**: 2 minutes  
**Steps**:

1. **Open Databricks SQL Editor**
   - Go to: https://[your-workspace].cloud.databricks.com/sql/editor

2. **Copy the SQL script**
   - GitHub (direct link): https://raw.githubusercontent.com/slysik/databricks-domino-rest-integration/main/sql/create_oe_detail.sql
   - Or clone the repo locally and run: `cat sql/create_oe_detail.sql`

3. **Paste into SQL Editor**
   - Select all the SQL text
   - Paste into the SQL editor window

4. **Run the script**
   - Click the **Run** button (or Cmd+Enter)
   - Wait 5-10 seconds for completion

5. **Done!**
   - Table `prd_gold.facts.oe_detail` is created with 20 invoices
   - All data is ready to test

**Quick Verification** (copy-paste one at a time):
```sql
-- 1. Check row count
SELECT COUNT(*) as row_count FROM prd_gold.facts.oe_detail;
-- Expected: 20

-- 2. Test the actual lookup query (what Bill's Java agent will use)
SELECT custno, custname, orderdate, shipdate 
FROM prd_gold.facts.oe_detail 
WHERE invoice_no = 'INV-2024-001';
-- Expected: CHEW-SC | Chew-leston Charms Trading Co | 2024-01-05 | 2024-01-10
```

**Advantages**:
- ✅ **Simplest** — just copy & paste SQL
- ✅ **Fastest** — 2 minutes total
- ✅ **No dependencies** — no Python, no notebooks
- ✅ **You control everything** — data location, permissions
- ✅ **Easy to regenerate** — just run the SQL again if needed

---

## **Alternative Options**

### **Option 2: Use Python (if you prefer)**
1. Create a new notebook in Databricks
2. Run the script from: `scripts/export-sample-data.py`
3. Done!

### **Option 3: Export for Sharing**
If you need to share the data across workspaces, export as CSV/Parquet from the notebook.



---

## **What You Get (20 Sample Invoices)**

| Property | Details |
|----------|---------|
| **Table** | `prd_gold.facts.oe_detail` |
| **Rows** | 20 invoices (INV-2024-001 through INV-2024-020) |
| **Columns** | invoice_no, custno, custname, orderdate, shipdate (+ 4 placeholders) |
| **Companies** | 8 unique South Carolina pun companies (Bill will love!) |
| **Special** | 3 unshipped orders (NULL shipdate), authentic company names with commas |

**Sample invoice to test with:**
```
Invoice: INV-2024-001
Customer: Chew-leston Charms Trading Co
Order Date: 2024-01-05
Ship Date: 2024-01-10
```

---

---

## **Next: Configure Your Domino Agent**

Once the table is created, you're ready to:

1. **Set up the Domino environment document** with 3 values:
   - `DATABRICKS_HOST`: Your workspace hostname (e.g., `dbc-xxxxx.cloud.databricks.com`)
   - `DATABRICKS_TOKEN`: Your Databricks PAT
   - `WAREHOUSE_ID`: Your SQL warehouse ID

2. **Test the integration**:
   - Deploy the Java agent to your Domino database
   - Add the JavaScript to your form
   - Enter invoice `INV-2024-001` and click Lookup
   - Expected result: Shows customer "Chew-leston Charms Trading Co"

3. **Reference the full README**:
   - Installation guide: https://github.com/slysik/databricks-domino-rest-integration#installation
   - Troubleshooting: https://github.com/slysik/databricks-domino-rest-integration#troubleshooting

---

## **If You Run Into Issues**

| Problem | Check | Solution |
|---------|-------|----------|
| SQL errors | Catalog/schema names | Table should be: `prd_gold.facts.oe_detail` |
| Warehouse error | Warehouse status | Start your SQL warehouse; get ID from workspace UI |
| Permission denied | UC permissions | Verify you can create tables in your catalog |
| Want to regenerate | Sample data | Delete the table and re-run the SQL script |

---

**You're all set!** Table is ready for Bill's Domino agent. 🚀
