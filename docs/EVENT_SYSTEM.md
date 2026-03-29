# Kola UI – Event System

## Overview

- All interactive components use the event system.
- Events are represented by `event.UiEvent`.
- Event types: mouse, keyboard, focus, custom.

## Handling Events

- Override `onEvent(UiEvent e)` in your component.
- Use provided handler APIs: `onClick`, `onChange`, `onKey`, etc.

## Example: Button Click

```java
Button btn = new Button("OK", () -> System.out.println("Clicked!"));
```

## Example: Text Change

```java
TextField tf = new TextField();
tf.setOnChange(val -> System.out.println("Changed: " + val));
```

## Custom Events

- Extend `UiEvent` for new event types.
- Use `EventManager` to dispatch/listen for custom events.

## Focus Management

- Use `BaseWindow.focusComponent(BaseComp)` to set focus.
- Components can request focus on click or programmatically.

---

See also: [API_OVERVIEW.md](API_OVERVIEW.md), [COMPONENT_REFERENCE.md](COMPONENT_REFERENCE.md)
