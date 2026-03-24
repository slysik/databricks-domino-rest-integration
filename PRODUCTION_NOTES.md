# Production Deployment Notes

**Final checklist before going live**

---

## Pre-Deployment

### Databricks

- [ ] Table exists: `SELECT COUNT(*) FROM prd_gold.facts.oe_detail;` (use YOUR table)
- [ ] SQL warehouse is running
- [ ] PAT token generated and working
- [ ] Test query works in SQL editor:
  ```sql
  SELECT custno, custname, orderdate, shipdate 
  FROM prd_gold.facts.oe_detail 
  WHERE invoice_no = 'INV-2024-001';  -- use your actual invoice
  ```

### Domino

- [ ] Java agent imported: `OEDetailLookup`
  - Verify: Design → Agents → `OEDetailLookup` appears
  - Properties → Agent tab → Interpreter = "Java"
- [ ] JavaScript added to form: `js/oe-lookup.js`
- [ ] Form fields added (with correct IDs):
  - `InvoiceNo` (input)
  - `LookupBtn` (button)
  - `CustomerNo`, `CustomerName`, `OrderDate`, `ShipDate` (disabled inputs)
  - **`DatabasePath` hidden field** (CRITICAL)
- [ ] Database ACL configured:
  - File → Database Access Control
  - Your user has Editor or Manager role
  - `[AgentExecutor]` role checked (if role-based)

### Configuration

- [ ] Domino environment document created with 3 fields:
  - `DATABRICKS_HOST`: Your workspace host
  - `DATABRICKS_TOKEN`: Your PAT (Password type field)
  - `WAREHOUSE_ID`: Your SQL warehouse ID

---

## Testing

### Quick Test (In Domino)

1. Open your form
2. Enter invoice number from your table
3. Click Lookup
4. Verify fields populate in 2-3 seconds
5. Try another invoice (test different data)
6. Try invalid invoice (test error handling)
7. Test NULL field if you have one (should show empty)

### Test Results Expected

| Input | Expected Result |
|-------|---|
| Valid invoice (e.g., INV-001) | Fields populate ✓ |
| Invalid invoice (e.g., BAD-123) | Error: "No invoice found" ✓ |
| Empty invoice | Error: "Invalid or missing invoice number" ✓ |

### Test with cURL (Optional)

```bash
./test/test-api-call.sh "INV-2024-001"
```

Expected output:
```
CHEW-SC~*~Chew-leston Charms Trading Co~*~2024-01-05~*~2024-01-10
```

---

## After Deployment

### Monitoring

✅ **First day**: Monitor for errors
- Check Domino console for exceptions
- Test with various invoice numbers
- Verify error messages are clear

✅ **First week**: Verify performance
- Response time should be 2-3 seconds consistently
- Cold warehouse (first query) might take 30s (normal)
- Subsequent queries are instant (warehouse is warm)

✅ **Ongoing**: Track usage
- Document which invoices users typically search
- Note if any queries are consistently slow
- Monitor warehouse costs (if applicable)

---

## Troubleshooting (If Something Fails)

| Symptom | Check | Fix |
|---------|-------|-----|
| "Connection refused" | Is Databricks workspace accessible? | Verify `DATABRICKS_HOST` in environment doc |
| "Invalid token" | Is PAT token still valid? | Generate new token in Databricks Settings |
| "No warehouse found" | Is warehouse ID correct? | Copy exact ID from Databricks SQL Warehouses |
| "Timeout" | Cold warehouse (first query) takes >30s | This is normal; wait or start warehouse first |
| "No invoice found" | Does invoice exist in table? | Verify invoice in Databricks: `SELECT * FROM ... WHERE invoice_no = '...'` |
| "Form doesn't change" | Is JavaScript reading right field IDs? | Check form HTML: `id="CustomerName"` must match JavaScript |
| "Form stuck (loading forever)" | Missing `DatabasePath` field? | Add: `<input id="DatabasePath" type="hidden" value="your-db.nsf" />` |

---

## If You Get Access to More Tables

1. See [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md)
2. Follow the 3-step pattern
3. Test with same checklist above

---

## Security Notes

✅ **PAT is encrypted** in Domino environment document (not in code)  
✅ **SQL is parameterized** — safe from injection  
✅ **HTTPS enforced** for all Databricks API calls  
✅ **User credentials** — only you can see them (Domino ACL controls access)

---

## Backup & Recovery

**Before going live**:
1. Export your Domino database: File → Export
2. Export your environment document settings
3. Save a screenshot of your working form layout

**If something breaks**:
1. You can always re-import the Java agent
2. You can always re-paste the JavaScript
3. Your Databricks data is untouched (this tool only reads)

---

## Timeline

**Your expected sequence**:
1. Week 1: Implement (6 steps from QUICK_START.md)
2. Week 2: Test with your own invoices
3. Week 3: Go live
4. Week 4+: Add more fields as you discover other tables

---

## Success Criteria

✅ You deployed the code successfully  
✅ Lookup works for your invoice numbers  
✅ Form fields populate correctly  
✅ Error handling works (bad invoice → clear message)  
✅ No Domino agent errors in console  

When all 5 are true, you're done! 🎉

---

## Need Help?

- **Setup question?** → [BILL_QUICK_START.md](./BILL_QUICK_START.md)
- **Adding fields?** → [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md)
- **Design details?** → [ARCHITECTURE.md](./ARCHITECTURE.md)

---

**You've got this!** ✅
