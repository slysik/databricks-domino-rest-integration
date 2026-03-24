# Architecture & Design Decisions

## System Design

This document explains the architectural choices and design patterns used in the Databricks-Domino integration.

---

## Component Breakdown

### 1. Frontend: JavaScript (`oe-lookup.js`)

**Technology**: jQuery + vanilla JavaScript

**Responsibilities**:
- Validate user input (invoice number)
- Make Ajax call to Domino Java agent
- Parse `~*~` delimited response
- Format dates (YYYY-MM-DD → MM/DD/YYYY)
- Handle null values (display as empty string)
- Populate form fields
- Show error/success messages

**Why jQuery?**
- Available in all Domino environments
- Handles browser compatibility issues
- Simplifies Ajax calls and DOM manipulation

**Why `~*~` delimiter?**
- Not found in typical business data
- Easy to split in any language
- Avoids comma/quote escaping issues
- Works with nullable fields (empty values)

**Error Handling**:
- Network errors: "Unable to connect to server"
- Timeout errors: "Request timed out"
- Server errors: Display error message from Java agent
- Parse errors: Validate field count before populating

---

### 2. Backend: Java Agent (`OEDetailLookup.java`)

**Technology**: HCL Domino Java agent (extends AgentBase)

**Responsibilities**:
- Read POST body from Domino `Request_Content` CGI variable
- URL-decode the request body
- Extract invoice parameter
- Validate invoice number
- Read Databricks configuration from environment document
- Make HTTPS request to Databricks SQL Statement API
- Parse JSON response (quote-aware extraction)
- Check query state (`status.state`)
- Extract first row from `data_array`
- Return formatted `~*~` delimited string
- Handle errors gracefully

**Why Java?**
- Native support in Domino
- Better error handling than @Formula language
- Can manage HTTPS connections
- Can parse JSON manually (no external deps needed)

**Why quote-aware JSON parsing instead of javax.json?**
- Domino JVM may not include javax.json library
- Manual parsing is lightweight and reliable
- Single method `parseDataArrayValues()` handles all cases
- No external dependencies = easier deployment

**Domino-Specific Patterns**:

1. **Request_Content CGI variable**: Domino doesn't expose HTTP request body directly. Must read via document context:
   ```java
   Document docContext = agentContext.getDocumentContext();
   String requestContent = docContext.getItemValueString("Request_Content");
   ```

2. **Content-Type header**: Must write HTTP headers explicitly before response body:
   ```java
   writer.println("Content-Type: text/plain");
   writer.println();  // Blank line
   writer.flush();
   ```

3. **Environment document configuration**: PAT is read from Domino environment, not hardcoded:
   ```java
   String token = session.getEnvironmentString("DATABRICKS_TOKEN", true);
   ```

4. **Stream cleanup**: All streams must be closed in finally block:
   ```java
   finally {
     if (inputStream != null) inputStream.close();
     if (connection != null) connection.disconnect();
   }
   ```

**Error Codes**:
- `400`: Invalid input (checked locally)
- `401`: Unauthorized (invalid PAT)
- `403`: Forbidden (insufficient permissions)
- `404`: Not found (bad warehouse ID)
- `500`: Server error (warehouse issue)
- `PENDING/RUNNING`: Query still executing
- `FAILED`: Query error (syntax or permissions)

---

### 3. Data Source: Databricks Table

**Schema**: `prd_gold.facts.oe_detail`

**Columns**:
- `invoice_no` (STRING, PRIMARY KEY) — Lookup key
- `custno` (STRING) — Customer number
- `custname` (STRING) — Customer name (can include commas)
- `orderdate` (DATE) — Order placed
- `shipdate` (DATE, NULLABLE) — Shipped (or NULL if pending)
- `ponumber`, `shipvia`, `totalamt`, `orderstatus` (placeholders)

**Why this design?**
- Fact table model (narrow fact, broad dimensions optional)
- Lookup by single key (invoice_no) is efficient
- Nullable shipdate represents unshipped orders
- DECIMAL(38,2) for monetary amounts (safe for SUM aggregations)

**Sample data**: 20 invoices
- Customers: CUST001-CUST008 (some repeat)
- Dates: 2024-01-05 through 2024-06-25
- South Carolina pun company names (Chew-leston Charms, Myrtle Be Serious, etc.)
- 3 NULL shipdates (tests nullable column handling)

---

### 4. Databricks REST API Integration

