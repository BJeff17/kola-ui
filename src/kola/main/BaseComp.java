package kola.main;

import kola.event.EventManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import kola.style.StyleManager;

public class BaseComp {
    private static class ContainerQueryRule {
        private final int minWidth;
        private final int maxWidth;
        private final int minHeight;
        private final int maxHeight;
        private final Runnable onEnter;
        private final Runnable onExit;
        private boolean active;

        private ContainerQueryRule(int minWidth, int maxWidth, int minHeight, int maxHeight, Runnable onEnter,
                Runnable onExit) {
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.onEnter = onEnter;
            this.onExit = onExit;
            this.active = false;
        }

        private boolean matches(int width, int height) {
            return width >= minWidth && width <= maxWidth && height >= minHeight && height <= maxHeight;
        }
    }

    private StyleManager styleManager = null;
    private EventManager eventManager;
    private final List<BaseComp> children;
    private BaseComp parent;
    private BaseWindow ownerWindow;

    private int x;
    private int y;
    private int width;
    private int height;

    private boolean draggable;
    private boolean windowDragHandle;
    private boolean focusable;
    private boolean focused;
    private boolean visible;
    private final List<ContainerQueryRule> containerQueries;
    private boolean evaluatingContainerQueries;

    public BaseComp(BaseComp[] children) {
        this.children = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.containerQueries = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.eventManager = new EventManager();
        this.visible = true;
        this.evaluatingContainerQueries = false;
        if (children == null) {
            return;
        }
        for (BaseComp child : children) {
            attachChild(child);
        }
    }

    private void attachChild(BaseComp child) {
        if (child == null) {
            return;
        }
        child.parent = this;
        if (this.ownerWindow != null) {
            child.setOwnerWindow(this.ownerWindow);
        }
        this.children.add(child);
        invalidate();
    }

    public void setBounds(int x, int y, int width, int height) {
        Rectangle before = getGlobalBounds();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (styleManager != null) {
            styleManager.setBounds(x, y, width, height);
        }
        evaluateContainerQueries();
        Rectangle after = getGlobalBounds();
        if (ownerWindow != null) {
            ownerWindow.invalidateRect(before);
            ownerWindow.invalidateRect(after);
            ownerWindow.requestRenderIfNeeded();
        }
    }

    public void doLayout() {
        if (styleManager != null) {
            styleManager.doLayout(this);
        }
    }

    public void paint(Graphics g) {
        if (!visible) {
            return;
        }
        customGraphics(g);
        paintChildren(g);
    }

    protected void paintChildren(Graphics g) {
        if (children.isEmpty()) {
            return;
        }
        for (BaseComp child : children) {
            if (child == null || !child.isVisible()) {
                continue;
            }
            Graphics g_ = g.create();
            g_.translate(child.getX(), child.getY());

            // Allow style manager to apply additional transforms or clips if needed
            if (styleManager != null) {
                g_ = styleManager.createChildGraphics(this, child, g_);
            }

            child.paint(g_);
            g_.dispose();
        }
    }

    public void customGraphics(Graphics g) {
        if (styleManager != null) {
            styleManager.apply(g);
        }
    }

    public void render(Graphics2D g) {
        paint(g);
    }

    public boolean containsGlobalPoint(int globalX, int globalY) {
        if (!visible) {
            return false;
        }
        int gx = getGlobalX();
        int gy = getGlobalY();
        return globalX >= gx && globalX <= (gx + width) && globalY >= gy && globalY <= (gy + height);
    }

    public int toLocalX(int globalX) {
        return globalX - getGlobalX();
    }

    public int toLocalY(int globalY) {
        return globalY - getGlobalY();
    }

    public int getGlobalX() {
        if (parent == null) {
            return x;
        }
        return parent.getGlobalX() + x;
    }

    public int getGlobalY() {
        if (parent == null) {
            return y;
        }
        return parent.getGlobalY() + y;
    }

    public void addChild(BaseComp child) {
        attachChild(child);
    }

    public void removeChild(BaseComp child) {
        if (child == null) {
            return;
        }
        Rectangle oldBounds = child.getGlobalBounds();
        if (this.children.remove(child)) {
            child.parent = null;
            child.setOwnerWindow(null);
            if (ownerWindow != null) {
                ownerWindow.invalidateRect(oldBounds);
                ownerWindow.requestRenderIfNeeded();
            }
        }
    }

