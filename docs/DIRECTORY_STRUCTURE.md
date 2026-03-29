# Kola UI – Directory Structure

```
ui-renderer/
├── src/
│   ├── main/           # Core windowing, rendering, base classes
│   ├── components/     # UI components (Button, Label, etc.)
│   ├── event/          # Event system
│   ├── layout/         # Layout engines
│   ├── style/          # Styling (TailwindParser, StyleManager)
│   ├── utils/          # Utilities
│   └── module-info.java (module name: kola.ui)
├── output/             # Compiled classes (javac -d output ...)
├── complete-app/       # Example/demo app
├── docs/               # Documentation
├── README.md           # Main documentation (Kola UI)
├── LICENSE             # MIT License
├── CHANGELOG.md        # Release notes
├── USAGE_EXAMPLES.md   # Code examples
├── MODULE_USAGE.md     # Module/JAR usage (kola-ui.jar)
```

---

See [README.md](../README.md) for usage and [docs/](.) for more guides.
