# Testing

## Current Status

⚠️ **Unit tests need to be refactored** to test actual production code.

See [HONEST_ASSESSMENT.md](../HONEST_ASSESSMENT.md#3--medium-unit-tests-dont-test-production-code) for details.

---

## What Should Be Done

### TestJsonParsing.java (Current)
- ❌ Copies helper methods from OEDetailLookup instead of importing them
- ❌ Tests can pass while production code fails
- ⚠️ Filename matches class name (good), but tests are isolated

### What Needs to Happen

1. **Remove copied methods** from TestJsonParsing.java
2. **Import OEDetailLookup** as a class
3. **Call actual production methods**:
   ```java
   // WRONG (current)
   private static String extractFieldValue(...) { ... }  // copy
   
   // RIGHT (needed)
   OEDetailLookup agent = new OEDetailLookup();
   String result = agent.extractFieldValue(...);  // real method
   ```

4. **Add to CI pipeline** to prevent regressions

---

## Integration Tests Needed

Before recommending to Bill:

- [ ] Test against real Databricks workspace
- [ ] Cold warehouse scenario (30s+ startup)
- [ ] Timeout validation (100s browser timeout)
- [ ] Uniqueness enforcement (PRIMARY KEY test)
- [ ] Error message completeness (escape sequences)
- [ ] UI state management (error/success cleanup)

---

## How to Run Current Tests

```bash
cd tests
javac TestJsonParsing.java
java TestJsonParsing
```

**Note**: These tests are isolated and don't validate production code.

---

## Test Coverage

| Component | Coverage | Status |
|-----------|----------|--------|
| OEDetailLookup.java | ❌ 0% (tests are copies) | Needs refactor |
| oe-lookup.js | ⚠️ Manual testing only | Needs Jest/Mocha |
| SQL | ⚠️ Manual testing only | Needs schema validation |
| Integration | ❌ 0% | Needs pytest/bash tests |

---

## Next Steps

See [HONEST_ASSESSMENT.md](../HONEST_ASSESSMENT.md) for the full checklist.
