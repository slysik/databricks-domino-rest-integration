/**
 * OE Detail Lookup Java Agent
 * 
 * Domino Java agent for order/invoice detail lookup via Databricks REST API.
 * 
 * This agent:
 * 1. Reads POST body from Domino Request_Content CGI variable
 * 2. Parses and validates the invoice number
 * 3. Reads Databricks configuration securely from Domino environment document
 * 4. Calls Databricks SQL Statement Execution API (/api/2.0/sql/statements)
 * 5. Checks query state and extracts results using quote-aware JSON parsing
 * 6. Returns delimited string: custno~*~custname~*~orderdate~*~shipdate
 * 
 * Configuration (stored in Domino environment document):
 *   - DATABRICKS_HOST: Workspace host (e.g., "dbc-12345.cloud.databricks.com")
 *   - DATABRICKS_TOKEN: Personal Access Token or OAuth token
 *   - WAREHOUSE_ID: SQL warehouse ID for query execution
 * 
 * Security:
 *   - PAT is NEVER hardcoded — always retrieved from session environment
 *   - Uses parameterized queries to prevent SQL injection
 *   - Validates all input before making API calls
 *   - Explicitly closes all streams in finally block
 * 
 * Error Handling:
 *   All errors returned as delimited string: ERROR~*~<message>
 *   - Invalid invoice: ERROR~*~Invalid or missing invoice number
 *   - Missing config: ERROR~*~Databricks configuration missing — check environment document
 *   - Connection timeout: ERROR~*~Connection to Databricks timed out
 *   - API error: ERROR~*~Databricks API error: <code> - <message>
 *   - Query not ready: ERROR~*~Databricks query state: <state>
 *   - No results: NOTFOUND~*~No invoice found for <number>
 *   - Parse error: ERROR~*~Failed to parse Databricks response
 * 
 * Compile:
 *   javac -cp lotus.jar:notes.jar OEDetailLookup.java
 * 
 * Deploy:
 *   1. Import into Domino database via Designer
 *   2. Configure DATABRICKS_* environment document fields
 *   3. Call from Domino form via Ajax: /<db.nsf>/OEDetailLookup?OpenAgent
 */

import lotus.domino.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.io.*;

public class OEDetailLookup extends AgentBase {

  // ========== CLASS CONSTANTS ==========
  private static final String DELIMITER = "~*~";
  private static final int MAX_INVOICE_LENGTH = 50;
  private static final int CONNECT_TIMEOUT_MS = 10000;  // 10 seconds
  private static final int READ_TIMEOUT_MS = 35000;     // 35 seconds

