# Kola UI – Game/FPS Demo

## Snake Game Example

- See `apps.snake.SnakeGameApp` for a real-time game demo.
- Demonstrates FPS rendering, keyboard input, and dynamic UI.

## Running the Demo

```
javac -d output $(find src -name '*.java')
java -cp output apps.snake.SnakeGameApp 30
```

- The number (e.g., 30) is the FPS target.

## Features

- Arrow keys and WASD for movement
- Difficulty levels (speed)
- Real-time rendering and input
- Uses core UI components for game UI

## Custom Games

- Use the same architecture to build your own games or real-time apps.

---

See also: [README.md](../README.md)
