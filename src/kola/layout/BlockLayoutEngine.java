package kola.layout;

import kola.main.BaseComp;

public class BlockLayoutEngine extends BaseLayoutEngine {
    public BlockLayoutEngine() {
    }

    @Override
    public void doLayout(BaseComp c) {
        BaseComp[] children = c.getChildren();
        if (children == null) {
            return;
        }
        int currentY = 0;
        for (BaseComp child : children) {
            if (child == null) {
                continue;
            }
            child.setBounds(0, currentY, child.getWidth(), child.getHeight());
            currentY += child.getHeight();
        }
    }
}
