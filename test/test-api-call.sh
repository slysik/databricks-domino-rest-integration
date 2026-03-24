#!/bin/bash

################################################################################
# Test Databricks SQL Statement API
#
# This script validates that your Databricks configuration is correct by
# making a direct API call to the SQL Statement Execution endpoint.
#
# Prerequisites:
#   - DATABRICKS_HOST (workspace hostname)
#   - DATABRICKS_TOKEN (PAT or OAuth token)
#   - WAREHOUSE_ID (SQL warehouse ID)
#
# Usage:
#   ./test-api-call.sh <invoice_number>
#
# Example:
#   ./test-api-call.sh "INV-2024-001"
#
# Output:
#   - Pretty-printed JSON response from Databricks
#   - Query state and results (if successful)
#
# Common error codes:
#   - 401: Unauthorized (invalid token)
#   - 403: Forbidden (token lacks permissions)
#   - 404: Not Found (invalid warehouse ID or endpoint)
#   - 500: Internal Server Error (warehouse issue or query syntax error)
#
################################################################################

set -euo pipefail

# ============================================================================
# PREREQUISITES CHECK
# ============================================================================

if [[ -z "${DATABRICKS_HOST:-}" ]]; then
  echo "ERROR: DATABRICKS_HOST environment variable not set"
  echo ""
  echo "Set it with:"
  echo "  export DATABRICKS_HOST='dbc-12345.cloud.databricks.com'"
  exit 1
fi

if [[ -z "${DATABRICKS_TOKEN:-}" ]]; then
  echo "ERROR: DATABRICKS_TOKEN environment variable not set"
  echo ""
  echo "Set it with:"
  echo "  export DATABRICKS_TOKEN='dapi...'"
  exit 1
fi

if [[ -z "${WAREHOUSE_ID:-}" ]]; then
  echo "ERROR: WAREHOUSE_ID environment variable not set"
  echo ""
  echo "Set it with:"
  echo "  export WAREHOUSE_ID='abc123def456'"
  exit 1
fi

# ============================================================================
# USAGE MESSAGE
# ============================================================================

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <invoice_number>"
  echo ""
  echo "Example:"
  echo "  $0 'INV-2024-001'"
  echo ""
  echo "Environment variables required:"
  echo "  DATABRICKS_HOST (e.g., dbc-12345.cloud.databricks.com)"
  echo "  DATABRICKS_TOKEN (e.g., dapi...)"
  echo "  WAREHOUSE_ID (e.g., abc123def456)"
  exit 1
fi

INVOICE_NO="$1"

# ============================================================================
# VALIDATE INVOICE INPUT
# ============================================================================

if [[ -z "$INVOICE_NO" ]]; then
  echo "ERROR: Invoice number cannot be empty"
  exit 1
fi

echo "Testing Databricks SQL Statement API..."
echo "=========================================="
echo "Host:         $DATABRICKS_HOST"
echo "Warehouse ID: $WAREHOUSE_ID"
echo "Invoice No:   $INVOICE_NO"
echo ""

# ============================================================================
# BUILD REQUEST JSON
# ============================================================================

# Escape special characters in invoice number for JSON
ESCAPED_INVOICE=$(echo "$INVOICE_NO" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')

REQUEST_JSON="{
  \"warehouse_id\": \"$WAREHOUSE_ID\",
  \"statement\": \"SELECT custno, custname, orderdate, shipdate FROM prd_gold.facts.oe_detail WHERE invoice_no = :invoice_no\",
  \"parameters\": [
    {\"name\": \"invoice_no\", \"value\": \"$ESCAPED_INVOICE\", \"type\": \"STRING\"}
  ],
  \"wait_timeout\": \"30s\"
}"

# ============================================================================
# MAKE API CALL
# ============================================================================

echo "Calling Databricks SQL Statement API..."
echo ""

RESPONSE=$(curl -s -X POST \
  "https://${DATABRICKS_HOST}/api/2.0/sql/statements" \
  -H "Authorization: Bearer ${DATABRICKS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# ============================================================================
# PRETTY-PRINT RESPONSE
# ============================================================================

echo "Response:"
echo "=========================================="

# Try jq for pretty printing, fallback to Python
if command -v jq &> /dev/null; then
  echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
elif command -v python3 &> /dev/null; then
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
elif command -v python &> /dev/null; then
  echo "$RESPONSE" | python -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "$RESPONSE"
fi

echo ""
echo "=========================================="

# ============================================================================
# PARSE RESPONSE AND SHOW RESULTS
# ============================================================================

# Check for error_code in response
if echo "$RESPONSE" | grep -q "error_code"; then
  ERROR_CODE=$(echo "$RESPONSE" | grep -o '"error_code":"[^"]*"' | cut -d'"' -f4 || echo "UNKNOWN")
  MESSAGE=$(echo "$RESPONSE" | grep -o '"message":"[^"]*"' | cut -d'"' -f4 || echo "Unknown error")
  echo "ERROR: $ERROR_CODE - $MESSAGE"
  exit 1
fi

# Extract state from response
STATE=$(echo "$RESPONSE" | grep -o '"state":"[^"]*"' | cut -d'"' -f4 || echo "UNKNOWN")

if [[ "$STATE" == "SUCCEEDED" ]]; then
  echo "✓ Query succeeded"
  echo ""
  
  # Try to extract and show results
  if echo "$RESPONSE" | grep -q "data_array"; then
    echo "Results:"
    echo "$RESPONSE" | grep -o '"data_array":\[\[[^]]*\]\]' || echo "  (results available)"
    echo ""
  fi
  
  echo "Status: SUCCESS"
  exit 0
  
elif [[ "$STATE" == "PENDING" ]] || [[ "$STATE" == "RUNNING" ]]; then
  echo "⏱ Query is still running (state: $STATE)"
  echo ""
  echo "The Databricks warehouse may be starting up."
  echo "Try again in a few seconds, or check warehouse status:"
  echo "  https://${DATABRICKS_HOST}/?o=<org-id>#browse/warehouses"
  exit 0
  
elif [[ "$STATE" == "FAILED" ]]; then
  echo "✗ Query failed"
  # Try to extract error details
  if echo "$RESPONSE" | grep -q "error_message"; then
    ERROR_MSG=$(echo "$RESPONSE" | grep -o '"error_message":"[^"]*"' | cut -d'"' -f4)
    echo "Error: $ERROR_MSG"
  fi
  exit 1
  
else
  echo "? Unknown state: $STATE"
  exit 1
fi
