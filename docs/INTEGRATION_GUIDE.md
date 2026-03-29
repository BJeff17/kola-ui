# Kola UI – Integration Guide

## As Source

- Copy the `src/` directory into your project.
- Import and use components as described in the README.

## As JAR

- Compile:
  ```sh
  javac -d output $(find src -name '*.java')
  jar cf ui-renderer.jar -C output .
  ```
- Add `ui-renderer.jar` to your classpath.

## As Java Module

- See [MODULE_USAGE.md](../MODULE_USAGE.md) for full instructions.

## Example Project

- See `complete-app/` for a full demo app using the library.
- See `src/apps/worksuite/WorkSuiteApp.java` for a live component showcase.

## Dependencies

- Pure Java SE (Java 11+ recommended)
- No external dependencies

---

For more, see [README.md](../README.md) and [docs/FAQ.md](FAQ.md).
