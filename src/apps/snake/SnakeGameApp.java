package apps.snake;

import kola.components.Div;
import kola.components.H;
import kola.components.Label;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import javax.swing.Timer;
import kola.main.BaseComp;
import kola.main.BaseWindow;

public class SnakeGameApp {
    private enum Difficulty {
        EASY("Facile", 170),
        NORMAL("Normal", 120),
        HARD("Difficile", 85),
        EXPERT("Expert", 60);

        final String label;
        final int stepDelayMs;

        Difficulty(String label, int stepDelayMs) {
            this.label = label;
            this.stepDelayMs = stepDelayMs;
        }
    }

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    private static class Cell {
        final int x;
        final int y;

        Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Cell other)) {
                return false;
            }
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return (31 * x) + y;
        }
    }

    private static class SnakeBoard extends BaseComp {
        private final int cols;
        private final int rows;
        private final int cellSize;
        private final Deque<Cell> snake;
        private final Random random;
        private Direction direction;
        private Direction queuedDirection;
        private Cell food;
        private boolean gameOver;
        private int score;
        private Timer stepTimer;
        private Difficulty difficulty;

        SnakeBoard(int x, int y, int cols, int rows, int cellSize) {
            super(null);
            this.cols = cols;
            this.rows = rows;
            this.cellSize = cellSize;
            this.snake = new ArrayDeque<>();
            this.random = new Random();
            this.direction = Direction.RIGHT;
            this.queuedDirection = Direction.RIGHT;
            this.food = new Cell(0, 0);
            this.gameOver = false;
            this.score = 0;
            this.difficulty = Difficulty.NORMAL;

            setBounds(x, y, cols * cellSize, rows * cellSize);
            setFocusable(true);
            setCursor(java.awt.Cursor.HAND_CURSOR);

            getEventManager().register(event.UiEvent.Type.POINTER_DOWN, (component, event) -> {
                if (event.getTarget() == this && gameOver) {
                    restart();
                }
            });

            restart();
        }

        void start() {
            if (stepTimer != null) {
                stepTimer.stop();
            }
            stepTimer = new Timer(difficulty.stepDelayMs, e -> tick());
            stepTimer.setRepeats(true);
            stepTimer.start();
        }

        void setDifficulty(Difficulty difficulty) {
            if (difficulty == null || this.difficulty == difficulty) {
                return;
            }
            this.difficulty = difficulty;
            if (stepTimer != null) {
                stepTimer.setDelay(difficulty.stepDelayMs);
                stepTimer.setInitialDelay(difficulty.stepDelayMs);
            }
            invalidate();
        }

        @Override
        public boolean onKeyPressed(KeyEvent e) {
            if (!isFocused() || e == null) {
                return false;
            }

            int key = e.getKeyCode();
            if (key == KeyEvent.VK_R) {
                restart();
                return true;
            }
            if (key == KeyEvent.VK_1) {
                setDifficulty(Difficulty.EASY);
                return true;
            }
            if (key == KeyEvent.VK_2) {
                setDifficulty(Difficulty.NORMAL);
                return true;
            }
            if (key == KeyEvent.VK_3) {
                setDifficulty(Difficulty.HARD);
                return true;
            }
            if (key == KeyEvent.VK_4) {
                setDifficulty(Difficulty.EXPERT);
                return true;
            }
            if (key == KeyEvent.VK_UP) {
                queueDirection(Direction.UP);
                return true;
            }
            if (key == KeyEvent.VK_W) {
                queueDirection(Direction.UP);
                return true;
            }
            if (key == KeyEvent.VK_DOWN) {
                queueDirection(Direction.DOWN);
                return true;
            }
            if (key == KeyEvent.VK_S) {
                queueDirection(Direction.DOWN);
                return true;
            }
            if (key == KeyEvent.VK_LEFT) {
                queueDirection(Direction.LEFT);
                return true;
            }
            if (key == KeyEvent.VK_A) {
                queueDirection(Direction.LEFT);
                return true;
            }
            if (key == KeyEvent.VK_RIGHT) {
                queueDirection(Direction.RIGHT);
                return true;
            }
            if (key == KeyEvent.VK_D) {
                queueDirection(Direction.RIGHT);
                return true;
            }
            return false;
        }

        @Override
        public void customGraphics(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;

            g2.setColor(new Color(17, 24, 39));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

            drawGrid(g2);
            drawFood(g2);
            drawSnake(g2);

            g2.setColor(new Color(241, 245, 249));
            g2.setFont(new Font("Dialog", Font.BOLD, 14));
            g2.drawString("Score: " + score, 12, 22);
            g2.drawString("Niveau: " + difficulty.label + " (" + difficulty.stepDelayMs + "ms)", 128, 22);
            g2.setFont(new Font("Dialog", Font.PLAIN, 12));
            g2.setColor(new Color(191, 219, 254));
            g2.drawString("1 Facile  2 Normal  3 Difficile  4 Expert", 12, Math.max(38, getHeight() - 10));

            if (gameOver) {
                g2.setColor(new Color(15, 23, 42, 190));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(248, 113, 113));
                g2.setFont(new Font("Dialog", Font.BOLD, 24));
                g2.drawString("Game Over", Math.max(12, (getWidth() / 2) - 64), Math.max(28, (getHeight() / 2) - 8));
                g2.setColor(new Color(191, 219, 254));
                g2.setFont(new Font("Dialog", Font.PLAIN, 13));
                g2.drawString("R pour recommencer ou clic sur la zone", Math.max(12, (getWidth() / 2) - 112),
                        Math.max(48, (getHeight() / 2) + 16));
            }
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(new Color(30, 41, 59));
            for (int x = 0; x <= cols; x++) {
                int px = x * cellSize;
                g2.drawLine(px, 0, px, rows * cellSize);
            }
            for (int y = 0; y <= rows; y++) {
                int py = y * cellSize;
                g2.drawLine(0, py, cols * cellSize, py);
            }
        }

        private void drawFood(Graphics2D g2) {
            int x = food.x * cellSize;
            int y = food.y * cellSize;
            g2.setColor(new Color(239, 68, 68));
            g2.fillOval(x + 3, y + 3, cellSize - 6, cellSize - 6);
        }

        private void drawSnake(Graphics2D g2) {
            boolean head = true;
            for (Cell part : snake) {
                int x = part.x * cellSize;
                int y = part.y * cellSize;
                if (head) {
                    g2.setColor(new Color(74, 222, 128));
                    g2.fillRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 8, 8);
                    head = false;
                } else {
                    g2.setColor(new Color(34, 197, 94));
                    g2.fillRoundRect(x + 3, y + 3, cellSize - 6, cellSize - 6, 6, 6);
                }
            }
        }

        private void restart() {
            snake.clear();
            int startX = cols / 2;
            int startY = rows / 2;
            snake.addFirst(new Cell(startX, startY));
            snake.addLast(new Cell(startX - 1, startY));
            snake.addLast(new Cell(startX - 2, startY));
            direction = Direction.RIGHT;
            queuedDirection = Direction.RIGHT;
            score = 0;
            gameOver = false;
            placeFood();
            invalidate();
        }

        private void tick() {
            if (gameOver || snake.isEmpty()) {
                invalidate();
                return;
            }

            direction = queuedDirection;
            Cell head = snake.peekFirst();
            int nextX = head.x;
            int nextY = head.y;
            switch (direction) {
                case UP -> nextY -= 1;
                case DOWN -> nextY += 1;
                case LEFT -> nextX -= 1;
                case RIGHT -> nextX += 1;
                default -> {
                }
            }

            Cell next = new Cell(nextX, nextY);
            if (nextX < 0 || nextY < 0 || nextX >= cols || nextY >= rows || isOnSnake(next, true)) {
                gameOver = true;
                invalidate();
                return;
            }

            snake.addFirst(next);
            if (next.equals(food)) {
                score += 10;
                placeFood();
            } else {
                snake.removeLast();
            }

            invalidate();
        }

        private boolean isOnSnake(Cell c, boolean ignoreTail) {
            int index = 0;
            int lastIndex = snake.size() - 1;
            for (Cell part : snake) {
                if (ignoreTail && index == lastIndex) {
                    index++;
                    continue;
                }
                if (part.equals(c)) {
                    return true;
                }
                index++;
            }
            return false;
        }

        private void queueDirection(Direction desired) {
            if (gameOver) {
                return;
            }
            if (isOpposite(direction, desired)) {
                return;
            }
            queuedDirection = desired;
        }

        private boolean isOpposite(Direction current, Direction next) {
            if (current == Direction.UP && next == Direction.DOWN) {
                return true;
            }
            if (current == Direction.DOWN && next == Direction.UP) {
                return true;
            }
            if (current == Direction.LEFT && next == Direction.RIGHT) {
                return true;
            }
            return current == Direction.RIGHT && next == Direction.LEFT;
        }

        private void placeFood() {
            Cell nextFood;
            do {
                nextFood = new Cell(random.nextInt(cols), random.nextInt(rows));
            } while (isOnSnake(nextFood, false));
            food = nextFood;
        }
    }

    public static void main(String[] args) {
        int fps = 30;
        if (args.length > 0) {
            try {
                fps = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                fps = 30;
            }
        }
        fps = Math.max(1, fps);

        BaseWindow window = new BaseWindow("Snake Test - UI Renderer", 900, 680, fps);
        BaseComp content = window.getContent();

        content.setStyleManager(new style.StyleManager(new Color(236, 239, 244), 0,
                content.getWidth(), content.getHeight(), 0, 0, "absolute"));

        Div header = new Div(12, 10, 860, 58, new Color(30, 41, 59), 12);
        content.addChild(header);

        H title = new H("Snake Renderer Test", 14, 10, 320, 28);
        title.setColor(new Color(241, 245, 249));
        header.addChild(title);

        Label hint = new Label("Fleches/WASD pour jouer. 1..4=vitesses. R=restart. FPS actif=" + fps,
                14, 34, 800, 18);
        hint.setColor(new Color(191, 219, 254));
        hint.setFont(new Font("Dialog", Font.PLAIN, 12));
        header.addChild(hint);

        SnakeBoard board = new SnakeBoard(12, 78, 30, 20, 26);
        content.addChild(board);

        board.start();
        window.show();
        window.focusComponent(board);
    }
}
