# Databricks-Domino REST Integration

**A working pattern for querying Databricks from HCL Domino forms**

⚠️ **Status**: Tested with sample data. Before production use, read [HONEST_ASSESSMENT.md](./HONEST_ASSESSMENT.md)

---

## What This Does

```
Your Domino Form
   ↓ (invoice number)
JavaScript Ajax POST
   ↓
Java Agent (OEDetailLookup)
   ↓
Databricks REST API (/api/2.0/sql/statements)
   ↓
SQL Query (parameterized, safe)
   ↓
JSON Response
   ↓
Java Agent parses & returns: custno~*~custname~*~orderdate~*~shipdate
   ↓
JavaScript splits on ~*~
   ↓
jQuery populates form fields
```

**Result**: User enters invoice number → Fields auto-populate in real-time

---

## Architecture Diagram

![Architecture](./docs/diagrams/bill_domino_databricks_architecture.png)

---

## Your Quick Start (6 Steps)

📖 **See [BILL_QUICK_START.md](./BILL_QUICK_START.md)** for your exact implementation steps.

**Time**: ~30 minutes  
**What you need**: Your Databricks host, PAT, warehouse ID  
**No setup needed**: You use your company's own data table

---

## Adding More Fields (When You Get Table Access)

When you get access to other tables and see what columns are available:

📖 **See [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md)** for the simple 3-step pattern.

**Example**: Add `ponumber`, `shipvia`, or any other fields to your form in minutes.

---

## Files in This Repo

### Your Implementation Files
```
java/OEDetailLookup.java      ← Java agent (query logic)
js/oe-lookup.js               ← JavaScript (form integration)
```

### For Testing (Optional)
```
sql/create_oe_detail.sql      ← Sample data (for testing only)
test/test-api-call.sh         ← cURL test script
tests/TestJsonParsing.java    ← Unit tests (validates parsing)
```

### Documentation
```
BILL_QUICK_START.md           ← START HERE (your 6 steps)
EXTEND_WITH_MORE_FIELDS.md    ← How to add more columns
PRODUCTION_NOTES.md           ← Deployment checklist
ARCHITECTURE.md               ← Design decisions
```

---

## How It Works (Your Pattern)

### 1. Java Agent (Your Logic)
```java
// Receive invoice number via Ajax POST
String invoiceNo = extractParameterValue(requestBody, "invoice");

// Query Databricks REST API
// (Your SQL adapts to your actual table)
String sqlStatement = "SELECT custno, custname, orderdate, shipdate " +
  "FROM prd_gold.facts.oe_detail WHERE invoice_no = :invoice_no";

// Parse JSON response into variables
String custno = values[0];
String custname = values[1];
// ... etc

// Print delimited for JavaScript
writer.println(custno + "~*~" + custname + "~*~" + orderdate + "~*~" + shipdate);
```

### 2. JavaScript (Your Form)
```javascript
// Receive response
response = response.trim();

// Split on your delimiter
var parts = response.split("~*~");

// Extract variables (Bill's pattern)
var custNo = parts[0];
var custName = parts[1];
var orderDate = parts[2];
var shipDate = parts[3];

// Populate form (jQuery)
$("#CustomerNo").val(custNo);
$("#CustomerName").val(custName);
$("#OrderDate").val(orderDate);
$("#ShipDate").val(shipDate);
```

---

## Prerequisites

✅ Databricks workspace with SQL warehouse  
✅ Your company's delta table (or similar)  
✅ Databricks PAT token  
✅ Domino Designer (for deployment)  

---

## Key Features

✅ **Parameterized SQL** — Safe from injection  
✅ **Secure credentials** — Stored in Domino environment document  
✅ **Production timeouts** — Works with cold warehouses (30s+)  
✅ **Error handling** — Clear messages for debugging  
✅ **Extensible pattern** — Add fields easily as you discover tables  

---

## Next Steps

### 👉 **Step 1**: Read [BILL_QUICK_START.md](./BILL_QUICK_START.md)
Your 6-step implementation guide. Follow exactly.

### 👉 **Step 2**: Set Up
1. Gather Databricks info (host, PAT, warehouse ID)
2. Configure Domino environment document
3. Import Java agent
4. Add JavaScript to form
5. Add form fields
6. Test with your invoice numbers

### 👉 **Step 3**: When You Get Table Access
1. See what columns are available in other tables
2. Follow [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md)
3. Add fields in 3 simple steps

---

## Important: DatabasePath Hidden Field

**You MUST add this to your Domino form** (non-negotiable):

```html
<input id="DatabasePath" type="hidden" value="your-database.nsf" />
```

Replace `your-database.nsf` with your actual database filename.

**Without this**: Form will show error on Lookup click.

---

## Testing (Optional)

To test Databricks connectivity before deployment:

```bash
./test/test-api-call.sh "INV-2024-001"
```

(Replace with your actual invoice number from your table)

---

## Questions?

- **How do I add more fields?** → See [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md)
- **How do I deploy?** → See [BILL_QUICK_START.md](./BILL_QUICK_START.md) Step 6
- **Design details?** → See [ARCHITECTURE.md](./ARCHITECTURE.md)
- **Before going live?** → See [PRODUCTION_NOTES.md](./PRODUCTION_NOTES.md)

---

## GitHub Repository

https://github.com/slysik/databricks-domino-rest-integration

---

**Ready to implement?** → [BILL_QUICK_START.md](./BILL_QUICK_START.md) ✅
