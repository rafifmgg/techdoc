# Guideline: Creating Flowcharts in Draw.io

This guideline describes how to create consistent flowcharts using Draw.io for OCMS technical documentation. Based on approved example: `OCMS 18 staff portal.drawio`.

---

## 1. Getting Started

### Opening Draw.io

1. **Online:** Go to [app.diagrams.net](https://app.diagrams.net)
2. **Desktop:** Download from [draw.io](https://www.drawio.com/blog/diagrams-offline)
3. **VS Code:** Install "Draw.io Integration" extension

### File Naming Convention

```
OCMS [NUMBER] [description].drawio
```

Example: `OCMS 18 staff portal.drawio`

---

## 2. Document Structure

### Page/Diagram Setup

| Setting | Value |
| --- | --- |
| Page Name | Meaningful names based on content (e.g., PS_High_Level, OCMS_Staff_Portal_Manual_PS) |
| Canvas Width | 6000-7000 px (wide horizontal flow) |
| Canvas Height | 1700-2000 px |
| Grid | Enabled, size 10 |

**Page Naming Convention:**
- Use descriptive names that reflect the diagram content
- Use underscores to separate words
- Examples: `PS_High_Level`, `OCMS_Staff_Portal_Manual_PS`, `Auto_PS_Triggers`

**To add a new page:**
1. Right-click on page tab at bottom
2. Select "Insert Page"
3. Name with descriptive name based on content

---

## 3. Swimlane Structure

### Standard Swimlanes

| Swimlane | Fill Color | Stroke Color | Description |
| --- | --- | --- | --- |
| Staff Portal | #dae8fc (light blue) | #6c8ebf | Frontend swimlane |
| OCMS Admin API Intranet | #d5e8d4 (light green) | #82b366 | Backend API swimlane |

### Swimlane Style Code

**Staff Portal (Frontend):**
```
swimlane;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;
```

**OCMS Admin API Intranet (Backend):**
```
swimlane;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;
```

### Swimlane Sizing

| Swimlane | Recommended Size |
| --- | --- |
| Staff Portal | width="6710" height="630" |
| OCMS Admin API | width="3730" height="1020" |

**Note:** Swimlanes should be wide enough to contain the entire horizontal flow.

---

## 4. Standard Shapes

### Shape Reference

| Shape | Style | Size | Use For |
| --- | --- | --- | --- |
| Terminator | `strokeWidth=2;html=1;shape=mxgraph.flowchart.terminator;whiteSpace=wrap;` | 100x60 | Start / End |
| Process | `rounded=1;whiteSpace=wrap;html=1;absoluteArcSize=1;arcSize=14;strokeWidth=2;` | 100x100 or 120x60 | Action steps |
| Decision | `strokeWidth=2;html=1;shape=mxgraph.flowchart.decision;whiteSpace=wrap;` | 100x100 or 140x140 | Yes/No decisions |

**IMPORTANT:** Shapes use **default colors** (white fill, black stroke). Do NOT add fill colors to process shapes.

### Shape Examples

**Start/End Terminator:**
```xml
<mxCell id="3" value="Start" style="strokeWidth=2;html=1;shape=mxgraph.flowchart.terminator;whiteSpace=wrap;" vertex="1" parent="1">
    <mxGeometry x="30" y="130" width="100" height="60" as="geometry"/>
</mxCell>
```

**Process Box:**
```xml
<mxCell id="5" value="Staff portal search notice" style="rounded=1;whiteSpace=wrap;html=1;absoluteArcSize=1;arcSize=14;strokeWidth=2;" vertex="1" parent="1">
    <mxGeometry x="460" y="110" width="100" height="100" as="geometry"/>
</mxCell>
```

**Decision Diamond:**
```xml
<mxCell id="10" value="have permission ?" style="strokeWidth=2;html=1;shape=mxgraph.flowchart.decision;whiteSpace=wrap;" vertex="1" parent="1">
    <mxGeometry x="820" y="110" width="100" height="100" as="geometry"/>
</mxCell>
```

---

## 5. API/Payload Boxes

### API Box Style

API calls and payloads should be displayed in **yellow boxes** with dashed line connections.

**Style:**
```
text;html=1;whiteSpace=wrap;strokeColor=#d6b656;fillColor=#fff2cc;align=left;verticalAlign=middle;rounded=0;
```

### API Box Example

```xml
<mxCell id="26" value="POST /validoffencenoticelist&lt;div&gt;{&lt;/div&gt;&lt;div&gt;  &quot;$skip&quot;: 0,&lt;/div&gt;&lt;div&gt;  &quot;$limit&quot;: 10&lt;/div&gt;&lt;div&gt;}&lt;/div&gt;" style="text;html=1;whiteSpace=wrap;strokeColor=#d6b656;fillColor=#fff2cc;align=left;verticalAlign=middle;rounded=0;" vertex="1" parent="1">
    <mxGeometry x="102.5" y="280" width="475" height="157.5" as="geometry"/>
</mxCell>
```

### Connecting API Boxes

Use **dashed lines** to connect process shapes to API boxes:

```xml
<mxCell id="24" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;dashed=1;dashPattern=8 8;" edge="1" parent="1" source="25" target="26">
    <mxGeometry relative="1" as="geometry"/>
</mxCell>
```

---

## 6. Information Notes

### Blue Information Box

For additional information or notes:

**Style:**
```
text;html=1;whiteSpace=wrap;strokeColor=#6c8ebf;fillColor=#dae8fc;align=center;verticalAlign=middle;rounded=0;
```

**Example:**
```xml
<mxCell id="76" value="10 notice numbers per call" style="text;html=1;whiteSpace=wrap;strokeColor=#6c8ebf;fillColor=#dae8fc;align=center;verticalAlign=middle;rounded=0;" vertex="1" parent="72">
    <mxGeometry x="2980" y="470" width="100" height="30" as="geometry"/>
</mxCell>
```

### Reference Text (Italic)

For document references:

**Style:**
```
text;html=1;whiteSpace=wrap;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;rounded=0;fontStyle=2
```

**Example:**
```xml
<mxCell id="11" value="Refer to JWT Authentication Technical Document v1.0" style="text;html=1;whiteSpace=wrap;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;rounded=0;fontStyle=2" vertex="1" parent="1">
    <mxGeometry x="630" y="230" width="120" height="60" as="geometry"/>
</mxCell>
```

---

## 7. Connector Lines

### Standard Connectors

| Type | Style | Use For |
| --- | --- | --- |
| Solid arrow | `edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;` | Normal flow |
| Dashed line | Add `dashed=1;dashPattern=8 8;` | API/Database connections |

### Decision Labels

| Branch | Label |
| --- | --- |
| True path | "yes" |
| False path | "no" |

**Connector with label:**
```xml
<mxCell id="8" value="no" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" edge="1" parent="1" source="10" target="14">
    <mxGeometry relative="1" as="geometry"/>
</mxCell>
```

---

## 8. Layout Guidelines

### Flow Direction

| Direction | Use Case |
| --- | --- |
| Left to Right | Primary flow (main direction) |
| Top to Bottom | Swimlane transitions, sub-flows |

### Positioning

| Element | X Position Start | Y Position |
| --- | --- | --- |
| Start | 30 | 130 (within swimlane) |
| First process | ~290-460 | 110 |
| Subsequent | +180 spacing | Same row |
| API boxes | Below process | +100-200 from process |

### Shape Spacing

| Between | Spacing |
| --- | --- |
| Horizontal shapes | 150-200 px |
| Vertical rows | 100-200 px |
| Process to API box | 50-100 px vertical |

---

## 9. Title Box

Simple text without border:

**Style:**
```
text;html=1;whiteSpace=wrap;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;rounded=0;
```

**Example:**
```xml
<mxCell id="12" value="Staff Portal Manual Apply" style="text;html=1;whiteSpace=wrap;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;rounded=0;" vertex="1" parent="1">
    <mxGeometry x="40" y="10" width="200" height="30" as="geometry"/>
</mxCell>
```

---

## 10. Parent-Child Relationship

### Elements Inside Swimlanes

When placing elements inside a swimlane, set the parent attribute to the swimlane ID:

```xml
<!-- Swimlane with id="72" -->
<mxCell id="72" value="Staff Portal" style="swimlane;..." vertex="1" parent="1">
    <mxGeometry x="20" y="40" width="6710" height="630" as="geometry"/>
</mxCell>

<!-- Element inside swimlane, parent="72" -->
<mxCell id="73" value="show error response" style="whiteSpace=wrap;html=1;rounded=1;strokeWidth=2;" vertex="1" parent="72">
    <mxGeometry x="3210" y="520" width="120" height="60" as="geometry"/>
</mxCell>
```

### Elements Outside Swimlanes

Elements that span multiple swimlanes or connect them should have `parent="1"`:

```xml
<mxCell id="88" value="Validate Authentication" style="whiteSpace=wrap;html=1;rounded=1;strokeWidth=2;" vertex="1" parent="1">
    <mxGeometry x="3045" y="900" width="120" height="60" as="geometry"/>
</mxCell>
```

---

## 11. Checklist Before Saving

- [ ] Page named correctly (meaningful names like PS_High_Level, OCMS_Staff_Portal_Manual_PS)
- [ ] Swimlanes have correct colors (blue for frontend, green for backend)
- [ ] All shapes have labels
- [ ] Process shapes use default colors (no fill color added)
- [ ] All connectors have arrows
- [ ] Decision connectors have "yes"/"no" labels
- [ ] All paths lead to End
- [ ] API boxes are yellow with dashed line connections
- [ ] Reference texts are italic (fontStyle=2)
- [ ] Elements inside swimlanes have correct parent ID
- [ ] Canvas is wide enough for horizontal flow

---

## 12. Quick Reference: Style Codes

### Swimlanes
```
# Staff Portal (Frontend - Blue)
swimlane;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;

# OCMS Admin API (Backend - Green)
swimlane;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;
```

### Shapes
```
# Start/End
strokeWidth=2;html=1;shape=mxgraph.flowchart.terminator;whiteSpace=wrap;

# Process
rounded=1;whiteSpace=wrap;html=1;absoluteArcSize=1;arcSize=14;strokeWidth=2;

# Decision
strokeWidth=2;html=1;shape=mxgraph.flowchart.decision;whiteSpace=wrap;
```

### Special Boxes
```
# API/Payload Box (Yellow)
text;html=1;whiteSpace=wrap;strokeColor=#d6b656;fillColor=#fff2cc;align=left;verticalAlign=middle;rounded=0;

# Info Note (Blue)
text;html=1;whiteSpace=wrap;strokeColor=#6c8ebf;fillColor=#dae8fc;align=center;verticalAlign=middle;rounded=0;

# Reference (Italic, no border)
text;html=1;whiteSpace=wrap;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;rounded=0;fontStyle=2
```

### Connectors
```
# Normal flow
edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;

# Dashed (for API connections)
edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;dashed=1;dashPattern=8 8;
```

---

## 13. Complete Element Templates

### Full Process Box Template
```xml
<mxCell id="[ID]" value="[LABEL]" style="rounded=1;whiteSpace=wrap;html=1;absoluteArcSize=1;arcSize=14;strokeWidth=2;" vertex="1" parent="[SWIMLANE_ID]">
    <mxGeometry x="[X]" y="[Y]" width="100" height="100" as="geometry"/>
</mxCell>
```

### Full Decision Template
```xml
<mxCell id="[ID]" value="[CONDITION] ?" style="strokeWidth=2;html=1;shape=mxgraph.flowchart.decision;whiteSpace=wrap;" vertex="1" parent="[SWIMLANE_ID]">
    <mxGeometry x="[X]" y="[Y]" width="100" height="100" as="geometry"/>
</mxCell>
```

### Full API Box Template
```xml
<mxCell id="[ID]" value="POST /[endpoint]&lt;div&gt;{&lt;/div&gt;&lt;div&gt;  [payload]&lt;/div&gt;&lt;div&gt;}&lt;/div&gt;" style="text;html=1;whiteSpace=wrap;strokeColor=#d6b656;fillColor=#fff2cc;align=left;verticalAlign=middle;rounded=0;" vertex="1" parent="[SWIMLANE_ID]">
    <mxGeometry x="[X]" y="[Y]" width="[WIDTH]" height="[HEIGHT]" as="geometry"/>
</mxCell>
```

---

*End of Document*