  // ========== NOTESMAIN ENTRY POINT ==========
  /**
   * Agent execution entry point called by Domino
   */
  public void NotesMain() {
    PrintWriter writer = null;
    Session session = null;
    AgentContext agentContext = null;
    HttpURLConnection connection = null;
    InputStream inputStream = null;
    InputStream errorStream = null;

    try {
      // ========== INITIALIZE DOMINO CONTEXT ==========
      session = getSession();
      agentContext = session.getAgentContext();
      writer = getAgentOutput();

      // ========== WRITE HTTP RESPONSE HEADER ==========
      // CRITICAL: Domino requires Content-Type header before response body
      writer.println("Content-Type: text/plain");
      writer.println();  // Blank line separates header from body
      writer.flush();

      // ========== READ POST BODY FROM REQUEST_CONTENT ==========
      Document docContext = agentContext.getDocumentContext();
      String requestContent = docContext.getItemValueString("Request_Content");

      if (requestContent == null || requestContent.isEmpty()) {
        writer.println("ERROR" + DELIMITER + "Missing request body");
        return;
      }

      // ========== URL-DECODE REQUEST BODY ==========
      String decoded = URLDecoder.decode(requestContent, "UTF-8");

      // ========== PARSE INVOICE PARAMETER ==========
      String invoiceNo = extractParameterValue(decoded, "invoice");

      // ========== VALIDATE INVOICE NUMBER ==========
      if (invoiceNo == null || invoiceNo.trim().isEmpty()) {
        writer.println("ERROR" + DELIMITER + "Invalid or missing invoice number");
        return;
      }

      invoiceNo = invoiceNo.trim();
      if (invoiceNo.length() > MAX_INVOICE_LENGTH) {
        writer.println("ERROR" + DELIMITER + "Invalid or missing invoice number");
        return;
      }

      // ========== READ DATABRICKS CONFIGURATION FROM ENVIRONMENT ==========
      String databricksHost = session.getEnvironmentString("DATABRICKS_HOST", true);
      String databricksToken = session.getEnvironmentString("DATABRICKS_TOKEN", true);
      String warehouseId = session.getEnvironmentString("WAREHOUSE_ID", true);

      if (databricksHost == null || databricksHost.isEmpty() ||
          databricksToken == null || databricksToken.isEmpty() ||
          warehouseId == null || warehouseId.isEmpty()) {
        writer.println("ERROR" + DELIMITER + "Databricks configuration missing — check environment document");
        return;
      }

      // ========== BUILD DATABRICKS SQL STATEMENT API REQUEST ==========
      String sqlStatement = "SELECT custno, custname, orderdate, shipdate FROM prd_fold.facts.oe_detail WHERE invoice_no = :invoice_no";

      String requestBody = buildJsonRequest(warehouseId, sqlStatement, invoiceNo);

      // ========== OPEN HTTPS CONNECTION TO DATABRICKS API ==========
      String apiUrl = "https://" + databricksHost + "/api/2.0/sql/statements";
      URL url = new URL(apiUrl);
      connection = (HttpURLConnection) url.openConnection();

      // ========== CONFIGURE CONNECTION ==========
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(READ_TIMEOUT_MS);
      connection.setRequestProperty("Authorization", "Bearer " + databricksToken);
      connection.setRequestProperty("Content-Type", "application/json");

      // ========== WRITE REQUEST BODY ==========
      OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
      osw.write(requestBody);
      osw.flush();
      osw.close();

      // ========== CHECK HTTP RESPONSE CODE ==========
      int httpCode = connection.getResponseCode();
      if (httpCode != 200) {
        // Read error response
        String errorMsg = readErrorResponse(connection);
        writer.println("ERROR" + DELIMITER + "Databricks API error: " + httpCode + " - " + errorMsg);
        return;
      }

      // ========== READ RESPONSE BODY ==========
      inputStream = connection.getInputStream();
      String responseBody = readStreamAsString(inputStream);

      if (responseBody == null || responseBody.isEmpty()) {
        writer.println("ERROR" + DELIMITER + "Empty response from Databricks");
        return;
      }

      // ========== CHECK QUERY STATE ==========
      String state = extractFieldValue(responseBody, "\"state\"");
      if (state == null || !state.equals("SUCCEEDED")) {
        writer.println("ERROR" + DELIMITER + "Databricks query state: " + (state != null ? state : "UNKNOWN"));
        return;
      }

      // ========== EXTRACT DATA ARRAY ==========
      // Look for "data_array":[[...]]
      String dataArray = extractDataArray(responseBody);

      if (dataArray == null || dataArray.equals("[]")) {
        writer.println("NOTFOUND" + DELIMITER + "No invoice found for " + invoiceNo);
        return;
      }

      // ========== PARSE DATA ARRAY WITH QUOTE-AWARE EXTRACTION ==========
      String[] values = parseDataArrayValues(dataArray);

      if (values == null || values.length < 4) {
        writer.println("ERROR" + DELIMITER + "Failed to parse Databricks response");
        return;
      }

      // ========== FORMAT RESPONSE ==========
      // Response: custno~*~custname~*~orderdate~*~shipdate
      String result = values[0] + DELIMITER + values[1] + DELIMITER + values[2] + DELIMITER + values[3];
      writer.println(result);

    } catch (SocketTimeoutException e) {
      if (writer != null) {
        writer.println("ERROR" + DELIMITER + "Connection to Databricks timed out");
      }
    } catch (Exception e) {
      if (writer != null) {
        writer.println("ERROR" + DELIMITER + "Failed to parse Databricks response");
      }
    } finally {
      // ========== CLEANUP: CLOSE ALL STREAMS ==========
      try {
        if (inputStream != null) inputStream.close();
      } catch (IOException e) {
        // Ignore
      }
      try {
        if (errorStream != null) errorStream.close();
      } catch (IOException e) {
        // Ignore
      }
      if (connection != null) {
        connection.disconnect();
      }
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }

  // ========== HELPER METHODS ==========

  /**
   * Extracts a parameter value from URL-encoded string
   * Handles multiple parameters separated by &
   * 
   * @param queryString URL-encoded string (e.g., "invoice=INV-123&other=value")
   * @param paramName Parameter name to extract (e.g., "invoice")
   * @return Parameter value or null if not found
   */
  private String extractParameterValue(String queryString, String paramName) {
    String[] params = queryString.split("&");
    for (String param : params) {
      String[] pair = param.split("=", 2);
      if (pair.length == 2 && pair[0].equals(paramName)) {
        return pair[1];
      }
    }
    return null;
  }

  /**
   * Extracts a JSON field value from response body
   * Finds "fieldname": and reads until closing quote
   * 
   * @param json JSON response body
   * @param fieldName Field name including quotes (e.g., "\"state\"")
   * @return Field value (without quotes) or null if not found
   */
  private String extractFieldValue(String json, String fieldName) {
    int index = json.indexOf(fieldName);
    if (index == -1) return null;

    // Find the colon after field name
    int colonIndex = json.indexOf(":", index);
    if (colonIndex == -1) return null;

    // Skip colon and any whitespace
    int startIndex = colonIndex + 1;
    while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
      startIndex++;
    }

    // Handle quoted string values
    if (startIndex < json.length() && json.charAt(startIndex) == '"') {
      startIndex++;  // Skip opening quote
      int endIndex = json.indexOf("\"", startIndex);
      if (endIndex == -1) return null;
      return json.substring(startIndex, endIndex);
    }

    // Handle unquoted values (null, true, false, numbers)
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

  /**
   * Extracts the data_array from Databricks response
   * Finds "data_array":[ and returns the array content
   * 
   * @param json JSON response body
   * @return Data array string (e.g., "[[\"val1\",\"val2\",...]]") or null if not found
   */
  private String extractDataArray(String json) {
    String marker = "\"data_array\":";
    int index = json.indexOf(marker);
    if (index == -1) return null;

    int startIndex = index + marker.length();

    // Skip whitespace
    while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
      startIndex++;
    }

    if (startIndex >= json.length() || json.charAt(startIndex) != '[') {
      return null;
    }

    // Find matching closing bracket
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

  /**
   * Parses the first row from data_array using quote-aware extraction
   * Handles commas within quoted strings (e.g., "Smith, Jones & Co.")
   * Handles null literals in array
   * 
   * @param dataArray Data array string: [[...]]
   * @return Array of parsed field values or null if error
   */
  private String[] parseDataArrayValues(String dataArray) {
    try {
      // Remove outer brackets: [[...]] -> [...]
      if (!dataArray.startsWith("[[")) return null;

      int closeIdx = dataArray.lastIndexOf("]]");
      if (closeIdx == -1) return null;

      String firstRow = dataArray.substring(2, closeIdx);
      if (firstRow.isEmpty()) return null;

      // Parse values with quote awareness
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
          // End of current value
          String value = currentValue.toString().trim();
          value = sanitizeValue(value);
          values.add(value);
          currentValue = new StringBuilder();
          continue;
        }

        currentValue.append(c);
      }

      // Add last value
      String value = currentValue.toString().trim();
      value = sanitizeValue(value);
      values.add(value);

      return values.toArray(new String[0]);

    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Sanitizes a parsed JSON value
   * Removes surrounding quotes, handles null literal
   * 
   * @param value Raw value from JSON
   * @return Sanitized value (empty string for null literal)
   */
  private String sanitizeValue(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }

    // Handle null literal (unquoted)
    if (value.equals("null")) {
      return "";
    }

    // Remove surrounding quotes
    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
      return value.substring(1, value.length() - 1);
    }

