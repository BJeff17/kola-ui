# Kola UI Java Library

A modular, extensible Java UI rendering library for building custom desktop applications, games, and tools. Inspired by web frameworks (React, Tailwind CSS), Kola UI provides a component-based architecture, flexible styling, and real-time rendering capabilities.

---

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Core Concepts](#core-concepts)
- [Available Components](#available-components)
- [Styling with Tailwind-like Strings](#styling-with-tailwind-like-strings)
- [Custom Components](#custom-components)
- [Event Handling](#event-handling)
- [Window Management](#window-management)
- [Advanced Features](#advanced-features)
- [Extending the Library](#extending-the-library)
- [Integration & Embedding](#integration--embedding)
- [FAQ](#faq)
- [License](#license)

---

## Features

- **Component-based UI**: Compose UIs from reusable Java classes.
- **Real-time rendering**: 30+ FPS, suitable for games and dynamic apps.
- **Tailwind-style styling**: Use utility classes and custom values for layout, color, spacing, etc.
- **SVG & Image support**: Render SVG from string, load images from URL or file.
- **Advanced input**: Full keyboard, mouse, clipboard, undo/redo, selection.
- **Resizable & modal components**: Build interactive, resizable panels and dialogs.
- **Custom layout engines**: Flex, grid, block, absolute positioning.
- **Easy integration**: Use as a library in any Java project.

---

## Getting Started

### 1. Add to Your Project

- **Manual**: Copy the `src/` folder into your project, or build the library as a JAR (see [Integration & Embedding](#integration--embedding)).
- **Module**: (see [Making a Java Module](#making-a-java-module))

### 2. Minimal Example

```java
import main.BaseWindow;
import components.Button;

public class HelloUI {
    public static void main(String[] args) {
        BaseWindow window = new BaseWindow("Hello UI", 400, 300);
        Button btn = new Button("Click me", () -> System.out.println("Clicked!"));
        window.setContent(btn);
        window.show();
    }
}
```

---

## Core Concepts

- **Component (`BaseComp`)**: The base class for all UI elements.
- **Window (`BaseWindow`)**: Top-level container, manages focus, events, and rendering.
- **Layout Engines**: Control how children are arranged (Flex, Grid, Block, Absolute).
- **StyleManager/TailwindParser**: Parse and apply style strings.

---

## Available Components

- **Button**: Clickable button with action handler.
- **CheckBox**: Boolean toggle.
- **Label**: Static or dynamic text.
- **TextField**: Single-line text input (with selection, clipboard, undo/redo).
- **TextAreaInput**: Multi-line text input.
- **ImageComp**: Display images from file or URL, with alt text.
- **SvgFromStringComp**: Render SVG primitives from a string.
- **Div**: Generic container, supports styling and layout.
- **FormModal**: Modal dialog with form content.
- **ConfirmDialog**: Yes/No dialog.
- **NavMenuBar**: Horizontal/vertical menu bar.
- **LiveClockLabel**: Real-time clock label.
- **SegmentedSelect**: Tab-like segmented control.
- **SelectInput**: Dropdown/select input.
- **ScrollView**: Scrollable container.
- **ResizableDiv**: User-resizable panel (drag handles).

---

## Styling with Tailwind-like Strings

- Use `setStyle("...")` or constructor style param.
- Supported classes:
  - `bg-blue-500`, `bg-[#ff0]`, `bg-[rgb(255,0,0)]`, `bg-blue-500/60`
  - `rounded`, `rounded-[18]`
  - `w-64`, `h-32`, `w-[320]`, `h-[180]`
  - `grid-cols-2`, `gap-4`, `gap-[12]`
  - `flex`, `block`, `absolute`, `grid`
  - `p-4`, `m-2`, `border`, `border-red-500`, `shadow`, etc.
- Custom values: use brackets for px/rgb/hex values.
- Example:

```java
Div card = new Div();
card.setStyle("bg-white rounded-[12] shadow p-4 w-[320] h-[180]");
```

---

## Custom Components

- Extend `BaseComp` or any component.
- Override `render(Graphics2D g)`, `onEvent(UiEvent e)`, etc.
- Example:

```java
public class MyBadge extends BaseComp {
    public MyBadge(String text) { ... }
    @Override
    public void render(Graphics2D g) { ... }
}
```

---

## Event Handling

- All components support mouse and keyboard events.
- Use `onClick`, `onChange`, `onKey`, etc.
- Example:

```java
Button btn = new Button("Save", () -> saveData());
TextField tf = new TextField();
tf.setOnChange(val -> System.out.println("Changed: " + val));
```

---

## Window Management

- Create windows with `BaseWindow`.
- Set content, show/hide, manage focus.
- Example:

```java
BaseWindow win = new BaseWindow("Settings", 600, 400);
win.setContent(new SettingsPanel());
win.show();
```

---

## Advanced Features

- **SVG Rendering**: Use `SvgFromStringComp` for inline SVG.
- **Resizable Panels**: Use `ResizableDiv` for user-resizable areas.
- **Real-time Games**: See `apps.snake.SnakeGameApp` for FPS/game loop.
- **Custom Layouts**: Use/extend layout engines for custom arrangements.

---

## Extending the Library

- Add new components by extending `BaseComp`.
- Add new style rules in `TailwindParser`.
- Add new layout engines in `layout/`.

---

## Integration & Embedding

- **As Source**: Copy `src/` into your project.
- **As JAR**: Compile with `javac -d output $(find src -name '*.java')` then package:
  ```sh
  jar cf ui-renderer.jar -C output .
  ```
- **As Java Module**: See [Making a Java Module](#making-a-java-module).
- **Dependencies**: Pure Java SE, no external dependencies.

---

## FAQ

**Q: Is this Swing/JavaFX?**
A: No, it's a custom rendering engine using Java2D.

**Q: Can I use it in games?**
A: Yes! See the Snake game example.

**Q: How do I add my own styles?**
A: Extend `TailwindParser` and `StyleManager`.

**Q: How do I handle events?**
A: Use the event handler APIs on each component.

---

## License

MIT License (see LICENSE file)

---

## Making a Java Module

1. Add a `module-info.java` at the root of `src/`:

```java
module ui.renderer {
    exports main;
    exports components;
    exports event;
    exports layout;
    exports style;
    exports utils;
}
```

2. Compile with module support:

```sh
javac -d output --module-source-path src $(find src -name '*.java')
```

3. Package as a modular JAR:

```sh
jar --create --file ui-renderer.jar --main-class=main.BaseWindow -C output .
```

4. Use in your project:

```java
module my.app {
    requires ui.renderer;
}
```

---

For more examples, see the `complete-app/` and `src/apps/worksuite/WorkSuiteApp.java`.
