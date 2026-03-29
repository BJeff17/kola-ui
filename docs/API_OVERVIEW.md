# Kola UI – API Overview

## Package Structure

- `main` – Core windowing, rendering, and base component classes
- `components` – UI components (Button, Label, TextField, etc.)
- `event` – Event system (UiEvent, EventManager)
- `layout` – Layout engines (Flex, Grid, Block, Absolute)
- `style` – Styling (TailwindParser, StyleManager)
- `utils` – Utility classes (DirtyManager, HitTester, TileManager)

## Key Classes

### main.BaseWindow

- Top-level window, manages content, focus, events, and rendering.
- Methods: `setContent(BaseComp)`, `show()`, `hide()`, `setTitle(String)`, `focusComponent(BaseComp)`

### main.BaseComp

- Base class for all UI components.
- Methods: `setStyle(String)`, `setBounds(int x, int y, int w, int h)`, `onEvent(UiEvent)`
- Override: `render(Graphics2D)`, `onEvent(UiEvent)`

### components.\*

- `Button`, `Label`, `TextField`, `TextAreaInput`, `ImageComp`, `SvgFromStringComp`, `Div`, `ResizableDiv`, etc.
- Each component has its own API for content, events, and style.

### style.TailwindParser

- Parses Tailwind-like style strings into style objects.
- Supports: bg, rounded, w/h, grid, flex, gap, p/m, border, shadow, etc.

### event.UiEvent

- Encapsulates mouse, keyboard, and custom events.
- Used in all interactive components.

### layout.\*

- `FlexLayoutEngine`, `GridLayoutEngine`, `BlockLayoutEngine`, `AbsoluteLayoutEngine`
- Used internally by containers (Div, etc.)

## Customization

- Extend `BaseComp` for new components.
- Add style rules in `TailwindParser`.
- Implement new layout engines in `layout/`.

## See Also

- [README.md](../README.md)
- [USAGE_EXAMPLES.md](../USAGE_EXAMPLES.md)
- [MODULE_USAGE.md](../MODULE_USAGE.md)