    return value;
  }

  /**
   * Builds the Databricks SQL Statement API request body as JSON
   * 
   * @param warehouseId SQL warehouse ID
   * @param statement SQL statement with :invoice_no placeholder
   * @param invoiceNo Invoice number value
   * @return JSON request body string
   */
  private String buildJsonRequest(String warehouseId, String statement, String invoiceNo) {
    // Build JSON with parameterized query
    // Escape invoice number for JSON
    String escapedInvoice = invoiceNo.replace("\\", "\\\\").replace("\"", "\\\"");

    return "{" +
      "\"warehouse_id\": \"" + warehouseId + "\"," +
      "\"statement\": \"" + statement + "\"," +
      "\"parameters\": [" +
        "{\"name\": \"invoice_no\", \"value\": \"" + escapedInvoice + "\", \"type\": \"STRING\"}" +
      "]," +
      "\"wait_timeout\": \"30s\"" +
    "}";
  }

  /**
   * Reads an HTTP response stream as String
   * 
   * @param inputStream Input stream from HTTP connection
   * @return Response body as String
   * @throws IOException on read error
   */
  private String readStreamAsString(InputStream inputStream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append("\n");
    }
    reader.close();
    return sb.toString();
  }

  /**
   * Reads error response from HTTP connection
   * Extracts error_code and message fields if available
   * 
   * @param connection HTTP connection with error response
   * @return Error message string
   */
  private String readErrorResponse(HttpURLConnection connection) {
    try {
      InputStream errorStream = connection.getErrorStream();
      if (errorStream != null) {
        String errorBody = readStreamAsString(errorStream);
        errorStream.close();

        // Try to extract error_code and message
        String code = extractFieldValue(errorBody, "\"error_code\"");
        String message = extractFieldValue(errorBody, "\"message\"");

        if (code != null && message != null) {
          return code + " - " + message;
        } else if (message != null) {
          return message;
        }

        return "Unknown error";
      }
    } catch (Exception e) {
      // Ignore, return generic error
    }

    return "Unknown error";
  }

  // Inner class for timeout exception handling
  class SocketTimeoutException extends java.net.SocketTimeoutException {
    public SocketTimeoutException(String msg) {
      super(msg);
    }
  }
}