    public void moveChild(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= children.size()) {
            return;
        }
        int target = Math.max(0, Math.min(toIndex, children.size() - 1));
        BaseComp child = children.remove(fromIndex);
        children.add(target, child);
        invalidate();
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public BaseComp[] getChildren() {
        return children.toArray(BaseComp[]::new);
    }

    public List<BaseComp> getChildrenList() {
        return children;
    }

    public StyleManager getStyleManager() {
        return styleManager;
    }

    public void setStyleManager(StyleManager styleManager) {
        this.styleManager = styleManager;
    }

    public void setClass(String tailwindClasses) {
        if (this.styleManager == null) {
            this.styleManager = new StyleManager(tailwindClasses);
        } else {
            style.TailwindParser.applyTailwind(this.styleManager, tailwindClasses);
        }
    }

    public BaseComp getParent() {
        return parent;
    }

    public void setOwnerWindow(BaseWindow ownerWindow) {
        this.ownerWindow = ownerWindow;
        for (BaseComp child : children) {
            if (child != null) {
                child.setOwnerWindow(ownerWindow);
            }
        }
    }

    public BaseWindow getOwnerWindow() {
        return ownerWindow;
    }

    public Rectangle getGlobalBounds() {
        return new Rectangle(getGlobalX(), getGlobalY(), Math.max(1, width), Math.max(1, height));
    }

    public void invalidate() {
        if (ownerWindow == null) {
            return;
        }
        ownerWindow.invalidateComponent(this);
        ownerWindow.requestRenderIfNeeded();
    }

    public void invalidateLocalRect(int localX, int localY, int w, int h) {
        if (ownerWindow == null || w <= 0 || h <= 0) {
            return;
        }
        ownerWindow.invalidateRect(new Rectangle(getGlobalX() + localX, getGlobalY() + localY, w, h));
        ownerWindow.requestRenderIfNeeded();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean isWindowDragHandle() {
        return windowDragHandle;
    }

    public void setWindowDragHandle(boolean windowDragHandle) {
        this.windowDragHandle = windowDragHandle;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        invalidate();
    }

    public boolean onKeyPressed(int keyCode, char keyChar) {
        return false;
    }

    public boolean onKeyPressed(java.awt.event.KeyEvent event) {
        if (event == null) {
            return false;
        }
        return onKeyPressed(event.getKeyCode(), event.getKeyChar());
    }

    public boolean onKeyTyped(char keyChar) {
        return false;
    }

    public boolean onKeyTyped(java.awt.event.KeyEvent event) {
        if (event == null) {
            return false;
        }
        return onKeyTyped(event.getKeyChar());
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        invalidate();
    }

    public void addContainerQuery(int minWidth, int maxWidth, int minHeight, int maxHeight, Runnable onEnter,
            Runnable onExit) {
        ContainerQueryRule rule = new ContainerQueryRule(minWidth, maxWidth, minHeight, maxHeight,
                onEnter == null ? () -> {
                } : onEnter,
                onExit == null ? () -> {
                } : onExit);
        containerQueries.add(rule);
        evaluateContainerQueries();
    }

    public void addWidthContainerQuery(int maxWidth, Runnable onEnter, Runnable onExit) {
        addContainerQuery(Integer.MIN_VALUE, maxWidth, Integer.MIN_VALUE, Integer.MAX_VALUE, onEnter, onExit);
    }

    public void clearContainerQueries() {
        containerQueries.clear();
    }

    private void evaluateContainerQueries() {
        if (containerQueries.isEmpty() || evaluatingContainerQueries) {
            return;
        }
        evaluatingContainerQueries = true;
        try {
            int currentWidth = Math.max(0, width);
            int currentHeight = Math.max(0, height);
            for (ContainerQueryRule rule : containerQueries) {
                boolean matches = rule.matches(currentWidth, currentHeight);
                if (matches && !rule.active) {
                    rule.active = true;
                    rule.onEnter.run();
                } else if (!matches && rule.active) {
                    rule.active = false;
                    rule.onExit.run();
                }
            }
        } finally {
            evaluatingContainerQueries = false;
        }
    }

    private int cursor = java.awt.Cursor.DEFAULT_CURSOR;

    public int getCursor() {
        return cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

}
