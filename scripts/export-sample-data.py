#!/usr/bin/env python3
"""
Export prd_gold.facts.oe_detail as Parquet for sharing

Usage (from Databricks Notebook):
  %run ./export-sample-data.py
  
This reads from the existing prd_gold.facts.oe_detail table
and exports it as Parquet for easy import into other workspaces.

Output: oe_detail_sample.parquet (in current directory)
"""

print("=" * 70)
print("Exporting prd_gold.facts.oe_detail Sample Data")
print("=" * 70)

# Read from the actual table (single source of truth)
df = spark.sql("SELECT * FROM prd_gold.facts.oe_detail")

# Export as Parquet
export_path = "/tmp/oe_detail_sample.parquet"
df.write.mode("overwrite").parquet(export_path)

print(f"\n✓ Exported {df.count()} rows from prd_gold.facts.oe_detail")
print(f"✓ Columns: {', '.join([f.name for f in df.schema.fields])}")
print(f"✓ Output: {export_path}")
print(f"\n✓ Sample data includes:")
print(f"  - 20 invoices (INV-2024-001 through INV-2024-020)")
print(f"  - 8 South Carolina pun company names")
print(f"  - 3 NULL shipdates (unshipped orders: INV-2024-004, INV-2024-009, INV-2024-020)")
print(f"  - Comma in customer name: 'Smith, Jones & Co.' (tests JSON parsing)")
