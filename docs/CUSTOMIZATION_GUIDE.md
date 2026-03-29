# Kola UI – Customization Guide

## Custom Components

- Extend `main.BaseComp` or any component.
- Override `render(Graphics2D g)`, `onEvent(UiEvent e)`, etc.
- Example:

```java
public class MyBadge extends BaseComp {
    public MyBadge(String text) { ... }
    @Override
    public void render(Graphics2D g) { ... }
}
```

## Custom Styles

- Add new style rules in `style/TailwindParser.java`.
- Example: Add support for `text-shadow` or new color names.

## Custom Layouts

- Implement a new layout engine in `layout/`.
- Example: `public class MasonryLayoutEngine extends BaseLayoutEngine { ... }`

## Custom Events

- Extend `event.UiEvent` for new event types.
- Use `EventManager` to dispatch/listen for custom events.

## Tips

- All components are composable and extensible.
- Use style strings for rapid prototyping.
- See `src/components/` for examples.

---

See also: [API_OVERVIEW.md](API_OVERVIEW.md), [COMPONENT_REFERENCE.md](COMPONENT_REFERENCE.md)
