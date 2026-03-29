# Kola UI – Component Reference

## Button

- Usage: `new Button(String label, Runnable onClick)`
- Events: onClick
- Style: All Tailwind classes

## CheckBox

- Usage: `new CheckBox(boolean checked, Consumer<Boolean> onChange)`
- Events: onChange

## Label

- Usage: `new Label(String text)`
- Dynamic: Can update text at runtime

## TextField

- Usage: `new TextField(String initial)`
- Features: Selection, clipboard, undo/redo
- Events: onChange, onKey

## TextAreaInput

- Usage: `new TextAreaInput(String initial)`
- Features: Multi-line, selection, clipboard, undo/redo

## ImageComp

- Usage: `new ImageComp(String pathOrUrl, String alt)`
- Supports: Local file or URL, alt text fallback

## SvgFromStringComp

- Usage: `new SvgFromStringComp(String svgString)`
- Renders: SVG primitives (rect, circle, path, etc.)

## Div

- Usage: `new Div()`
- Container: Holds children, supports layout/style

## ResizableDiv

- Usage: `new ResizableDiv()`
- Features: Mouse-resizable, min size, drag handles

## ConfirmDialog

- Usage: `new ConfirmDialog(String msg, Runnable onYes, Runnable onNo)`
- Modal: Blocks until user responds

## FormModal

- Usage: `new FormModal(BaseComp content, Runnable onSubmit)`
- Modal: Custom form content

## NavMenuBar

- Usage: `new NavMenuBar(List<MenuItem> items)`
- Menu: Horizontal/vertical navigation

## LiveClockLabel

- Usage: `new LiveClockLabel()`
- Dynamic: Real-time clock

## SegmentedSelect

- Usage: `new SegmentedSelect(String[] options, Consumer<Integer> onSelect)`
- Tab-like segmented control

## SelectInput

- Usage: `new SelectInput(String[] options, Consumer<String> onSelect)`
- Dropdown/select input

## ScrollView

- Usage: `new ScrollView(BaseComp content)`
- Scrollable container

---

For more, see [API_OVERVIEW.md](API_OVERVIEW.md) and [USAGE_EXAMPLES.md](../USAGE_EXAMPLES.md).
