package main;

import event.UiEvent;
import event.UiEvent.Type;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import style.StyleManager;
import utils.DirtyManager;
import utils.HitTester;

public class BaseWindow extends BaseComp {
    private static final int DEFAULT_HEADER_HEIGHT = 38;
    private static final int FRAME_RADIUS = 18;
    private static final int RESIZE_BORDER = 2;
    private static final int MIN_WIDTH = 520;
    private static final int MIN_HEIGHT = 380;

    private enum ResizeEdge {
        NONE,
        NORTH,
        SOUTH,
        EAST,
        WEST,
        NORTH_EAST,
        NORTH_WEST,
        SOUTH_EAST,
        SOUTH_WEST
    }

    private final JFrame frame;
    private final JPanel canvas;
    private final HitTester hitTester;
    private final DirtyManager dirtyManager;
    private final int fps;

    private BaseComp root;
    private BaseComp header;
    private BaseComp content;
    private BaseComp layerHost;
    private BaseComp capturedPointer;
    private BaseComp pressedSystemButton;
    private boolean windowDragActive;
    private int windowDragOffsetX;
    private int windowDragOffsetY;
    private ResizeEdge activeResizeEdge;
    private Point resizeStartMouse;
    private Rectangle resizeStartBounds;
    private final Map<BaseComp, Consumer<BaseWindow>> systemButtons;
    private final List<Runnable> resizeListeners;
    private final List<BaseWindow> childWindows;
    private final Map<BaseComp, ExecutorService> modalWorkers;
    private final ExecutorService windowWorker;

    private BaseComp closeButton;
    private BaseComp minimizeButton;
    private BaseComp maximizeButton;

    private Timer activeRenderTimer;

    public BaseWindow(String title, int width, int height, int fps) {
        super(null);
        this.fps = Math.max(0, fps);
        this.hitTester = new HitTester();
        this.dirtyManager = new DirtyManager();
        this.frame = new JFrame(title);
        this.canvas = createCanvas();
        this.activeResizeEdge = ResizeEdge.NONE;
        this.systemButtons = new HashMap<>();
        this.resizeListeners = new ArrayList<>();
        this.childWindows = new ArrayList<>();
        this.modalWorkers = new HashMap<>();
        this.windowWorker = Executors.newSingleThreadExecutor(new NamedThreadFactory("ui-window"));

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        frame.setSize(width, height);
        frame.setContentPane(canvas);
        canvas.setOpaque(false);
        canvas.setBackground(new Color(0, 0, 0, 0));
        canvas.setBorder(BorderFactory.createEmptyBorder());
        frame.setLocationRelativeTo(null);
        applyRoundedShape(width, height);

        this.root = createDefaultRoot(width, height);
        installResizeHook();
        installWindowCloseHook();
        wireMouseEvents();
    }

