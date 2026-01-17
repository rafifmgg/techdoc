# Flowchart Sizing Standard

## Overview

This document defines the standard sizing rules for technical flowcharts to ensure consistency and proportional layouts across all diagrams.

---

## 1. Page Dimensions Formula

```
Page Width  = Swimlane Width + 100px (margin)
Page Height = Total Swimlane Heights + Title Height (50px) + Bottom Margin (50px)
```

---

## 2. Swimlane Width Calculation

```
Swimlane Width = Content Max X + Element Width + Padding (200-300px)
```

### Example:
- If rightmost element is at x=1500 with width=100
- Content Max = 1500 + 100 = 1600
- Swimlane Width = 1600 + 200 = 1800

---

## 3. Success/End Element Positioning

**CRITICAL:** Success and End elements should be positioned within 200-300px after the last process step, NOT at the far end of the swimlane.

### Wrong:
```
Main flow ends at x=2000
Success at x=4500  ❌ (gap 2500px)
End at x=4700      ❌
```

### Correct:
```
Main flow ends at x=2000
Success at x=2200  ✓ (gap 200px)
End at x=2400      ✓
```

---

## 4. Standard Swimlane Heights

| Number of Swimlanes | Total Page Height |
|---------------------|-------------------|
| 1 swimlane          | 450-550px         |
| 2 swimlanes         | 700-900px         |
| 3 swimlanes         | 900-1100px        |
| 4 swimlanes         | 1100-1400px       |

### Individual Swimlane Heights:
- Simple flow (few elements): 250-350px
- Medium flow: 350-450px
- Complex flow (many decisions): 450-600px

---

## 5. Element Spacing Standards

| Spacing Type | Distance |
|--------------|----------|
| Between elements (horizontal) | 50-100px |
| Between swimlanes (vertical) | 0px (touching) |
| From swimlane edge to first element | 30-50px |
| From last element to swimlane edge | 100-200px |

---

## 6. Diagram Type Guidelines

### High-Level Overview (e.g., 3.2)
- Wider page for multi-step flows
- Single swimlane typically 300-400px height
- API boxes positioned below main flow

### Detailed API Flow (e.g., 3.6, 3.7)
- Multiple swimlanes (Portal, API, Intranet DB, Internet DB)
- Height based on complexity
- Database boxes aligned with corresponding API steps

### Cron/Batch Jobs (e.g., 3.3)
- Usually single swimlane
- Compact layout
- Schedule info box at top-left

---

## 7. Before Creating New Diagram

1. **Estimate content width:**
   - Count process steps: ~150px each
   - Add decision points: ~150px each
   - Add spacing: ~50px between elements

2. **Calculate dimensions:**
   ```
   Estimated Width = (Steps × 150) + (Decisions × 150) + (Gaps × 50) + 300
   ```

3. **Set initial page/swimlane:**
   - Page Width = Estimated Width + 100
   - Swimlane Width = Estimated Width

4. **Adjust after completion:**
   - Find actual max X of elements
   - Resize to content + 200px padding

---

## 8. Quick Reference Table

| Diagram Complexity | Est. Elements | Page Width | Page Height |
|--------------------|---------------|------------|-------------|
| Simple (5-8 steps) | 5-8 | 1400-1800 | 500-700 |
| Medium (10-15 steps) | 10-15 | 2000-2800 | 800-1100 |
| Complex (20+ steps) | 20+ | 3000-4500 | 1100-1600 |

---

## 9. Checklist Before Finalizing

- [ ] Content max X + 200 ≈ Swimlane width
- [ ] Page width = Swimlane width + 100
- [ ] Success/End within 300px of last process
- [ ] No excessive whitespace (>500px unused)
- [ ] Heights proportional to content

---

*Document Version: 1.0*
*Created: 2026-01-07*
