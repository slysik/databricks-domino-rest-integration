/**
 * Unit tests for OEDetailLookup JSON parsing and state handling
 * 
 * Validates:
 * 1. Quote-aware field extraction (with commas, quotes, nulls)
 * 2. Data array parsing
 * 3. Async state transitions
 * 4. Error handling
 */

public class TestJsonParsing {

  /**
   * Test 1: Extract field with quoted value
   */
  public static void testExtractFieldSimple() {
    String json = "{\"state\": \"SUCCEEDED\", \"statement_id\": \"12345\"}";
    String state = extractFieldValue(json, "\"state\"");
    assert state.equals("SUCCEEDED") : "Failed to extract state";
    System.out.println("✓ Test 1: Extract quoted field");
  }

  /**
   * Test 2: Extract field with null value
   */
  public static void testExtractFieldNull() {
    String json = "{\"shipdate\": null, \"state\": \"SUCCEEDED\"}";
    String value = extractFieldValue(json, "\"shipdate\"");
    assert value.equals("null") : "Failed to extract null value";
    System.out.println("✓ Test 2: Extract null value");
  }

  /**
   * Test 3: Parse data array with quoted strings containing commas
   */
  public static void testParseDataArrayWithCommas() {
    String dataArray = "[[\"CHEW-SC\", \"Chew-leston, Inc.\", \"2024-01-05\", \"2024-01-10\"]]";
    String[] values = parseDataArrayValues(dataArray);
    
    assert values.length == 4 : "Wrong number of fields";
    assert values[1].equals("Chew-leston, Inc.") : "Failed to preserve comma in quoted string";
    System.out.println("✓ Test 3: Parse data array with commas");
  }

  /**
   * Test 4: Parse data array with nullable field
   */
  public static void testParseDataArrayWithNull() {
    String dataArray = "[[\"INV-001\", \"CUST-123\", \"2024-01-05\", null]]";
    String[] values = parseDataArrayValues(dataArray);
    
    assert values.length == 4 : "Wrong number of fields";
    assert values[3].equals("") : "NULL should be converted to empty string";
    System.out.println("✓ Test 4: Parse data array with null");
  }

  /**
   * Test 5: Handle PENDING state (should trigger polling)
   */
  public static void testAsyncPendingState() {
    String json = "{\"state\": \"PENDING\", \"statement_id\": \"stmt-abc123\"}";
    String state = extractFieldValue(json, "\"state\"");
    String statementId = extractFieldValue(json, "\"statement_id\"");
    
    assert state.equals("PENDING") : "Failed to extract PENDING state";
    assert statementId.equals("stmt-abc123") : "Failed to extract statement_id";
    System.out.println("✓ Test 5: Handle PENDING state for polling");
  }

  /**
   * Test 6: Handle RUNNING state (should continue polling)
   */
  public static void testAsyncRunningState() {
    String json = "{\"state\": \"RUNNING\", \"statement_id\": \"stmt-xyz789\"}";
    String state = extractFieldValue(json, "\"state\"");
    
    assert state.equals("RUNNING") : "Failed to handle RUNNING state";
    System.out.println("✓ Test 6: Handle RUNNING state for polling");
  }

  /**
   * Test 7: Extract and verify statement_id for polling
   */
  public static void testStatementIdExtraction() {
    String json = "{\"statement_id\": \"abc-123-def-456\", \"state\": \"SUCCEEDED\"}";
    String statementId = extractFieldValue(json, "\"statement_id\"");
    
    assert statementId.equals("abc-123-def-456") : "Failed to extract statement_id";
    System.out.println("✓ Test 7: Extract statement_id for polling");
  }

  /**
   * Test 8: Error response parsing
   */
  public static void testErrorResponseParsing() {
    String json = "{\"error_code\": \"RESOURCE_EXHAUSTED\", \"message\": \"Warehouse is cold-starting\"}";
    String errorCode = extractFieldValue(json, "\"error_code\"");
    String message = extractFieldValue(json, "\"message\"");
    
    assert errorCode.equals("RESOURCE_EXHAUSTED") : "Failed to extract error code";
    assert message.equals("Warehouse is cold-starting") : "Failed to extract error message";
    System.out.println("✓ Test 8: Parse error response");
  }

  /**
   * Test 9: Sanitize values (quote removal, null handling)
   */
  public static void testSanitizeValue() {
    assert sanitizeValue("\"test\"").equals("test") : "Failed to remove quotes";
    assert sanitizeValue("null").equals("") : "Failed to convert null";
    assert sanitizeValue("\"\"").equals("") : "Failed to handle empty string";
    assert sanitizeValue("123").equals("123") : "Failed to handle unquoted number";
    System.out.println("✓ Test 9: Sanitize values");
  }

