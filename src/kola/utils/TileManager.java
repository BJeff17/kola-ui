package kola.utils;

import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JFrame;

public class TileManager {
    private final JFrame frame;
    private Rectangle restoreBounds;
    private boolean maximized;

    public TileManager(JFrame frame) {
        this.frame = frame;
        this.maximized = false;
    }

    public void toggleMaximizeRestore() {
        if (maximized) {
            restore();
            return;
        }
        maximize();
    }

    public void maximize() {
        saveRestoreBounds();
        Rectangle workArea = getWorkArea();
        frame.setBounds(workArea);
        maximized = true;
    }

    public void restore() {
        if (restoreBounds != null) {
            frame.setBounds(restoreBounds);
        }
        maximized = false;
    }

    public void tileLeft() {
        saveRestoreBounds();
        Rectangle workArea = getWorkArea();
        frame.setBounds(workArea.x, workArea.y, workArea.width / 2, workArea.height);
        maximized = false;
    }

    public void tileRight() {
        saveRestoreBounds();
        Rectangle workArea = getWorkArea();
        int halfWidth = workArea.width / 2;
        frame.setBounds(workArea.x + halfWidth, workArea.y, halfWidth, workArea.height);
        maximized = false;
    }

    private void saveRestoreBounds() {
        if (!maximized) {
            restoreBounds = frame.getBounds();
        }
    }

    private Rectangle getWorkArea() {
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = frame.getToolkit().getScreenInsets(gc);
        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom);
    }
}
