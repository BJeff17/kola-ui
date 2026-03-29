# Kola UI – TailwindParser Guide

## Overview

- `TailwindParser` parses Tailwind-style utility strings into style objects.
- Supports a wide range of classes and custom values.

## Supported Classes

- Background: `bg-blue-500`, `bg-[#ff0]`, `bg-[rgb(255,0,0)]`, `bg-blue-500/60`
- Border: `border`, `border-red-500`, `border-[2]`
- Rounded: `rounded`, `rounded-[18]`
- Width/Height: `w-64`, `h-32`, `w-[320]`, `h-[180]`
- Grid: `grid`, `grid-cols-2`, `gap-4`, `gap-[12]`
- Flex: `flex`, `block`, `absolute`
- Padding/Margin: `p-4`, `m-2`, `p-[8]`, `m-[16]`
- Shadow: `shadow`, `shadow-lg`, etc.
- Text: `text-red-500`, `text-[18]`, `font-bold`, etc.

## Custom Values

- Use brackets for px/rgb/hex values: `w-[320]`, `bg-[#ff0]`, `gap-[12]`

## Extending

- Add new rules in `style/TailwindParser.java`.
- Add new color names, spacing, or logic as needed.

## Example

```java
Div card = new Div();
card.setStyle("bg-white rounded-[12] shadow p-4 w-[320] h-[180]");
```

---

See also: [STYLING_GUIDE.md](STYLING_GUIDE.md)
