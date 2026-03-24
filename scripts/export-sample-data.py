#!/usr/bin/env python3
"""
Export prd_fold.facts.oe_detail as Parquet for sharing

Usage (from Databricks Notebook):
  %run ./export-sample-data.py
  
This creates a Parquet export of the sample invoice data
for easy import into other Databricks workspaces.

Output: oe_detail_sample.parquet (in current directory)
"""

from pyspark.sql.types import StructType, StructField, StringType, DateType, DecimalType
from pyspark.sql.functions import to_date, col
from datetime import date

# Define schema matching our table
schema = StructType([
    StructField("invoice_no", StringType(), False),
    StructField("custno", StringType(), False),
    StructField("custname", StringType(), False),
    StructField("orderdate", StringType(), False),  # Will convert to DATE
    StructField("shipdate", StringType(), True),     # Will convert to DATE (nullable)
    StructField("ponumber", StringType(), True),
    StructField("shipvia", StringType(), True),
    StructField("totalamt", DecimalType(38, 2), True),
    StructField("orderstatus", StringType(), True),
])

# Sample data (matches create_oe_detail.sql exactly)
data = [
    ("INV-2024-001", "CUST001", "Acme Corporation", "2024-01-05", "2024-01-10", "PO-2024-001", "FedEx", 15250.00, "SHIPPED"),
    ("INV-2024-002", "CUST002", "Global Ventures Inc.", "2024-01-08", "2024-01-15", "PO-2024-002", "UPS", 8900.50, "SHIPPED"),
    ("INV-2024-003", "CUST003", "Smith, Jones & Co.", "2024-01-15", "2024-01-22", "PO-2024-003", "DHL", 22500.00, "SHIPPED"),
    ("INV-2024-004", "CUST001", "Acme Corporation", "2024-01-25", None, "PO-2024-004", "FedEx", 11200.75, "PENDING"),
    ("INV-2024-005", "CUST004", "TechPro Solutions LLC", "2024-02-02", "2024-02-09", "PO-2024-005", "UPS", 19500.00, "SHIPPED"),
    ("INV-2024-006", "CUST005", "Industrial Dynamics", "2024-02-05", "2024-02-12", "PO-2024-006", "FedEx", 33400.25, "SHIPPED"),
    ("INV-2024-007", "CUST002", "Global Ventures Inc.", "2024-02-14", "2024-02-21", "PO-2024-007", "DHL", 7650.00, "SHIPPED"),
    ("INV-2024-008", "CUST006", "Premium Finance Partners", "2024-02-22", "2024-02-28", "PO-2024-008", "UPS", 41200.50, "SHIPPED"),
    ("INV-2024-009", "CUST003", "Smith, Jones & Co.", "2024-03-01", None, "PO-2024-009", "FedEx", 18900.00, "PROCESSING"),
    ("INV-2024-010", "CUST007", "Enterprise Systems Group", "2024-03-10", "2024-03-17", "PO-2024-010", "DHL", 25600.75, "SHIPPED"),
    ("INV-2024-011", "CUST004", "TechPro Solutions LLC", "2024-03-20", "2024-03-27", "PO-2024-011", "UPS", 12300.00, "SHIPPED"),
    ("INV-2024-012", "CUST001", "Acme Corporation", "2024-04-01", "2024-04-08", "PO-2024-012", "FedEx", 19850.50, "SHIPPED"),
    ("INV-2024-013", "CUST008", "NextGen Logistics", "2024-04-12", "2024-04-19", "PO-2024-013", "DHL", 29500.00, "SHIPPED"),
    ("INV-2024-014", "CUST005", "Industrial Dynamics", "2024-04-25", "2024-05-02", "PO-2024-014", "UPS", 16700.25, "SHIPPED"),
    ("INV-2024-015", "CUST002", "Global Ventures Inc.", "2024-05-03", "2024-05-10", "PO-2024-015", "FedEx", 35200.00, "SHIPPED"),
    ("INV-2024-016", "CUST006", "Premium Finance Partners", "2024-05-15", "2024-05-22", "PO-2024-016", "DHL", 22100.75, "SHIPPED"),
    ("INV-2024-017", "CUST007", "Enterprise Systems Group", "2024-05-28", "2024-06-04", "PO-2024-017", "UPS", 18500.00, "SHIPPED"),
    ("INV-2024-018", "CUST003", "Smith, Jones & Co.", "2024-06-01", "2024-06-08", "PO-2024-018", "FedEx", 27900.50, "SHIPPED"),
    ("INV-2024-019", "CUST004", "TechPro Solutions LLC", "2024-06-12", "2024-06-19", "PO-2024-019", "DHL", 14200.00, "SHIPPED"),
    ("INV-2024-020", "CUST008", "NextGen Logistics", "2024-06-25", None, "PO-2024-020", "UPS", 31500.75, "PENDING"),
]

# Create DataFrame
df = spark.createDataFrame(data, schema=schema)

# Convert string dates to proper DATE type
df = df.withColumn("orderdate", to_date(col("orderdate"), "yyyy-MM-dd")) \
        .withColumn("shipdate", to_date(col("shipdate"), "yyyy-MM-dd"))

# Write to Delta table
df.write.mode("overwrite").option("mergeSchema", "true").saveAsTable("prd_fold.facts.oe_detail")

print(f"✓ Created table prd_fold.facts.oe_detail with {df.count()} rows")
print(f"✓ Columns: {', '.join([f.name for f in df.schema.fields])}")
print(f"✓ Sample data includes:")
print(f"  - 20 invoices (INV-2024-001 through INV-2024-020)")
print(f"  - 8 unique customers")
print(f"  - 3 NULL shipdates (unshipped orders)")
print(f"  - Comma in customer name: 'Smith, Jones & Co.' (tests JSON parsing)")