**Endpoint**: `/api/2.0/sql/statements` (SQL Statement Execution API)

**Why REST instead of JDBC?**
- No JDBC driver needed in Domino
- Firewall-friendly (standard HTTPS)
- Async/polling support (handles slow queries)
- Standard HTTP (no special setup)

**Request Format**:
```json
{
  "warehouse_id": "...",
  "statement": "SELECT ... WHERE col = :param",
  "parameters": [{"name": "param", "value": "val", "type": "STRING"}],
  "wait_timeout": "30s"
}
```

**Why parameterized queries?**
- Prevents SQL injection
- Handles special characters automatically
- Scales to multi-parameter queries

**Response Polling**:
- `status.state`: PENDING → RUNNING → SUCCEEDED (or FAILED)
- Agent waits up to 30 seconds (configured by `wait_timeout`)
- Returns immediately if query finishes early

**Response Parsing**:
```json
{
  "result": {
    "data_array": [
      ["val1", "val2", "val3", "val4"]
    ]
  }
}
```

- Real-world company names with hyphens and special characters tested
- Null values: `null` (unquoted, handled by `sanitizeValue()`)
- Multiple rows: Only first row used (noted in code)

---

## Security Architecture

### Credential Management

```
┌─────────────────────────────┐
│  Domino Environment Doc     │
│  (encrypted at rest)        │
├─────────────────────────────┤
│  DATABRICKS_HOST            │ (public)
│  DATABRICKS_TOKEN           │ (encrypted)
│  WAREHOUSE_ID               │ (public)
└──────────────┬──────────────┘
               │
               ▼ (read via session.getEnvironmentString)
         ┌──────────────┐
         │ Java Agent   │
         │ (runtime)    │
         └──────┬───────┘
                │
                ▼ (in Authorization header)
         ┌──────────────────────┐
         │ HTTPS/Bearer token   │
         │ Databricks API       │
         └──────────────────────┘
```

**Why environment document?**
- Never stored in source code
- Encrypted by Domino at rest
- Easy to rotate without recompiling
- Different values per workspace (dev/test/prod)

### Input Validation

```
Invoice Input → Trim → Length check → Format check → Parameterized Query
                  (0-50)              (alphanumeric)
```

**Why parameterized?**
- `SELECT ... WHERE col = :param` prevents SQL injection
- Special chars handled automatically
- Databricks API validates parameter types

### Network Security

- HTTPS only (enforced by `https://` in URL)
- Bearer token authentication
- TLS certificate validation (Java default)
- No credentials in logs or error messages

---

## Data Flow Diagram

```
User Input (Invoice #)
    │
    ▼
JavaScript Validation
    │
    ├─► Alert if empty
    │
    ▼ (encodeURIComponent)
Ajax POST to Agent
    │
    ├─► Content-Type: application/x-www-form-urlencoded
    │
    ▼
Request_Content (Domino CGI variable)
    │
    ├─► URL-decode
    ├─► Extract "invoice=" parameter
    ├─► Validate length (0-50)
    │
    ▼
Read Databricks Config (Environment Doc)
    │
    ├─► DATABRICKS_HOST
    ├─► DATABRICKS_TOKEN
    ├─► WAREHOUSE_ID
    │
    ▼
Build JSON Request
    │
    ├─► Parameterized query
    ├─► Wait timeout: 30s
    │
    ▼
HTTPS POST to Databricks
    │
    ├─► Authorization: Bearer <token>
    ├─► Content-Type: application/json
    │
    ▼
Check status.state
    │
    ├─► PENDING/RUNNING → ERROR
    ├─► FAILED → ERROR
    ├─► SUCCEEDED → proceed
    │
    ▼
Extract data_array
    │
    ├─► Empty → NOTFOUND
    ├─► Multiple rows → Use first
    │
    ▼
Parse Values (quote-aware)
    │
    ├─► Handle commas in quoted strings
    ├─► Handle null literals
    ├─► Strip quotes
    │
    ▼
Format Response
    │
    ├─► custno~*~custname~*~orderdate~*~shipdate
    │
    ▼
Write to HTTP Response
    │
    ├─► Content-Type: text/plain
    │
    ▼
JavaScript Parse & Format
    │
    ├─► Split on ~*~
    ├─► Check for ERROR/NOTFOUND
    ├─► Format dates (YYYY-MM-DD → MM/DD/YYYY)
    ├─► Handle null/"null" values
    │
    ▼
Populate Form Fields
    │
    ├─► $("#CustomerNo").val(custno)
    ├─► $("#CustomerName").val(custname)
    ├─► $("#OrderDate").val(orderdate)
    ├─► $("#ShipDate").val(shipdate)
    │
    ▼
Display Success Message
```

