package kola.event;

import kola.main.BaseComp;
import kola.main.BaseWindow;

public class UiEvent {
    public enum Type {
        POINTER_DOWN,
        POINTER_MOVE,
        POINTER_UP,
        CLICK,
        WHEEL
    }

    private final Type type;
    private final int x;
    private final int y;
    private final int screenX;
    private final int screenY;
    private final int button;
    private final double wheelRotation;
    private final boolean shiftDown;
    private final int clickCount;
    private boolean propagationStopped;
    private BaseComp target;
    private BaseWindow window;

    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button) {
        this(type, x, y, screenX, screenY, button, 0.0, false, 1);
    }

    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, double wheelRotation) {
        this(type, x, y, screenX, screenY, button, wheelRotation, false, 1);
    }

    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, double wheelRotation,
            boolean shiftDown) {
        this(type, x, y, screenX, screenY, button, wheelRotation, shiftDown, 1);
    }

    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, double wheelRotation,
            boolean shiftDown, int clickCount) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.button = button;
        this.wheelRotation = wheelRotation;
        this.shiftDown = shiftDown;
        this.clickCount = Math.max(1, clickCount);
        this.propagationStopped = false;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public int getClickCount() {
        return clickCount;
    }

    public Type getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getScreenX() {
        return screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public int getButton() {
        return button;
    }

    public double getWheelRotation() {
        return wheelRotation;
    }

    public BaseComp getTarget() {
        return target;
    }

    public void setTarget(BaseComp target) {
        this.target = target;
    }

    public BaseWindow getWindow() {
        return window;
    }

    public void setWindow(BaseWindow window) {
        this.window = window;
    }

    public void stopPropagation() {
        this.propagationStopped = true;
    }

    public boolean isPropagationStopped() {
        return propagationStopped;
    }
}
