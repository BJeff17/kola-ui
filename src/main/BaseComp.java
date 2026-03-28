package main;

import event.EventManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import style.StyleManager;

public class BaseComp {
    private StyleManager styleManager = null;
    private EventManager eventManager;
    private final List<BaseComp> children;
    private BaseComp parent;

    private int x;
    private int y;
    private int width;
    private int height;

    private boolean draggable;
    private boolean windowDragHandle;

    public BaseComp(BaseComp[] children) {
        this.children = new ArrayList<>();
        this.eventManager = new EventManager();
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
        this.children.add(child);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (styleManager != null) {
            styleManager.setBounds(x, y, width, height);
        }
    }

    public void doLayout() {
        if (styleManager != null) {
            styleManager.doLayout(this);
        }
    }

    public void paint(Graphics g) {
        customGraphics(g);
        paintChildren(g);
    }

    protected void paintChildren(Graphics g) {
        if (children.isEmpty()) {
            return;
        }
        for (BaseComp child : children) {
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
        if (this.children.remove(child)) {
            child.parent = null;
        }
    }

    public void moveChild(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= children.size()) {
            return;
        }
        int target = Math.max(0, Math.min(toIndex, children.size() - 1));
        BaseComp child = children.remove(fromIndex);
        children.add(target, child);
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

    public BaseComp getParent() {
        return parent;
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

    private int cursor = java.awt.Cursor.DEFAULT_CURSOR;
    
    public int getCursor() {
        return cursor;
    }
    
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

}
