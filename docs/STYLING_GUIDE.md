# Kola UI – Styling Guide

## Tailwind-style Classes

- Use `setStyle("...")` or constructor style param.
- Supported classes:
  - `bg-blue-500`, `bg-[#ff0]`, `bg-[rgb(255,0,0)]`, `bg-blue-500/60`
  - `rounded`, `rounded-[18]`
  - `w-64`, `h-32`, `w-[320]`, `h-[180]`
  - `grid-cols-2`, `gap-4`, `gap-[12]`
  - `flex`, `block`, `absolute`, `grid`
  - `p-4`, `m-2`, `border`, `border-red-500`, `shadow`, etc.
- Custom values: use brackets for px/rgb/hex values.

## Examples

```java
Div card = new Div();
card.setStyle("bg-white rounded-[12] shadow p-4 w-[320] h-[180]");

Button btn = new Button("OK", () -> {});
btn.setStyle("bg-blue-500 text-white rounded w-32");
```

## Extending Styles

- Add new rules in `style/TailwindParser.java`.
- Add new color names, spacing, or custom logic as needed.

## Tips

- Combine multiple classes for complex layouts.
- Use bracketed values for custom sizes/colors.
- All components support style strings.

---

See also: [API_OVERVIEW.md](API_OVERVIEW.md), [COMPONENT_REFERENCE.md](COMPONENT_REFERENCE.md)
