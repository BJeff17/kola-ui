sed -i '/private final int wheelRotation;/a \    private final boolean shiftDown;' src/event/UiEvent.java

sed -i 's/public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, int wheelRotation) {/public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, int wheelRotation) {\n        this(type, x, y, screenX, screenY, button, wheelRotation, false);\n    }\n\n    public UiEvent(Type type, int x, int y, int screenX, int screenY, int button, int wheelRotation, boolean shiftDown) {/g' src/event/UiEvent.java

sed -i '/this.wheelRotation = wheelRotation;/a \        this.shiftDown = shiftDown;' src/event/UiEvent.java

sed -i '/return wheelRotation;/a \    }\n\n    public boolean isShiftDown() {\n        return shiftDown;' src/event/UiEvent.java