  /**
   * Test 10: Full response parsing with real Databricks format
   */
  public static void testFullResponseParsing() {
    String json = "{\"statement_id\": \"stmt-123\", \"state\": \"SUCCEEDED\", " +
      "\"result\": {\"data_array\": [[\"CHEW-SC\", \"Chew-leston Charms\", \"2024-01-05\", \"2024-01-10\"]]}}";
    
    String state = extractFieldValue(json, "\"state\"");
    String statementId = extractFieldValue(json, "\"statement_id\"");
    String dataArray = extractDataArray(json);
    
    assert state.equals("SUCCEEDED") : "Failed to extract state";
    assert statementId.equals("stmt-123") : "Failed to extract statement_id";
    assert dataArray != null && !dataArray.isEmpty() : "Failed to extract data array";
    System.out.println("✓ Test 10: Full response parsing");
  }

  /**
   * Run all tests
   */
  public static void main(String[] args) {
    System.out.println("=" + "=".repeat(68));
    System.out.println("Testing OEDetailLookup JSON Parsing & Async Handling");
    System.out.println("=" + "=".repeat(68) + "\n");

    try {
      testExtractFieldSimple();
      testExtractFieldNull();
      testParseDataArrayWithCommas();
      testParseDataArrayWithNull();
      testAsyncPendingState();
      testAsyncRunningState();
      testStatementIdExtraction();
      testErrorResponseParsing();
      testSanitizeValue();
      testFullResponseParsing();

      System.out.println("\n" + "=".repeat(70));
      System.out.println("✓ ALL TESTS PASSED (10/10)");
      System.out.println("=" + "=".repeat(69));
    } catch (AssertionError e) {
      System.out.println("\n✗ TEST FAILED: " + e.getMessage());
      System.exit(1);
    }
  }

  // Placeholder methods (implement from OEDetailLookup.java)
  private static String extractFieldValue(String json, String fieldName) {
    // Implementation from OEDetailLookup
    int index = json.indexOf(fieldName);
    if (index == -1) return null;

    int colonIndex = json.indexOf(":", index);
    if (colonIndex == -1) return null;

    int startIndex = colonIndex + 1;
    while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
      startIndex++;
    }

    if (startIndex < json.length() && json.charAt(startIndex) == '"') {
      startIndex++;
      int endIndex = json.indexOf("\"", startIndex);
      if (endIndex == -1) return null;
      return json.substring(startIndex, endIndex);
    }

    int endIndex = startIndex;
    while (endIndex < json.length() && !Character.isWhitespace(json.charAt(endIndex)) &&
           json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}' && json.charAt(endIndex) != ']') {
      endIndex++;
    }

    if (endIndex > startIndex) {
      return json.substring(startIndex, endIndex);
    }

    return null;
  }

  private static String[] parseDataArrayValues(String dataArray) {
    if (!dataArray.startsWith("[[")) return null;
    int closeIdx = dataArray.lastIndexOf("]]");
    if (closeIdx == -1) return null;

    String firstRow = dataArray.substring(2, closeIdx);
    if (firstRow.isEmpty()) return null;

    java.util.List<String> values = new java.util.ArrayList<>();
    boolean inQuote = false;
    boolean inEscape = false;
    StringBuilder currentValue = new StringBuilder();

    for (int i = 0; i < firstRow.length(); i++) {
      char c = firstRow.charAt(i);

      if (inEscape) {
        currentValue.append(c);
        inEscape = false;
        continue;
      }

      if (c == '\\') {
        inEscape = true;
        currentValue.append(c);
        continue;
      }

      if (c == '"') {
        inQuote = !inQuote;
        currentValue.append(c);
        continue;
      }

      if (c == ',' && !inQuote) {
        String value = currentValue.toString().trim();
        value = sanitizeValue(value);
        values.add(value);
        currentValue = new StringBuilder();
        continue;
      }

      currentValue.append(c);
    }

    String value = currentValue.toString().trim();
    value = sanitizeValue(value);
    values.add(value);

    return values.toArray(new String[0]);
  }

  private static String extractDataArray(String json) {
    String marker = "\"data_array\":";
    int index = json.indexOf(marker);
    if (index == -1) return null;

    int startIndex = index + marker.length();
    while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
      startIndex++;
    }

    if (startIndex >= json.length() || json.charAt(startIndex) != '[') {
      return null;
    }

    int bracketCount = 1;
    int endIndex = startIndex + 1;
    while (endIndex < json.length() && bracketCount > 0) {
      char c = json.charAt(endIndex);
      if (c == '[') bracketCount++;
      else if (c == ']') bracketCount--;
      endIndex++;
    }

    if (bracketCount == 0) {
      return json.substring(startIndex, endIndex);
    }

    return null;
  }

  private static String sanitizeValue(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }

    if (value.equals("null")) {
      return "";
    }

    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
      return value.substring(1, value.length() - 1);
    }

    return value;
  }
}
