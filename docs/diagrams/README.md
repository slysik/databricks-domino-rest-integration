# Architecture Diagrams

## Bill's Domino-Databricks Integration

**File**: `bill_domino_databricks_architecture.png`

### Overview

Hand-drawn sketchnote architecture diagram illustrating the complete flow of the Domino-to-Databricks invoice lookup integration.

### Components

```
┌─────────────────┬────────────────┬──────────────────────┐
│  Domino Form    │  Java Agent    │  Databricks Data     │
│  Layer          │  (Bridge)      │  Layer (Gold)        │
├─────────────────┼────────────────┼──────────────────────┤
│ • Invoice # (✏️)│ 1. Read POST   │ 🗄️  prd_gold.facts  │
│ • Customer      │ 2. Parse ID    │ • oe_detail table    │
│ • Order Date    │ 3. Call API    │ • 20 sample invoices │
│ • Ship Date     │ 4. Quote-Parse │ • invoice_no = PK    │
│ • Lookup Button │ 5. Return JSON │ • custno, custname   │
└─────────────────┴────────────────┴──────────────────────┘
        ↓                ↓                   ↑
    Ajax POST    /api/2.0/sql       JSON Response
```

### Key Features

✅ **Input**: invoice_no via Ajax POST from Domino form  
✅ **Processing**: Java agent executes REST API call to Databricks  
✅ **Query**: Parameterized SQL with quote-aware JSON parsing  
✅ **Output**: JSON response with custno, custname, orderdate, shipdate  
✅ **Result**: JavaScript populates form fields in real-time  

### Design Details

- **Style**: Hand-drawn sketchnote with pastel watercolor fills
- **Layout**: Three-column vertical flow (left → middle → right)
- **Colors**: Light blue (Domino), light green (integration), light orange (database)
- **Icons**: Hand-drawn pictograms for each concept
- **Typography**: Bold marker fonts with natural imperfections
- **Dimensions**: 1920x1080 (16:9 landscape, presentation-ready)

### Perfect For

- **Interview presentations** to banking CTOs
- **Technical deep-dive** with Bill
- **Architecture reviews** with stakeholders
- **Documentation** in the GitHub repo

### Generated With

- **Tool**: Google Gemini 3.1 Flash Image Generation (nano-banana)
- **Prompt**: Hand-drawn sketchnote specifications
- **Date**: 2026-03-24
- **Quality**: Production-ready for presentations

---

**Ready to show Bill this beautiful architecture!** 🎨
