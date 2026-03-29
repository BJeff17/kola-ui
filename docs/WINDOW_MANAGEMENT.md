# Kola UI – Window Management

## BaseWindow

- Top-level window for your app.
- Manages content, focus, events, and rendering.

## Usage

```java
BaseWindow win = new BaseWindow("Settings", 600, 400);
win.setContent(new SettingsPanel());
win.show();
```

## Features

- Set title, size, and content
- Show/hide window
- Focus management (programmatic and user-driven)
- Window controls (close, minimize, etc.)
- Tooltips and hover effects

## Customization

- Extend `BaseWindow` for custom window logic
- Add new controls or behaviors as needed

---

See also: [API_OVERVIEW.md](API_OVERVIEW.md)
