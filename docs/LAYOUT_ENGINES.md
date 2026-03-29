# Kola UI – Layout Engines

## Overview

- Layout engines control how child components are arranged.
- Each container (e.g., Div) can use a different layout engine.

## Available Layouts

- **FlexLayoutEngine**: Flexbox-like, row/column, grow/shrink.
- **GridLayoutEngine**: CSS grid-like, rows/columns, gap, grid-cols-\*
- **BlockLayoutEngine**: Vertical stacking, like HTML block elements.
- **AbsoluteLayoutEngine**: Manual positioning (x, y, w, h).

## Usage

- Set layout via style string (e.g., `flex`, `grid`, `block`, `absolute`).
- Or set layout engine directly in code.

## Example

```java
Div flexBox = new Div();
flexBox.setStyle("flex gap-4");

Div grid = new Div();
grid.setStyle("grid grid-cols-2 gap-2");
```

## Custom Layouts

- Extend `BaseLayoutEngine` to create new layouts.

---

See also: [API_OVERVIEW.md](API_OVERVIEW.md), [STYLING_GUIDE.md](STYLING_GUIDE.md)
