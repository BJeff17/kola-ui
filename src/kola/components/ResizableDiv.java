package kola.components;

import kola.event.UiEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class ResizableDiv extends Div {
    private static final int HANDLE_SIZE = 14;

    private int minWidth;
    private int minHeight;
    private boolean resizing;
    private boolean hoverHandle;
    private int startMouseX;
    private int startMouseY;
    private int startWidth;
    private int startHeight;

    public ResizableDiv(int x, int y, int width, int height, Color background, int radius) {
        super(x, y, width, height, background, radius);
        this.minWidth = 80;
        this.minHeight = 60;
        this.resizing = false;
        this.hoverHandle = false;
        setCursor(java.awt.Cursor.DEFAULT_CURSOR);
        registerResizeEvents();
    }

    public void setMinSize(int minWidth, int minHeight) {
        this.minWidth = Math.max(20, minWidth);
        this.minHeight = Math.max(20, minHeight);
    }

    public boolean isResizing() {
        return resizing;
    }

    @Override
    public void customGraphics(Graphics g) {
        super.customGraphics(g);

        Graphics2D g2 = (Graphics2D) g;
        int x0 = Math.max(0, getWidth() - HANDLE_SIZE);
        int y0 = Math.max(0, getHeight() - HANDLE_SIZE);

        g2.setColor(new Color(30, 41, 59, hoverHandle || resizing ? 220 : 145));
        g2.drawLine(x0 + 3, y0 + HANDLE_SIZE - 4, x0 + HANDLE_SIZE - 4, y0 + 3);
        g2.drawLine(x0 + 6, y0 + HANDLE_SIZE - 4, x0 + HANDLE_SIZE - 4, y0 + 6);
        g2.drawLine(x0 + 9, y0 + HANDLE_SIZE - 4, x0 + HANDLE_SIZE - 4, y0 + 9);

        if (hoverHandle || resizing) {
            g2.setColor(new Color(59, 130, 246, 120));
            g2.fillRoundRect(x0, y0, HANDLE_SIZE, HANDLE_SIZE, 6, 6);
        }
    }

    private void registerResizeEvents() {
        getEventManager().register(UiEvent.Type.POINTER_DOWN, (component, event) -> {
            if (event == null) {
                return;
            }
            int lx = toLocalX(event.getX());
            int ly = toLocalY(event.getY());
            if (!isInHandle(lx, ly)) {
                return;
            }
            resizing = true;
            startMouseX = event.getX();
            startMouseY = event.getY();
            startWidth = getWidth();
            startHeight = getHeight();
            setCursor(java.awt.Cursor.SE_RESIZE_CURSOR);
            if (event.getWindow() != null) {
                event.getWindow().capturePointer(this);
            }
            event.stopPropagation();
            invalidate();
        });

        getEventManager().register(UiEvent.Type.POINTER_MOVE, (component, event) -> {
            if (event == null) {
                return;
            }
            int lx = toLocalX(event.getX());
            int ly = toLocalY(event.getY());

            if (resizing) {
                int dx = event.getX() - startMouseX;
                int dy = event.getY() - startMouseY;
                int nextW = Math.max(minWidth, startWidth + dx);
                int nextH = Math.max(minHeight, startHeight + dy);
                setBounds(getX(), getY(), nextW, nextH);
                event.stopPropagation();
                return;
            }

            boolean nextHover = isInHandle(lx, ly);
            if (nextHover != hoverHandle) {
                hoverHandle = nextHover;
                setCursor(hoverHandle ? java.awt.Cursor.SE_RESIZE_CURSOR : java.awt.Cursor.DEFAULT_CURSOR);
                invalidate();
            }
        });

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (!resizing) {
                return;
            }
            resizing = false;
            if (event != null && event.getWindow() != null) {
                event.getWindow().releasePointer(this);
            }
            setCursor(hoverHandle ? java.awt.Cursor.SE_RESIZE_CURSOR : java.awt.Cursor.DEFAULT_CURSOR);
            if (event != null) {
                event.stopPropagation();
            }
            invalidate();
        });
    }

    private boolean isInHandle(int localX, int localY) {
        return localX >= (getWidth() - HANDLE_SIZE) && localY >= (getHeight() - HANDLE_SIZE)
                && localX < getWidth() && localY < getHeight();
    }
}
