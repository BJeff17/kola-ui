# Kola UI – ResizableDiv Guide

## Overview

- `ResizableDiv` is a container that can be resized by the user with the mouse.
- Supports min size, drag handles, and live resizing.

## Usage

```java
ResizableDiv resDiv = new ResizableDiv();
resDiv.setContent(new Label("Drag to resize me!"));
resDiv.setStyle("w-[320] h-[180] bg-gray-100 rounded shadow");
```

## Features

- Drag handle (bottom-right corner)
- Enforces minimum width/height
- Can contain any component as content
- Emits resize events (if needed)

## Customization

- Extend `ResizableDiv` to add more handles (edges/corners)
- Style with any Tailwind classes

## Demo

- See `src/apps/worksuite/WorkSuiteApp.java` for a live demo.

---

See also: [COMPONENT_REFERENCE.md](COMPONENT_REFERENCE.md)
