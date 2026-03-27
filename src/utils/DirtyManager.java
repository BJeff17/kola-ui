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

}