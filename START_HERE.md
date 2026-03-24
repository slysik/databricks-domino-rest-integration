# Welcome! Start Here 👋

This is a **simple, production-ready** Databricks-Domino integration. Follow the path below.

---

## 🚀 Your Path to Deployment

### **Step 1: Understand What This Does**

Open [README.md](./README.md) and read the "What This Does" section.

Takes **2 minutes**.

---

### **Step 2: Implement (6 Steps)**

Open [BILL_QUICK_START.md](./BILL_QUICK_START.md).

Follow exactly. Takes **~30 minutes**.

---

### **Step 3: Test**

Use your own invoice numbers. Expected: Fields populate in 2-3 seconds.

If something fails: Check [PRODUCTION_NOTES.md](./PRODUCTION_NOTES.md) troubleshooting section.

---

### **Step 4: Get More Tables (Later)**

When you get access to other tables, open [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md).

Add fields using the simple 3-step pattern. Takes **~5 minutes per batch**.

---

## 📚 Full Documentation

| Document | Purpose | Read When |
|----------|---------|-----------|
| [README.md](./README.md) | Overview + architecture | Getting oriented |
| [BILL_QUICK_START.md](./BILL_QUICK_START.md) | Your 6-step implementation | Ready to code |
| [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md) | Adding columns pattern | You have table access |
| [PRODUCTION_NOTES.md](./PRODUCTION_NOTES.md) | Deployment checklist | Before going live |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Design decisions | Curious about tradeoffs |

---

## 🎯 Quick Answer Guide

**Q: How do I implement?**  
A: Read [BILL_QUICK_START.md](./BILL_QUICK_START.md)

**Q: How do I add more fields?**  
A: Read [EXTEND_WITH_MORE_FIELDS.md](./EXTEND_WITH_MORE_FIELDS.md)

**Q: Something failed, what now?**  
A: Check [PRODUCTION_NOTES.md](./PRODUCTION_NOTES.md) troubleshooting

**Q: Why did you design it this way?**  
A: Read [ARCHITECTURE.md](./ARCHITECTURE.md)

---

## 📁 Code Files

```
java/OEDetailLookup.java      ← Your Java agent
js/oe-lookup.js               ← Your form JavaScript
```

That's it. Just 2 files to integrate.

---

## ⚠️ Critical: DatabasePath Hidden Field

When you add fields to your form, **don't forget this**:

```html
<input id="DatabasePath" type="hidden" value="your-database.nsf" />
```

Replace `your-database.nsf` with your actual database filename.

Without it: Form will error on Lookup click.

---

## 🚀 Ready?

**→ Open [BILL_QUICK_START.md](./BILL_QUICK_START.md) and follow the 6 steps.**

Takes ~30 minutes. You'll be live! ✅

---

*For technical details, see [ARCHITECTURE.md](./ARCHITECTURE.md)*