---

## Extension Points

### Add a New Field

From 4 fields → 5 fields example (adding PO number):

1. **SQL**: Add column to SELECT
2. **Java**: Update response format (values[4])
3. **JavaScript**: Update field count check and field mapping
4. **Form**: Add new input field with matching ID

See **README.md → Extending for New Fields** for step-by-step.

### Change Query Logic

Replace `WHERE invoice_no = :invoice_no` with:
- `WHERE custno = :custno` (lookup by customer)
- `WHERE orderdate BETWEEN :start_date AND :end_date` (lookup by date range)
- `WHERE custname LIKE :pattern` (lookup by customer name)

Requires:
- Update SQL in Java agent
- Update JavaScript parameter sending
- Update form field bindings

### Add Authentication (Domino)

Current: No Domino-level auth (agent runs as database owner)

To restrict:
1. In Designer → Agent → Security tab → "Run as web user"
2. Domino checks document ACL before executing

---

## Performance Characteristics

### Latency Breakdown (typical)

| Component | Time | Notes |
|-----------|------|-------|
| JavaScript validation | <5ms | Sync |
| Ajax POST to agent | 10-50ms | Network |
| Java agent execution | 50-200ms | Parse + setup |
| Databricks query | 1-5s | Data fetch |
| JSON parsing | 20-50ms | Quote-aware extraction |
| Total | 2-5s | Dominated by query time |

### Scalability

- **Data**: Table can grow to 100M+ rows (partition by orderdate)
- **Concurrency**: Databricks SQL warehouse auto-scales
- **Query speed**: Add index on invoice_no for instant lookup
- **Agent execution**: Domino thread pool handles multiple concurrent requests

---

## Alternatives Considered

### 1. JDBC instead of REST

**Pros**: Direct DB connection, faster
**Cons**: Requires JDBC driver in Domino, firewall/port complications, SSL setup

**Why REST won**: Simpler, no external JARs, works through firewall

### 2. Direct Spark API (PySpark) instead of SQL API

**Pros**: More flexibility, can join multiple tables
**Cons**: Agent would be PySpark, not Java; no Databricks ecosystem

**Why SQL API won**: Simpler, SQL is universal, REST is standard

### 3. javax.json for JSON parsing instead of manual

**Pros**: Type-safe, no custom parsing
**Cons**: Domino JVM may not include it, adds JAR dependency

**Why manual won**: No external deps, simpler deployment, still 200 lines of code

### 4. Document-based lookup (embed table in database) instead of REST

**Pros**: No network calls, faster
**Cons**: Data not in Databricks, defeats the purpose, ETL complexity

**Why Databricks won**: Single source of truth, scales for 100M rows

---

## Testing Strategy

### Unit Tests (Java)

```java
@Test
public void testExtractParameterValue() {
  String result = extractParameterValue("invoice=INV-001", "invoice");
  assertEquals("INV-001", result);
}

@Test
public void testSanitizeValue() {
  assertEquals("", sanitizeValue("null"));
  assertEquals("test", sanitizeValue("\"test\""));
}
```

### Integration Tests

1. **Databricks connectivity**: `test-api-call.sh` script
2. **Agent execution**: Run agent in Designer, check output
3. **Form integration**: Manual testing with browser dev tools

### Load Testing

Use Apache JMeter or similar to simulate concurrent users:
```
Thread Group: 10 threads, 100 requests/thread
Target: Domino agent
Expected: <5s response time, no errors
```

---

## Future Improvements

1. **Caching**: Cache lookup results for 5 minutes to reduce API calls
2. **Async execution**: Return immediately with polling ID instead of waiting
3. **Batch lookup**: Accept multiple invoice numbers, return CSV
4. **Analytics**: Log all lookups to Databricks for analytics
5. **Multi-tenant**: Add customer ID to queries for isolation

---

## References

- [Databricks SQL API Docs](https://docs.databricks.com/api/workspace/statementexecution)
- [Domino Java Agent Security Model](https://www.ibm.com/support/knowledgecenter/SSVRGU_9.0.0/admin/cag_agentsec.html)
- [OWASP SQL Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
