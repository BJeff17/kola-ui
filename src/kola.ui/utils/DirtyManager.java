package utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import main.BaseComp;

public class DirtyManager {
    private static final int MAX_REGIONS = 24;
    private static final int MERGE_PADDING = 2;
    private final List<Rectangle> dirtyRegions = new ArrayList<>();
    private boolean fullRedrawRequested;

    public synchronized void addDirtyRegion(int x, int y, int w, int h) {
        addDirtyRegion(new Rectangle(x, y, Math.max(1, w), Math.max(1, h)));
    }

    public synchronized void addDirtyRegion(Rectangle rect) {
        if (rect == null || rect.width <= 0 || rect.height <= 0) {
            return;
        }
        if (fullRedrawRequested) {
            return;
        }

        Rectangle incoming = new Rectangle(rect);
        boolean merged;
        do {
            merged = false;
            for (int i = 0; i < dirtyRegions.size(); i++) {
                Rectangle current = dirtyRegions.get(i);
                if (intersectsOrNear(current, incoming)) {
                    incoming = current.union(incoming);
                    dirtyRegions.remove(i);
                    merged = true;
                    break;
                }
            }
        } while (merged);

        dirtyRegions.add(incoming);
        if (dirtyRegions.size() > MAX_REGIONS) {
            requestFullRedraw();
        }
    }

    public synchronized void requestFullRedraw() {
        fullRedrawRequested = true;
        dirtyRegions.clear();
    }

    public synchronized boolean hasDirtyRegion() {
        return fullRedrawRequested || !dirtyRegions.isEmpty();
    }

    public synchronized boolean isFullRedrawRequested() {
        return fullRedrawRequested;
    }

    public synchronized List<Rectangle> getDirtyRegions() {
        return new ArrayList<>(dirtyRegions);
    }

    public synchronized int getDirtyRegionCount() {
        return fullRedrawRequested ? 1 : dirtyRegions.size();
    }

    public synchronized int getEstimatedDirtyArea() {
        if (fullRedrawRequested) {
            return Integer.MAX_VALUE;
        }
        int area = 0;
        for (Rectangle r : dirtyRegions) {
            area += Math.max(0, r.width) * Math.max(0, r.height);
        }
        return area;
    }

    public synchronized boolean shouldFallbackToFullRedraw(int viewportWidth, int viewportHeight) {
        if (fullRedrawRequested) {
            return true;
        }
        int viewportArea = Math.max(1, viewportWidth * viewportHeight);
        return getEstimatedDirtyArea() > (int) (viewportArea * 0.55);
    }

    public synchronized Rectangle getDirtyRegion() {
        if (fullRedrawRequested) {
            return null;
        }
        if (dirtyRegions.isEmpty()) {
            return null;
        }
        Rectangle union = new Rectangle(dirtyRegions.get(0));
        for (int i = 1; i < dirtyRegions.size(); i++) {
            union = union.union(dirtyRegions.get(i));
        }
        return union;
    }

    public synchronized void clear() {
        dirtyRegions.clear();
        fullRedrawRequested = false;
    }

    public void markAll(BaseComp root) {
        if (root == null) {
            return;
        }
        requestFullRedraw();
    }

    private boolean intersectsOrNear(Rectangle a, Rectangle b) {
        Rectangle expanded = new Rectangle(a.x - MERGE_PADDING, a.y - MERGE_PADDING, a.width + (MERGE_PADDING * 2),
                a.height + (MERGE_PADDING * 2));
        return expanded.intersects(b) || expanded.contains(b) || b.contains(expanded);
    }

}