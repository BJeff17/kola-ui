package components;

import event.UiEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.Timer;
import main.BaseComp;

public class ScrollView extends BaseComp {
    private enum ScrollAxis {
        NONE,
        HORIZONTAL,
        VERTICAL
    }

    private static final double BASE_PIXELS_PER_UNIT = 48.0;
    private static final long AXIS_LATCH_TIMEOUT_NANOS = readLatchTimeoutNanos("ui.scroll.latchMs", 220L);
    private static final double SCROLL_X_FACTOR = readFactor("ui.scroll.xFactor", 1.0);
    private static final double SCROLL_Y_FACTOR = readFactor("ui.scroll.yFactor", 1.0);

    private final BaseComp content;
    private int scrollY;
    private int scrollX;
    private int contentWidth;
    private int contentHeight;
    private boolean draggingVerticalThumb;
    private boolean draggingHorizontalThumb;
    private int dragStartPointerX;
    private int dragStartPointerY;
    private int dragStartScrollX;
    private int dragStartScrollY;
    private double wheelAccumulatorY;
    private double wheelAccumulatorX;
    private ScrollAxis latchedAxis;
    private long lastWheelEventNanos;
    private boolean scrollbarsVisible;
    private final Timer hideScrollbarsTimer;

    public ScrollView(int x, int y, int width, int height) {
        super(null);
        setBounds(x, y, width, height);
        this.content = new BaseComp(null);
        this.content.setBounds(0, 0, width, height);
        addChild(content);
        this.latchedAxis = ScrollAxis.NONE;
        this.lastWheelEventNanos = 0L;
        this.scrollbarsVisible = false;
        this.hideScrollbarsTimer = new Timer(850, e -> {
            if (draggingVerticalThumb || draggingHorizontalThumb) {
                return;
            }
            scrollbarsVisible = false;
            invalidate();
        });
        this.hideScrollbarsTimer.setRepeats(false);

        getEventManager().register(UiEvent.Type.WHEEL, (component, event) -> {
            double rotation = event.getWheelRotation();
            if (rotation == 0.0) {
                return;
            }

            requestShowScrollbars();

            boolean changed;
            boolean horizontalOverflow = canScrollHorizontally();
            boolean verticalOverflow = canScrollVertically();
            ScrollAxis axis = chooseLatchedAxis(event.isShiftDown(), horizontalOverflow, verticalOverflow);

            if (axis == ScrollAxis.HORIZONTAL) {
                wheelAccumulatorX += rotation * (BASE_PIXELS_PER_UNIT * SCROLL_X_FACTOR);
                int delta = consumeWheelAccumulatorX();
                if (delta == 0) {
                    return;
                }
                int before = scrollX;
                setScrollX(scrollX + delta);
                changed = before != scrollX;
            } else {
                wheelAccumulatorY += rotation * (BASE_PIXELS_PER_UNIT * SCROLL_Y_FACTOR);
                int delta = consumeWheelAccumulatorY();
                if (delta == 0) {
                    return;
                }
                int before = scrollY;
                setScrollY(scrollY + delta);
                changed = before != scrollY;
            }

            if (changed) {
                invalidate();
                event.stopPropagation();
                requestShowScrollbars();
            }
        });

        getEventManager().register(UiEvent.Type.POINTER_DOWN, (component, event) -> {
            int lx = toLocalX(event.getX());
            int ly = toLocalY(event.getY());

            Rectangle vThumb = getVerticalThumbBounds();
            Rectangle hThumb = getHorizontalThumbBounds();

            if (vThumb != null && vThumb.contains(lx, ly)) {
                draggingVerticalThumb = true;
                dragStartPointerY = ly;
                dragStartScrollY = scrollY;
                event.stopPropagation();
                requestShowScrollbars();
                invalidate();
                return;
            }

            if (hThumb != null && hThumb.contains(lx, ly)) {
                draggingHorizontalThumb = true;
                dragStartPointerX = lx;
                dragStartScrollX = scrollX;
                event.stopPropagation();
                requestShowScrollbars();
                invalidate();
            }
        });

        getEventManager().register(UiEvent.Type.POINTER_MOVE, (component, event) -> {
            if (!draggingVerticalThumb && !draggingHorizontalThumb) {
                return;
            }

            if (draggingVerticalThumb) {
                int maxScroll = getMaxScrollY();
                int trackLen = getHeight() - getVerticalThumbSize();
                if (maxScroll > 0 && trackLen > 0) {
                    int pointerDelta = toLocalY(event.getY()) - dragStartPointerY;
                    int scrollDelta = (int) Math.round((pointerDelta / (double) trackLen) * maxScroll);
                    setScrollY(dragStartScrollY + scrollDelta);
                }
            }

            if (draggingHorizontalThumb) {
                int maxScroll = getMaxScrollX();
                int trackLen = getWidth() - getHorizontalThumbSize();
                if (maxScroll > 0 && trackLen > 0) {
                    int pointerDelta = toLocalX(event.getX()) - dragStartPointerX;
                    int scrollDelta = (int) Math.round((pointerDelta / (double) trackLen) * maxScroll);
                    setScrollX(dragStartScrollX + scrollDelta);
                }
            }

            event.stopPropagation();
            requestShowScrollbars();
            invalidate();
        });

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (!draggingVerticalThumb && !draggingHorizontalThumb) {
                return;
            }
            draggingVerticalThumb = false;
            draggingHorizontalThumb = false;
            event.stopPropagation();
            requestShowScrollbars();
            invalidate();
        });

        getEventManager().register(UiEvent.Type.POINTER_MOVE, (component, event) -> {
            int lx = toLocalX(event.getX());
            int ly = toLocalY(event.getY());
            if (lx >= 0 && lx < getWidth() && ly >= 0 && ly < getHeight()) {
                if (canScrollHorizontally() || canScrollVertically()) {
                    requestShowScrollbars();
                }
            }
        });
    }

    public BaseComp getContent() {
        return content;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(getHeight(), contentHeight);
        syncContentPosition();
    }

    public void setContentWidth(int contentWidth) {
        this.contentWidth = Math.max(getWidth(), contentWidth);
        syncContentPosition();
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int newScrollY) {
        int max = getMaxScrollY();
        this.scrollY = Math.max(0, Math.min(newScrollY, max));
        syncContentPosition();
    }

    public int getScrollX() {
        return scrollX;
    }

    public void setScrollX(int newScrollX) {
        int max = getMaxScrollX();
        this.scrollX = Math.max(0, Math.min(newScrollX, max));
        syncContentPosition();
    }

    private int getMaxScrollY() {
        return Math.max(0, contentHeight - getHeight());
    }

    private int getMaxScrollX() {
        return Math.max(0, contentWidth - getWidth());
    }

    private boolean canScrollVertically() {
        return getMaxScrollY() > 0;
    }

    private boolean canScrollHorizontally() {
        return getMaxScrollX() > 0;
    }

    private int consumeWheelAccumulatorY() {
        int delta = (int) wheelAccumulatorY;
        wheelAccumulatorY -= delta;
        return delta;
    }

    private int consumeWheelAccumulatorX() {
        int delta = (int) wheelAccumulatorX;
        wheelAccumulatorX -= delta;
        return delta;
    }

    private void syncContentPosition() {
        content.setBounds(-scrollX, -scrollY, Math.max(getWidth(), contentWidth), Math.max(getHeight(), contentHeight));
    }

    private int getVerticalThumbSize() {
        if (contentHeight <= getHeight() || getHeight() <= 0) {
            return 0;
        }
        return Math.max(20, (int) ((getHeight() / (float) contentHeight) * getHeight()));
    }

    private int getHorizontalThumbSize() {
        if (contentWidth <= getWidth() || getWidth() <= 0) {
            return 0;
        }
        return Math.max(20, (int) ((getWidth() / (float) contentWidth) * getWidth()));
    }

    private Rectangle getVerticalThumbBounds() {
        int maxScroll = getMaxScrollY();
        int thumb = getVerticalThumbSize();
        if (maxScroll <= 0 || thumb <= 0) {
            return null;
        }
        int track = getHeight() - thumb;
        int y = (int) ((scrollY / (float) maxScroll) * track);
        return new Rectangle(getWidth() - 8, y, 6, thumb);
    }

    private Rectangle getHorizontalThumbBounds() {
        int maxScroll = getMaxScrollX();
        int thumb = getHorizontalThumbSize();
        if (maxScroll <= 0 || thumb <= 0) {
            return null;
        }
        int track = getWidth() - thumb;
        int x = (int) ((scrollX / (float) maxScroll) * track);
        return new Rectangle(x, getHeight() - 8, thumb, 6);
    }

    @Override
    public void paint(Graphics g) {
        customGraphics(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clipRect(0, 0, getWidth(), getHeight());
        paintChildren(g2);

        // Draw scrollbars
        if (scrollbarsVisible || draggingVerticalThumb || draggingHorizontalThumb) {
            Rectangle vThumb = getVerticalThumbBounds();
            Rectangle hThumb = getHorizontalThumbBounds();
            if (vThumb != null) {
                g2.setColor(draggingVerticalThumb ? new Color(120, 120, 120, 180) : new Color(150, 150, 150, 140));
                g2.fillRoundRect(vThumb.x, vThumb.y, vThumb.width, vThumb.height, 6, 6);
            }
            if (hThumb != null) {
                g2.setColor(
                        draggingHorizontalThumb ? new Color(120, 120, 120, 180) : new Color(150, 150, 150, 140));
                g2.fillRoundRect(hThumb.x, hThumb.y, hThumb.width, hThumb.height, 6, 6);
            }
        }

        g2.dispose();
    }

    private void requestShowScrollbars() {
        if (!(canScrollHorizontally() || canScrollVertically())) {
            return;
        }
        if (!scrollbarsVisible) {
            scrollbarsVisible = true;
            invalidate();
        }
        if (!draggingVerticalThumb && !draggingHorizontalThumb) {
            hideScrollbarsTimer.restart();
        }
    }

    private ScrollAxis chooseLatchedAxis(boolean shiftDown, boolean horizontalOverflow, boolean verticalOverflow) {
        long now = System.nanoTime();
        if (lastWheelEventNanos == 0L || (now - lastWheelEventNanos) > AXIS_LATCH_TIMEOUT_NANOS) {
            latchedAxis = ScrollAxis.NONE;
        }
        lastWheelEventNanos = now;

        ScrollAxis candidate = resolveAxisCandidate(shiftDown, horizontalOverflow, verticalOverflow);
        if (candidate == ScrollAxis.NONE) {
            return ScrollAxis.NONE;
        }

        if (latchedAxis == ScrollAxis.NONE) {
            latchedAxis = candidate;
            return latchedAxis;
        }

        if (latchedAxis == ScrollAxis.HORIZONTAL && !horizontalOverflow && verticalOverflow) {
            latchedAxis = ScrollAxis.VERTICAL;
            return latchedAxis;
        }

        if (latchedAxis == ScrollAxis.VERTICAL && !verticalOverflow && horizontalOverflow) {
            latchedAxis = ScrollAxis.HORIZONTAL;
            return latchedAxis;
        }

        if (shiftDown && horizontalOverflow) {
            latchedAxis = ScrollAxis.HORIZONTAL;
        }

        return latchedAxis;
    }

    private ScrollAxis resolveAxisCandidate(boolean shiftDown, boolean horizontalOverflow, boolean verticalOverflow) {
        if (shiftDown && horizontalOverflow) {
            return ScrollAxis.HORIZONTAL;
        }
        if (verticalOverflow) {
            return ScrollAxis.VERTICAL;
        }
        if (horizontalOverflow) {
            return ScrollAxis.HORIZONTAL;
        }
        return ScrollAxis.NONE;
    }

    private static double readFactor(String key, double fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            return clamp(parsed, 0.1, 8.0);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long readLatchTimeoutNanos(String key, long fallbackMs) {
        String raw = System.getProperty(key);
        long ms = fallbackMs;
        if (raw != null && !raw.isBlank()) {
            try {
                ms = Long.parseLong(raw.trim());
            } catch (NumberFormatException ignored) {
                ms = fallbackMs;
            }
        }
        ms = Math.max(40L, Math.min(ms, 2000L));
        return ms * 1_000_000L;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
