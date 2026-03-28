package event;

import main.BaseComp;
import main.BaseWindow;

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
    private final int wheelRotation;
    private BaseComp target;
    private BaseWindow window;

    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button) {
        this(type, x, y, screenX, screenY, button, 0);
    }

    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, int wheelRotation) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.button = button;
        this.wheelRotation = wheelRotation;
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

    public int getWheelRotation() {
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
}
