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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private static final int RESIZE_BORDER = 8;
    private static final int MIN_WIDTH = 520;
    private static final int MIN_HEIGHT = 380;
    private static final Color OPAQUE_WINDOW_BG = new Color(244, 245, 247);

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
    private final boolean transparentWindow;

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

    private BaseComp closeButton;
    private BaseComp minimizeButton;
    private BaseComp maximizeButton;
    private BaseComp focusedComponent;

    private Timer activeRenderTimer;
    private boolean repaintScheduled;
    private boolean debugOverlayEnabled;
    private boolean debugEventOverlayEnabled;
    private long lastFrameEndNanos;
    private double smoothedFrameMs;
    private int lastDirtyCount;
    private int lastDirtyArea;
    private boolean lastFrameFullRedraw;
    private final LinkedList<String> debugEventLines;
    private static final int MAX_DEBUG_EVENT_LINES = 12;

    public BaseWindow(String title, int width, int height, int fps) {
        super(null);
        this.fps = Math.max(0, fps);
        this.transparentWindow = resolveTransparentWindowSetting();
        this.hitTester = new HitTester();
        this.dirtyManager = new DirtyManager();
        this.frame = new JFrame(title);
        this.canvas = createCanvas();
        this.activeResizeEdge = ResizeEdge.NONE;
        this.systemButtons = new HashMap<>();
        this.resizeListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.childWindows = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.repaintScheduled = false;
        this.debugOverlayEnabled = true;
        this.debugEventOverlayEnabled = false;
        this.lastFrameEndNanos = System.nanoTime();
        this.smoothedFrameMs = 0.0;
        this.lastDirtyCount = 0;
        this.lastDirtyArea = 0;
        this.lastFrameFullRedraw = true;
        this.focusedComponent = null;
        this.debugEventLines = new LinkedList<>();

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setBackground(transparentWindow ? new Color(0, 0, 0, 0) : OPAQUE_WINDOW_BG);
        frame.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        frame.setSize(width, height);
        frame.setContentPane(canvas);
        canvas.setOpaque(!transparentWindow);
        canvas.setBackground(transparentWindow ? new Color(0, 0, 0, 0) : OPAQUE_WINDOW_BG);
        canvas.setBorder(BorderFactory.createEmptyBorder());
        frame.setLocationRelativeTo(null);
        applyRoundedShape(width, height);

        this.root = createDefaultRoot(width, height);
        this.root.setOwnerWindow(this);
        this.dirtyManager.requestFullRedraw();
        installResizeHook();
        installWindowCloseHook();
        wireMouseEvents();
    }

    @Override
    public void paint(Graphics g) {
        if (root != null) {
            root.paint(g);// cette ligne est le seul point de friction entre mon système de rendu et Swing, je dois m'assurer que tout le dessin se fasse à ce moment exact.
        }
    }

    public void show() {
        frame.setVisible(true);
        startRenderLoopIfNeeded();
        requestRender();
    }

    public void dispose() {
        for (BaseWindow childWindow : childWindows) {
            if (childWindow != null) {
                childWindow.dispose();
            }
        }
        childWindows.clear();
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
        childWindow.show();
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
        modalLayer.getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (event.getTarget() == modalLayer) {
                closeTopLayer();
                event.stopPropagation();
            }
        });

        if (modalContent != null) {
            int modalX = Math.max(12, (content.getWidth() - modalContent.getWidth()) / 2);
            int modalY = Math.max(12, (content.getHeight() - modalContent.getHeight()) / 2);
            modalContent.setBounds(modalX, modalY, modalContent.getWidth(), modalContent.getHeight());
            modalLayer.addChild(modalContent);
        }

        layerHost.addChild(modalLayer);
        requestRender();
        return modalLayer;
    }

    public void setRoot(BaseComp root) {
        if (root == null) {
            return;
        }
        this.root = root;
        this.root.setOwnerWindow(this);
        this.root.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        relayoutTree();
        requestRender();
    }

    public void requestRender() {
        dirtyManager.requestFullRedraw();
        requestRenderIfNeeded();
    }

    public void requestRenderIfNeeded() {
        if (SwingUtilities.isEventDispatchThread()) {
            scheduleRepaint();
            return;
        }
        SwingUtilities.invokeLater(this::scheduleRepaint);
    }

    public void invalidateAll() {
        dirtyManager.requestFullRedraw();
    }

    public void invalidateComponent(BaseComp component) {
        if (component == null) {
            return;
        }
        invalidateRect(component.getGlobalBounds());
    }

    public void invalidateRect(Rectangle rect) {
        if (rect == null || rect.width <= 0 || rect.height <= 0) {
            return;
        }
        dirtyManager.addDirtyRegion(rect);
    }

    private void scheduleRepaint() {
        if (repaintScheduled) {
            return;
        }
        repaintScheduled = true;
        SwingUtilities.invokeLater(() -> {
            repaintScheduled = false;
            canvas.repaint();
        });
    }

    public int getFps() {
        return fps;
    }

    public void capturePointer(BaseComp component) {
        this.capturedPointer = component;
    }

    public void releasePointer(BaseComp component) {
        if (component == null || capturedPointer == component) {
            capturedPointer = null;
        }
    }

    private java.awt.image.BufferedImage backBuffer;

    private JPanel createCanvas() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }

                boolean fullRedraw = false;
                if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
                    int imageType = transparentWindow ? java.awt.image.BufferedImage.TYPE_INT_ARGB
                            : java.awt.image.BufferedImage.TYPE_INT_RGB;
                    backBuffer = new java.awt.image.BufferedImage(w, h, imageType);
                    fullRedraw = true;
                }

                boolean hasDirty = dirtyManager.hasDirtyRegion();
                boolean shouldRender = fullRedraw || fps > 0 || hasDirty;
                if (shouldRender) {
                    Graphics2D b2g = backBuffer.createGraphics();
                    b2g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    java.awt.Shape windowShape = transparentWindow
                            ? new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, FRAME_RADIUS, FRAME_RADIUS)
                            : new Rectangle(0, 0, w, h);

                    boolean forceFull = fullRedraw || fps > 0 || dirtyManager.shouldFallbackToFullRedraw(w, h);
                    lastFrameFullRedraw = forceFull;
                    lastDirtyCount = dirtyManager.getDirtyRegionCount();
                    lastDirtyArea = dirtyManager.getEstimatedDirtyArea();

                    if (forceFull) {
                        b2g.setClip(windowShape);
                        clearBufferRegion(b2g, 0, 0, w, h);
                        if (root != null) {
                            root.render(b2g);
                        }
                    } else {
                        List<Rectangle> dirtyRects = dirtyManager.getDirtyRegions();
                        for (Rectangle dirty : dirtyRects) {
                            Rectangle clipped = dirty.intersection(new Rectangle(0, 0, w, h));
                            if (clipped.isEmpty()) {
                                continue;
                            }
                            b2g.setClip(clipped);
                            clearBufferRegion(b2g, clipped.x, clipped.y, clipped.width, clipped.height);
                            if (root != null) {
                                root.render(b2g);
                            }
                        }
                    }

                    dirtyManager.clear();
                    b2g.dispose();
                }

                // Draw the back buffer onto the panel
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(java.awt.AlphaComposite.SrcOver);
                g2d.drawImage(backBuffer, 0, 0, null);
                if (debugOverlayEnabled) {
                    drawDebugOverlay(g2d, w, h);
                }
                if (debugEventOverlayEnabled) {
                    drawEventDebugOverlay(g2d, w, h);
                }
                g2d.dispose();

                if (transparentWindow) {
                    java.awt.Toolkit.getDefaultToolkit().sync();
                }

                long now = System.nanoTime();
                double frameMs = (now - lastFrameEndNanos) / 1_000_000.0;
                lastFrameEndNanos = now;
                if (smoothedFrameMs == 0.0) {
                    smoothedFrameMs = frameMs;
                } else {
                    smoothedFrameMs = (smoothedFrameMs * 0.9) + (frameMs * 0.1);
                }
            }
        };
        p.setOpaque(!transparentWindow);
        p.setFocusable(true);
        p.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3) {
                    debugOverlayEnabled = !debugOverlayEnabled;
                    requestRender();
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_F4) {
                    debugEventOverlayEnabled = !debugEventOverlayEnabled;
                    recordDebugEvent("DebugEvents=" + (debugEventOverlayEnabled ? "ON" : "OFF"));
                    requestRender();
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && layerHost != null
                        && !layerHost.getChildrenList().isEmpty()) {
                    closeTopLayer();
                    e.consume();
                    return;
                }

                if (focusedComponent != null) {
                    boolean consumed = focusedComponent.onKeyPressed(e);
                    if (consumed) {
                        e.consume();
                        requestRenderIfNeeded();
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (focusedComponent != null) {
                    boolean consumed = focusedComponent.onKeyTyped(e);
                    if (consumed) {
                        e.consume();
                        requestRenderIfNeeded();
                    }
                }
            }
        });
        return p;
    }

    private void clearBufferRegion(Graphics2D g2, int x, int y, int w, int h) {
        if (transparentWindow) {
            g2.setComposite(java.awt.AlphaComposite.Clear);
            g2.fillRect(x, y, w, h);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
            return;
        }
        g2.setComposite(java.awt.AlphaComposite.SrcOver);
        g2.setColor(OPAQUE_WINDOW_BG);
        g2.fillRect(x, y, w, h);
    }

    private boolean resolveTransparentWindowSetting() {
        String prop = System.getProperty("ui.window.transparent", "").trim().toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(prop) || "1".equals(prop) || "yes".equals(prop)) {
            return true;
        }
        if ("false".equals(prop) || "0".equals(prop) || "no".equals(prop)) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return !os.contains("linux");
    }

    private void drawDebugOverlay(Graphics2D g2, int w, int h) {
        String fpsText;
        if (smoothedFrameMs <= 0.0) {
            fpsText = "FPS: --";
        } else {
            int estimatedFps = (int) Math.max(1, Math.round(1000.0 / smoothedFrameMs));
            fpsText = "FPS: " + estimatedFps + " (" + String.format(java.util.Locale.US, "%.2f", smoothedFrameMs)
                    + " ms)";
        }
        String modeText = "Render: " + (lastFrameFullRedraw ? "FULL" : "PARTIAL");
        String dirtyText = "Dirty: " + lastDirtyCount + " rect(s), area="
                + (lastDirtyArea == Integer.MAX_VALUE ? "FULL" : lastDirtyArea);
        String hintText = "F3: Metrics | F4: EventDebug";

        int boxW = Math.min(360, w - 20);
        int boxH = 72;
        int x = 10;
        int y = Math.max(10, h - boxH - 10);

        g2.setColor(new Color(17, 24, 39, 190));
        g2.fillRoundRect(x, y, boxW, boxH, 12, 12);
        g2.setColor(new Color(148, 163, 184, 220));
        g2.drawRoundRect(x, y, boxW, boxH, 12, 12);
        g2.setColor(new Color(241, 245, 249));
        g2.drawString(fpsText, x + 10, y + 20);
        g2.drawString(modeText, x + 10, y + 36);
        g2.drawString(dirtyText, x + 10, y + 52);
        g2.setColor(new Color(148, 163, 184));
        g2.drawString(hintText, x + 10, y + 67);
    }

    private void drawEventDebugOverlay(Graphics2D g2, int w, int h) {
        java.util.List<String> lines;
        synchronized (debugEventLines) {
            lines = new java.util.ArrayList<>(debugEventLines);
        }

        int lineHeight = 15;
        int lineCount = Math.max(1, Math.min(MAX_DEBUG_EVENT_LINES, lines.size()));
        int boxH = 34 + (lineCount * lineHeight);
        int boxW = Math.min(580, w - 20);
        int x = Math.max(10, w - boxW - 10);
        int y = 10;

        g2.setColor(new Color(11, 18, 32, 205));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(new Color(79, 70, 229, 210));
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(new Color(226, 232, 240));
        g2.drawString("Event Debug (F4)", x + 10, y + 18);

        int cy = y + 34;
        if (lines.isEmpty()) {
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("No events captured yet.", x + 10, cy);
            return;
        }

        for (String line : lines) {
            g2.setColor(new Color(196, 205, 219));
            g2.drawString(line, x + 10, cy);
            cy += lineHeight;
            if (cy > y + boxH - 6) {
                break;
            }
        }
    }

    private void recordDebugEvent(String msg) {
        if (!debugEventOverlayEnabled) {
            return;
        }
        String line = String.format(java.util.Locale.US, "%tT.%<tL %s", System.currentTimeMillis(), msg);
        synchronized (debugEventLines) {
            debugEventLines.addLast(line);
            while (debugEventLines.size() > MAX_DEBUG_EVENT_LINES) {
                debugEventLines.removeFirst();
            }
        }
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
                // Root must keep explicit child bounds (header/content/layerHost) for proper
                // z-layering.
                new StyleManager(new Color(232, 232, 232), FRAME_RADIUS, width, height, 0, 0, "absolute"));
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

            @Override
            public void windowDeactivated(WindowEvent e) {
                if (layerHost != null && !layerHost.getChildrenList().isEmpty()) {
                    closeTopLayer();
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
                canvas.requestFocusInWindow();
                if (dispatchLegacyWheelButton(e)) {
                    return;
                }
                BaseComp hit = hitTester.findBaseComp(e.getX(), e.getY(), root);
                setFocusedComponent(resolveFocusableTarget(hit));
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
                    changeCursor(java.awt.Cursor.DEFAULT_CURSOR);
                } else if (activeResizeEdge == ResizeEdge.NONE) {
                    if (hit != null && hit.getCursor() != java.awt.Cursor.DEFAULT_CURSOR) {
                        changeCursor(hit.getCursor());
                    } else if (hit != null && hit.isDraggable()) {
                        changeCursor(java.awt.Cursor.HAND_CURSOR);
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

        recordDebugEvent("PTR " + type + " btn=" + e.getButton() + " x=" + e.getX() + " y=" + e.getY());

        UiEvent event = new UiEvent(type, e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), e.getButton(),
                0.0, e.isShiftDown(), e.getClickCount());
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
            BaseComp dragHandle = findFirstAncestor(target, BaseComp::isWindowDragHandle);
            if (dragHandle != null) {
                windowDragActive = true;
                windowDragOffsetX = e.getXOnScreen() - frame.getX();
                windowDragOffsetY = e.getYOnScreen() - frame.getY();
                shouldRender = true;
            }
            BaseComp dragSource = findFirstAncestor(target, BaseComp::isDraggable);
            if (dragSource != null) {
                capturedPointer = dragSource;
                shouldRender = true;
            }
        }

        if (windowDragActive && type == Type.POINTER_MOVE) {
            int targetX = e.getXOnScreen() - windowDragOffsetX;
            int targetY = e.getYOnScreen() - windowDragOffsetY;
            frame.setLocation(targetX, targetY);
            shouldRender = true;
        }

        dispatchBubble(target, event);

        if (type == Type.POINTER_UP) {
            capturedPointer = null;
            windowDragActive = false;
            shouldRender = true;
        }

        if (fps == 0 && (type != Type.POINTER_MOVE || shouldRender)) {
            requestRenderIfNeeded();
        }
    }

    private void dispatchWheelEvent(MouseWheelEvent e) {
        if (root == null) {
            return;
        }

        double rotation = e.getPreciseWheelRotation();
        if (rotation == 0.0) {
            rotation = e.getWheelRotation();
        }
        if (rotation == 0.0 && e.getUnitsToScroll() != 0) {
            rotation = e.getUnitsToScroll() / 3.0;
        }

        recordDebugEvent(String.format(java.util.Locale.US,
                "WHL rot=%.3f precise=%.3f wheel=%d units=%d shift=%s type=%d",
                rotation, e.getPreciseWheelRotation(), e.getWheelRotation(), e.getUnitsToScroll(),
                e.isShiftDown(), e.getScrollType()));

        UiEvent event = new UiEvent(Type.WHEEL, e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), 0,
                rotation, e.isShiftDown());
        event.setWindow(this);

        BaseComp target = hitTester.findBaseComp(e.getX(), e.getY(), root);
        if (target == null) {
            target = root;
        }
        event.setTarget(target);
        dispatchBubble(target, event);
        if (fps == 0) {
            requestRenderIfNeeded();
        }
    }

    private boolean dispatchLegacyWheelButton(MouseEvent e) {
        int btn = e.getButton();
        if (btn < 4) {
            return false;
        }

        // X11/older stacks can emit wheel/trackpad gestures as mouse buttons.
        double rotation;
        boolean shiftLikeHorizontal = false;
        switch (btn) {
            case 4 -> rotation = -1.0; // up
            case 5 -> rotation = 1.0; // down
            case 6 -> {
                rotation = -1.0; // left
                shiftLikeHorizontal = true;
            }
            case 7 -> {
                rotation = 1.0; // right
                shiftLikeHorizontal = true;
            }
            default -> {
                return false;
            }
        }

        UiEvent event = new UiEvent(Type.WHEEL, e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), 0, rotation,
                shiftLikeHorizontal || e.isShiftDown());
        recordDebugEvent("LEGACY-WHL btn=" + btn + " rot=" + rotation + " shiftLike=" + shiftLikeHorizontal);
        event.setWindow(this);
        BaseComp target = hitTester.findBaseComp(e.getX(), e.getY(), root);
        if (target == null) {
            target = root;
        }
        event.setTarget(target);
        dispatchBubble(target, event);
        if (fps == 0) {
            requestRenderIfNeeded();
        }
        return true;
    }

    private BaseComp resolveFocusableTarget(BaseComp target) {
        BaseComp current = target;
        while (current != null) {
            if (current.isFocusable()) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private BaseComp findFirstAncestor(BaseComp from, java.util.function.Predicate<BaseComp> predicate) {
        BaseComp current = from;
        while (current != null) {
            if (predicate.test(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private void setFocusedComponent(BaseComp next) {
        if (focusedComponent == next) {
            return;
        }
        if (focusedComponent != null) {
            focusedComponent.setFocused(false);
        }
        focusedComponent = next;
        if (focusedComponent != null) {
            focusedComponent.setFocused(true);
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

    private int currentCursorId = java.awt.Cursor.DEFAULT_CURSOR;

    private void changeCursor(int cursorId) {
        if (this.currentCursorId != cursorId) {
            this.currentCursorId = cursorId;
            canvas.setCursor(java.awt.Cursor.getPredefinedCursor(cursorId));
        }
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
        changeCursor(cursor);
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
        changeCursor(java.awt.Cursor.DEFAULT_CURSOR);
    }

    private void applyRoundedShape(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (!transparentWindow) {
            // No native shape work in opaque mode.
        }
        // Native setShape is extremely slow on Linux/X11 during live resize.
        // We bypass it and rely on the window being transparent. (AWT Shape causes
        // lags)
        // If really needed, it can be deferred on drop.
    }

    private void dispatchBubble(BaseComp target, UiEvent event) {
        BaseComp current = target;
        while (current != null) {
            if (current.getEventManager() != null) {
                current.getEventManager().trigger(event, current);
                if (event.isPropagationStopped()) {
                    break;
                }
            }
            current = current.getParent();
        }
    }

}
