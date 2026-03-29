# How to Build and Use Kola UI as a Java Module

## 1. Compile as a Java Module

From the root directory:

```
javac -d output --module-source-path src $(find src -name '*.java')
```

## 2. Create a Modular JAR

```
jar --create --file kola-ui.jar --main-class=main.BaseWindow -C output .
```

## 3. Use in Your Project

- Add `kola-ui.jar` to your module path.
- In your `module-info.java`:

```
module my.app {
    requires kola.ui;
}
```

- Import and use components as described in the README and USAGE_EXAMPLES.md.

## 4. Run Example

```
java --module-path ui-renderer.jar:. -m ui.renderer/main.BaseWindow
```

Or run your own main class that uses the library.

---

For troubleshooting, see the FAQ in README.md.
