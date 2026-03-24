# Extending with More Fields

**Simple 3-step pattern to add columns from other tables**

When you get access and see what columns are in the other tables, follow this pattern.

---

## Scenario

You've implemented the basic 4 fields:
- custno
- custname  
- orderdate
- shipdate

Now you want to add more fields:
- ponumber (from prd_gold.facts.oe_detail)
- shipvia (from prd_gold.facts.oe_detail)
- customername (from prd_gold.facts.customer — different table, needs JOIN)

---

## 3-Step Extension

### Step 1: Update Java Agent SQL Query

**File**: `java/OEDetailLookup.java`

**Find this line** (around line 122):
```java
String sqlStatement = "SELECT custno, custname, orderdate, shipdate FROM prd_gold.facts.oe_detail WHERE invoice_no = :invoice_no";
```

**Replace with** (add your new fields):
```java
String sqlStatement = "SELECT custno, custname, orderdate, shipdate, ponumber, shipvia " +
  "FROM prd_gold.facts.oe_detail WHERE invoice_no = :invoice_no";
```

**If joining other tables** (example):
```java
String sqlStatement = "SELECT " +
  "d.custno, d.custname, d.orderdate, d.shipdate, d.ponumber, d.shipvia, " +
  "c.customername " +
  "FROM prd_gold.facts.oe_detail d " +
  "INNER JOIN prd_gold.facts.customer c ON d.custno = c.custno " +
  "WHERE d.invoice_no = :invoice_no";
```

---

### Step 2: Update Java Agent Output

**File**: `java/OEDetailLookup.java`

**Find this section** (around line 350-355):
```java
// Format response
String result = values[0] + DELIMITER + values[1] + DELIMITER + values[2] + DELIMITER + values[3];
writer.println(result);
```

**Replace with** (for your new fields):
```java
// Format response — add more fields to delimited string
String result = values[0] + DELIMITER + values[1] + DELIMITER + values[2] + DELIMITER + 
                values[3] + DELIMITER + values[4] + DELIMITER + values[5] + DELIMITER + values[6];
writer.println(result);
// Returns: custno~*~custname~*~orderdate~*~shipdate~*~ponumber~*~shipvia~*~customername
```

---

### Step 3: Update JavaScript Form Population

**File**: `js/oe-lookup.js`

**Find this section** (around line 110-120):
```javascript
var custNo = getValue(parts[0]);
var custName = getValue(parts[1]);
var orderDate = formatDate(getValue(parts[2]));
var shipDate = formatDate(getValue(parts[3]));

$("#CustomerNo").val(custNo);
$("#CustomerName").val(custName);
$("#OrderDate").val(orderDate);
$("#ShipDate").val(shipDate);
```

**Replace with** (add your new fields):
```javascript
var custNo = getValue(parts[0]);
var custName = getValue(parts[1]);
var orderDate = formatDate(getValue(parts[2]));
var shipDate = formatDate(getValue(parts[3]));
var poNumber = getValue(parts[4]);
var shipVia = getValue(parts[5]);
var customerName = getValue(parts[6]);

$("#CustomerNo").val(custNo);
$("#CustomerName").val(custName);
$("#OrderDate").val(orderDate);
$("#ShipDate").val(shipDate);
$("#PONumber").val(poNumber);
$("#ShipVia").val(shipVia);
$("#CustomerFullName").val(customerName);
```

---

### Step 4: Update Domino Form

Add HTML field IDs to match the JavaScript:

```html
<label>PO Number:</label>
<input id="PONumber" type="text" disabled />

<label>Ship Via:</label>
<input id="ShipVia" type="text" disabled />

<label>Customer Full Name:</label>
<input id="CustomerFullName" type="text" disabled />
```

---

## That's It!

The pattern scales infinitely:
- **4 fields** → Same pattern
- **10 fields** → Same pattern  
- **20 fields** → Same pattern

Just follow these 3 steps for each batch of new fields.

---

## Example: Realistic Scenario

**You discover**:
- `order_status` in prd_gold.facts.oe_detail
- `discount_pct` in prd_gold.facts.oe_detail
- `sales_rep_name` in prd_gold.facts.sales_rep (JOIN needed)
- `account_manager` in prd_gold.facts.customer (already joining)

**You do**:

### Step 1: Update SQL
```java
String sqlStatement = "SELECT " +
  "d.custno, d.custname, d.orderdate, d.shipdate, d.order_status, d.discount_pct, " +
  "s.sales_rep_name, " +
  "c.account_manager " +
  "FROM prd_gold.facts.oe_detail d " +
  "INNER JOIN prd_gold.facts.sales_rep s ON d.sales_rep_id = s.sales_rep_id " +
  "INNER JOIN prd_gold.facts.customer c ON d.custno = c.custno " +
  "WHERE d.invoice_no = :invoice_no";
```

### Step 2: Update Output
```java
String result = values[0] + DELIMITER + values[1] + DELIMITER + values[2] + DELIMITER +
                values[3] + DELIMITER + values[4] + DELIMITER + values[5] + DELIMITER +
                values[6] + DELIMITER + values[7];
writer.println(result);
```

### Step 3: Update JavaScript
```javascript
var orderStatus = getValue(parts[4]);
var discountPct = getValue(parts[5]);
var repName = getValue(parts[6]);
var accountMgr = getValue(parts[7]);

$("#OrderStatus").val(orderStatus);
$("#DiscountPct").val(discountPct);
$("#SalesRepName").val(repName);
$("#AccountManager").val(accountMgr);
```

Done! ✅

---

## Notes

**JSON formatting**:
- Dates come as YYYY-MM-DD from Databricks
- JavaScript `formatDate()` converts to MM/DD/YYYY
- For other data types, just pass as-is: `getValue(parts[N])`

**NULL values**:
- If a field is NULL in Databricks → comes through as empty string
- Form fields just show nothing (expected behavior)

**Error handling**:
- If any query fails → First field will be "ERROR" or "NOTFOUND"
- JavaScript checks this automatically (existing code)
- User sees error message, can retry

**Performance**:
- One query (even with 5 JOINs) = one Databricks request = instant response
- No change to response time as you add fields

---

## If You Need Help

1. **Table doesn't have the column?** → Check table schema: `DESCRIBE prd_gold.facts.table_name;`
2. **JOIN syntax wrong?** → Test in Databricks SQL editor first, then copy to Java agent
3. **Form field not updating?** → Check jQuery selector matches your HTML ID exactly (case-sensitive)

---

## Ready?

**Go back to**: [README.md](./README.md) or [BILL_QUICK_START.md](./BILL_QUICK_START.md)

---

**The key insight**: Your ~*~ delimiter pattern makes this trivially simple. No XML parsing, no complex JSON navigation. Just split and assign. 🎯
