# Kola UI – FAQ

**Q: Is this Swing/JavaFX?**
A: No, it's a custom rendering engine using Java2D.

**Q: Can I use it in games?**
A: Yes! See the Snake game example (`apps.snake.SnakeGameApp`).

**Q: How do I add my own styles?**
A: Extend `TailwindParser` and `StyleManager` in `style/`.

**Q: How do I handle events?**
A: Use the event handler APIs on each component (e.g., `onClick`, `onChange`).

**Q: How do I make a modal dialog?**
A: Use `ConfirmDialog` or `FormModal`.

**Q: How do I make a resizable panel?**
A: Use `ResizableDiv`.

**Q: How do I use images or SVG?**
A: Use `ImageComp` for images (file/URL/alt), `SvgFromStringComp` for SVG strings.

**Q: How do I use as a module?**
A: See `MODULE_USAGE.md` for full instructions.

**Q: What Java version is required?**
A: Java 11+ recommended (uses Java2D, no external dependencies).

---

For more, see [README.md](../README.md) and [API_OVERVIEW.md](API_OVERVIEW.md).