    @Override
    public void paint(Graphics g) {
        if (root != null) {
            root.paint(g);
        }
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            startRenderLoopIfNeeded();
            requestRender();
        });
    }

    public void dispose() {
        for (BaseWindow childWindow : childWindows) {
            if (childWindow != null) {
                childWindow.dispose();
            }
        }
        childWindows.clear();
        for (ExecutorService worker : modalWorkers.values()) {
            worker.shutdownNow();
        }
        modalWorkers.clear();
        windowWorker.shutdownNow();
        if (activeRenderTimer != null) {
            activeRenderTimer.stop();
        }
        frame.dispose();
    }

    public JFrame getNativeFrame() {
        return frame;
    }

    public BaseComp getRoot() {
        return root;
    }

    public BaseComp getHeader() {
        return header;
    }

    public BaseComp getContent() {
        return content;
    }

    public BaseComp getLayerHost() {
        return layerHost;
    }

    public void addResizeListener(Runnable listener) {
        if (listener != null) {
            resizeListeners.add(listener);
        }
    }

    public BaseWindow openChildWindow(String title, int width, int height, int childFps) {
        BaseWindow childWindow = new BaseWindow(title, width, height, childFps);
        childWindows.add(childWindow);
        windowWorker.submit(childWindow::show);
        return childWindow;
    }

    public BaseComp openLayer(BaseComp layer) {
        if (layer == null || layerHost == null) {
            return null;
        }
        layerHost.addChild(layer);
        requestRender();
        return layer;
    }

    public void closeTopLayer() {
        if (layerHost == null || layerHost.getChildrenList().isEmpty()) {
            return;
        }
        int topIndex = layerHost.getChildrenList().size() - 1;
        BaseComp top = layerHost.getChildrenList().get(topIndex);
        layerHost.removeChild(top);
        ExecutorService worker = modalWorkers.remove(top);
        if (worker != null) {
            worker.shutdownNow();
        }
        requestRender();
    }

    public BaseComp openModal(BaseComp modalContent) {
        if (content == null || layerHost == null) {
            return null;
        }

        BaseComp modalLayer = new BaseComp(null) {
            @Override
            public void customGraphics(Graphics g) {
                g.setColor(new Color(20, 24, 31, 120));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        modalLayer.setBounds(0, 0, content.getWidth(), content.getHeight());
        modalLayer.setDraggable(false);

        if (modalContent != null) {
            int modalX = Math.max(12, (content.getWidth() - modalContent.getWidth()) / 2);
            int modalY = Math.max(12, (content.getHeight() - modalContent.getHeight()) / 2);
            modalContent.setBounds(modalX, modalY, modalContent.getWidth(), modalContent.getHeight());
            modalLayer.addChild(modalContent);
        }

        layerHost.addChild(modalLayer);
        ExecutorService modalWorker = Executors.newSingleThreadExecutor(new NamedThreadFactory("ui-modal"));
        modalWorkers.put(modalLayer, modalWorker);
        modalWorker.submit(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        requestRender();
        return modalLayer;
    }

    public void setRoot(BaseComp root) {
        if (root == null) {
            return;
        }
        this.root = root;
        this.root.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        relayoutTree();
        requestRender();
    }

    public void requestRender() {
        dirtyManager.markAll(root);
        canvas.repaint();
    }

    public int getFps() {
        return fps;
    }

    private JPanel createCanvas() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                
                // Clear background completely transparent
                g2.setComposite(java.awt.AlphaComposite.Clear);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(java.awt.AlphaComposite.SrcOver);

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Clip rendering to rounded shape natively
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), FRAME_RADIUS, FRAME_RADIUS));

                if (fps > 0) {
                    if (root != null) {
                        root.render(g2);
                    }
                } else if (root != null) {
                    if (dirtyManager.hasDirtyRegion()) {
                        dirtyManager.render(g2, root);
                    } else {
                        root.render(g2);
                    }
                }
                g2.dispose();
            }
        };
    }

    private void startRenderLoopIfNeeded() {
        if (fps <= 0 || activeRenderTimer != null) {
            return;
        }
        int delayMs = Math.max(1, 1000 / fps);
        activeRenderTimer = new Timer(delayMs, e -> canvas.repaint());
        activeRenderTimer.start();
    }

    private BaseComp createDefaultRoot(int width, int height) {
        this.header = createDefaultHeader(width);
        this.content = new BaseComp(null);
        content.setStyleManager(
                new StyleManager(new Color(245, 245, 245), 0, width, Math.max(1, height - DEFAULT_HEADER_HEIGHT), 0,
                        DEFAULT_HEADER_HEIGHT, "absolute"));
        content.setBounds(0, DEFAULT_HEADER_HEIGHT, width, Math.max(1, height - DEFAULT_HEADER_HEIGHT));

        this.layerHost = new BaseComp(null) {
            @Override
            public boolean containsGlobalPoint(int globalX, int globalY) {
                // Keep event passthrough when no modal/layer is mounted.
                return !getChildrenList().isEmpty() && super.containsGlobalPoint(globalX, globalY);
            }
        };
        this.layerHost.setBounds(0, DEFAULT_HEADER_HEIGHT, width, Math.max(1, height - DEFAULT_HEADER_HEIGHT));

        BaseComp rootComp = new BaseComp(new BaseComp[] { header, content, layerHost }) {
            @Override
            public void customGraphics(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(244, 245, 247));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), FRAME_RADIUS, FRAME_RADIUS);
                g2.setColor(new Color(208, 210, 214));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, FRAME_RADIUS, FRAME_RADIUS);
            }
        };
        rootComp.setStyleManager(
                new StyleManager(new Color(232, 232, 232), FRAME_RADIUS, width, height, 0, 0, "block"));
        rootComp.setBounds(0, 0, width, height);
        return rootComp;
    }

    private BaseComp createDefaultHeader(int width) {
        this.header = new BaseComp(null) {
            @Override
            public void customGraphics(Graphics g) {
                g.setColor(new Color(235, 235, 235));
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(new Color(190, 193, 198));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

                g.setColor(new Color(74, 78, 84));
                int titleWidth = g.getFontMetrics().stringWidth(frame.getTitle());
                int titleX = Math.max(16, (getWidth() - titleWidth) / 2);
                g.drawString(frame.getTitle(), titleX, 24);
            }
        };
        this.header.setWindowDragHandle(true);
        this.header.setBounds(0, 0, width, DEFAULT_HEADER_HEIGHT);
        this.header.setStyleManager(
                new StyleManager(new Color(235, 235, 235), 0, width, DEFAULT_HEADER_HEIGHT, 0, 0, "absolute"));

        this.closeButton = createSystemButton(0, new Color(255, 95, 86), w -> w.dispose());
        this.minimizeButton = createSystemButton(0, new Color(255, 189, 46),
                w -> w.getNativeFrame().setState(JFrame.ICONIFIED));
        this.maximizeButton = createSystemButton(0, new Color(39, 201, 63), w -> {
            if ((w.getNativeFrame().getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                w.getNativeFrame().setExtendedState(JFrame.NORMAL);
            } else {
                w.getNativeFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });

        this.header.addChild(closeButton);
        this.header.addChild(minimizeButton);
        this.header.addChild(maximizeButton);
        repositionHeaderButtons(width);
        return this.header;
    }

    private BaseComp createSystemButton(int x, Color color, Consumer<BaseWindow> action) {
        BaseComp button = new BaseComp(null) {
            @Override
            public void customGraphics(Graphics g) {
                g.setColor(color);
                g.fillOval(0, 0, getWidth(), getHeight());
                g.setColor(new Color(0, 0, 0, 45));
                g.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        button.setBounds(x, 12, 12, 12);
        systemButtons.put(button, action);
        return button;
    }

    private void repositionHeaderButtons(int headerWidth) {
        int y = 12;
        int size = 12;
        int gap = 8;
        int rightPadding = RESIZE_BORDER + 12;

        int x = headerWidth - rightPadding - size;
        if (closeButton != null) {
            closeButton.setBounds(x, y, size, size);
            x -= (size + gap);
        }
        if (minimizeButton != null) {
            minimizeButton.setBounds(x, y, size, size);
            x -= (size + gap);
        }
        if (maximizeButton != null) {
            maximizeButton.setBounds(x, y, size, size);
        }
    }

    private void installResizeHook() {
        canvas.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (root == null) {
                    return;
                }
                root.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                header.setBounds(0, 0, canvas.getWidth(), DEFAULT_HEADER_HEIGHT);
                repositionHeaderButtons(canvas.getWidth());
                content.setBounds(0, DEFAULT_HEADER_HEIGHT, canvas.getWidth(),
                        Math.max(1, canvas.getHeight() - DEFAULT_HEADER_HEIGHT));
                layerHost.setBounds(0, DEFAULT_HEADER_HEIGHT, canvas.getWidth(),
                        Math.max(1, canvas.getHeight() - DEFAULT_HEADER_HEIGHT));
                for (BaseComp layer : layerHost.getChildren()) {
                    layer.setBounds(0, 0, layerHost.getWidth(), layerHost.getHeight());
                    BaseComp[] layerChildren = layer.getChildren();
                    if (layerChildren.length > 0) {
                        BaseComp modal = layerChildren[0];
                        int mx = Math.max(12, (layerHost.getWidth() - modal.getWidth()) / 2);
                        int my = Math.max(12, (layerHost.getHeight() - modal.getHeight()) / 2);
                        modal.setBounds(mx, my, modal.getWidth(), modal.getHeight());
                    }
                }
                applyRoundedShape(frame.getWidth(), frame.getHeight());
                relayoutTree();
                for (Runnable listener : resizeListeners) {
                    listener.run();
                }
                requestRender();
            }
        });
    }

    private void installWindowCloseHook() {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (activeRenderTimer != null) {
                    activeRenderTimer.stop();
                }
            }
        });
    }

    private void relayoutTree() {
        relayoutTree(root);
    }

    private void relayoutTree(BaseComp component) {
        if (component == null) {
            return;
        }
        component.doLayout();
        for (BaseComp child : component.getChildren()) {
            relayoutTree(child);
        }
    }

    private void wireMouseEvents() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                BaseComp hit = hitTester.findBaseComp(e.getX(), e.getY(), root);
                if (e.getButton() == MouseEvent.BUTTON1 && systemButtons.containsKey(hit)) {
                    dispatchPointerEvent(Type.POINTER_DOWN, e);
                    return;
                }
                ResizeEdge edge = detectResizeEdge(e.getX(), e.getY());
                if (edge != ResizeEdge.NONE) {
                    beginResize(edge, e);
                    return;
                }
                dispatchPointerEvent(Type.POINTER_DOWN, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (activeResizeEdge != ResizeEdge.NONE) {
                    stopResize();
                    return;
                }
                dispatchPointerEvent(Type.POINTER_UP, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (activeResizeEdge != ResizeEdge.NONE) {
                    applyResize(e);
                    return;
                }
                dispatchPointerEvent(Type.POINTER_MOVE, e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                BaseComp hit = hitTester.findBaseComp(e.getX(), e.getY(), root);
                if (systemButtons.containsKey(hit)) {
                    canvas.setCursor(java.awt.Cursor.getDefaultCursor());
                } else if (activeResizeEdge == ResizeEdge.NONE) {
                    if (hit != null && hit.getCursor() != java.awt.Cursor.DEFAULT_CURSOR) {
                        canvas.setCursor(java.awt.Cursor.getPredefinedCursor(hit.getCursor()));
                    } else if (hit != null && hit.isDraggable()) {
                        canvas.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    } else {
                        updateResizeCursor(e.getX(), e.getY());
                    }
                }
                dispatchPointerEvent(Type.POINTER_MOVE, e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                dispatchWheelEvent(e);
            }
        };
        canvas.addMouseListener(adapter);
        canvas.addMouseMotionListener(adapter);
        canvas.addMouseWheelListener(adapter);
    }

    private void dispatchPointerEvent(Type type, MouseEvent e) {
        if (root == null) {
            return;
        }

        UiEvent event = new UiEvent(type, e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), e.getButton());
        event.setWindow(this);

        BaseComp hit = hitTester.findBaseComp(e.getX(), e.getY(), root);
        BaseComp target = capturedPointer != null ? capturedPointer : hit;
        if (target == null) {
            target = root;
        }
        event.setTarget(target);

        boolean shouldRender = false;

        if (type == Type.POINTER_DOWN && e.getButton() == MouseEvent.BUTTON1 && systemButtons.containsKey(target)) {
            pressedSystemButton = target;
            return;
        }

        if (type == Type.POINTER_UP && e.getButton() == MouseEvent.BUTTON1 && pressedSystemButton != null) {
            if (pressedSystemButton == target && systemButtons.containsKey(target)) {
                Consumer<BaseWindow> action = systemButtons.get(target);
                if (action != null) {
                    action.accept(this);
                }
            }
            pressedSystemButton = null;
            shouldRender = true;
        }

        if (type == Type.POINTER_DOWN) {
            if (target.isWindowDragHandle()) {
                windowDragActive = true;
                windowDragOffsetX = e.getXOnScreen() - frame.getX();
                windowDragOffsetY = e.getYOnScreen() - frame.getY();
                shouldRender = true;
            }
            if (target.isDraggable()) {
                capturedPointer = target;
                shouldRender = true;
            }
        }

        if (windowDragActive && type == Type.POINTER_MOVE) {
            frame.setLocation(e.getXOnScreen() - windowDragOffsetX, e.getYOnScreen() - windowDragOffsetY);
            shouldRender = true;
        }

        dispatchBubble(target, event);

        if (type == Type.POINTER_UP) {
            capturedPointer = null;
            windowDragActive = false;
            shouldRender = true;
        }

        if (fps == 0 && (type != Type.POINTER_MOVE || shouldRender)) {
            requestRender();
        }
    }

    private void dispatchWheelEvent(MouseWheelEvent e) {
        if (root == null) {
            return;
        }

        UiEvent event = new UiEvent(Type.WHEEL, e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), 0,
                e.getWheelRotation());
        event.setWindow(this);

        BaseComp target = hitTester.findBaseComp(e.getX(), e.getY(), root);
        if (target == null) {
            target = root;
        }
        event.setTarget(target);
        dispatchBubble(target, event);
        if (fps == 0) {
            requestRender();
        }
    }

    private ResizeEdge detectResizeEdge(int x, int y) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        boolean north = y <= RESIZE_BORDER;
        boolean south = y >= height - RESIZE_BORDER;
        boolean west = x <= RESIZE_BORDER;
        boolean east = x >= width - RESIZE_BORDER;

        if (north && west) {
            return ResizeEdge.NORTH_WEST;
        }
        if (north && east) {
            return ResizeEdge.NORTH_EAST;
        }
        if (south && west) {
            return ResizeEdge.SOUTH_WEST;
        }
        if (south && east) {
            return ResizeEdge.SOUTH_EAST;
        }
        if (north) {
            return ResizeEdge.NORTH;
        }
        if (south) {
            return ResizeEdge.SOUTH;
        }
        if (west) {
            return ResizeEdge.WEST;
        }
        if (east) {
            return ResizeEdge.EAST;
        }
        return ResizeEdge.NONE;
    }

    private void updateResizeCursor(int x, int y) {
        ResizeEdge edge = detectResizeEdge(x, y);
        int cursor = switch (edge) {
            case NORTH, SOUTH -> java.awt.Cursor.N_RESIZE_CURSOR;
            case EAST, WEST -> java.awt.Cursor.E_RESIZE_CURSOR;
            case NORTH_EAST, SOUTH_WEST -> java.awt.Cursor.NE_RESIZE_CURSOR;
            case NORTH_WEST, SOUTH_EAST -> java.awt.Cursor.NW_RESIZE_CURSOR;
            default -> java.awt.Cursor.DEFAULT_CURSOR;
        };
        canvas.setCursor(java.awt.Cursor.getPredefinedCursor(cursor));
    }

    private void beginResize(ResizeEdge edge, MouseEvent e) {
        activeResizeEdge = edge;
        resizeStartMouse = e.getLocationOnScreen();
        resizeStartBounds = frame.getBounds();
    }

    private void applyResize(MouseEvent e) {
        if (resizeStartMouse == null || resizeStartBounds == null) {
            return;
        }

        int dx = e.getXOnScreen() - resizeStartMouse.x;
        int dy = e.getYOnScreen() - resizeStartMouse.y;

        int newX = resizeStartBounds.x;
        int newY = resizeStartBounds.y;
        int newW = resizeStartBounds.width;
        int newH = resizeStartBounds.height;

        switch (activeResizeEdge) {
            case EAST -> newW = resizeStartBounds.width + dx;
            case WEST -> {
                newX = resizeStartBounds.x + dx;
                newW = resizeStartBounds.width - dx;
            }
            case SOUTH -> newH = resizeStartBounds.height + dy;
            case NORTH -> {
                newY = resizeStartBounds.y + dy;
                newH = resizeStartBounds.height - dy;
            }
            case NORTH_EAST -> {
                newY = resizeStartBounds.y + dy;
                newH = resizeStartBounds.height - dy;
                newW = resizeStartBounds.width + dx;
            }
            case NORTH_WEST -> {
                newX = resizeStartBounds.x + dx;
                newW = resizeStartBounds.width - dx;
                newY = resizeStartBounds.y + dy;
                newH = resizeStartBounds.height - dy;
            }
            case SOUTH_EAST -> {
                newW = resizeStartBounds.width + dx;
                newH = resizeStartBounds.height + dy;
            }
            case SOUTH_WEST -> {
                newX = resizeStartBounds.x + dx;
                newW = resizeStartBounds.width - dx;
                newH = resizeStartBounds.height + dy;
            }
            default -> {
            }
        }

        if (newW < MIN_WIDTH) {
            if (activeResizeEdge == ResizeEdge.WEST || activeResizeEdge == ResizeEdge.NORTH_WEST
                    || activeResizeEdge == ResizeEdge.SOUTH_WEST) {
                newX -= (MIN_WIDTH - newW);
            }
            newW = MIN_WIDTH;
        }
        if (newH < MIN_HEIGHT) {
            if (activeResizeEdge == ResizeEdge.NORTH || activeResizeEdge == ResizeEdge.NORTH_WEST
                    || activeResizeEdge == ResizeEdge.NORTH_EAST) {
                newY -= (MIN_HEIGHT - newH);
            }
            newH = MIN_HEIGHT;
        }

        frame.setBounds(newX, newY, newW, newH);
        applyRoundedShape(newW, newH);
        requestRender();
    }

    private void stopResize() {
        activeResizeEdge = ResizeEdge.NONE;
        resizeStartMouse = null;
        resizeStartBounds = null;
        canvas.setCursor(java.awt.Cursor.getDefaultCursor());
    }

    private void applyRoundedShape(int width, int height) {
        // Native setShape is extremely slow on Linux/X11 during live resize.
        // We bypass it and rely on the window being transparent. (AWT Shape causes lags)
        // If really needed, it can be deferred on drop.
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private int counter;

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
            this.counter = 1;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter++);
            t.setDaemon(true);
            return t;
        }
    }

    private void dispatchBubble(BaseComp target, UiEvent event) {
        BaseComp current = target;
        while (current != null) {
            if (current.getEventManager() != null) {
                current.getEventManager().trigger(event, current);
            }
            current = current.getParent();
        }
    }

}
