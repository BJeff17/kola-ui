# Kola UI – SVG & Image Support

## SVG Rendering

- Use `SvgFromStringComp` to render SVG primitives from a string.
- Supports: `<rect>`, `<circle>`, `<ellipse>`, `<line>`, `<path>`, `<polygon>`, `<polyline>`, etc.
- Example:

```java
SvgFromStringComp svg = new SvgFromStringComp("<svg viewBox='0 0 100 100'><circle cx='50' cy='50' r='40' fill='red'/></svg>");
```

## Image Rendering

- Use `ImageComp` to display images from file or URL.
- Supports: PNG, JPEG, GIF, etc.
- Alt text: Displayed if image fails to load.
- Example:

```java
ImageComp img = new ImageComp("https://example.com/image.png", "Alt text");
```

## Customization

- Extend `ImageComp` or `SvgFromStringComp` for advanced rendering.

---

See also: [COMPONENT_REFERENCE.md](COMPONENT_REFERENCE.md)
