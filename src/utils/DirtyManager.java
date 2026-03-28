package utils;

import java.awt.*;
import main.BaseComp;

public class DirtyManager {

    private Rectangle dirtyRegion = null;

    public void addDirtyRegion(int x, int y, int w, int h) {

        Rectangle r = new Rectangle(x, y, w, h);

        if (dirtyRegion == null) {

            dirtyRegion = r;

        } else {

            dirtyRegion = dirtyRegion.union(r);

        }

    }

    public void render(Graphics2D g, BaseComp root) {

        if (dirtyRegion == null)
            return;

        g.setClip(dirtyRegion);

        root.paint(g);

        g.setClip(null);

        dirtyRegion = null;

    }

    public boolean hasDirtyRegion() {
        return dirtyRegion != null;
    }

    public void markAll(BaseComp root) {
        if (root == null) {
            return;
        }
        addDirtyRegion(0, 0, Math.max(1, root.getWidth()), Math.max(1, root.getHeight()));
    }

}