package layout;

import main.BaseComp;

public class BlockLayoutEngine extends BaseLayoutEngine {
    public BlockLayoutEngine() {
    }

    @Override
    public void doLayout(BaseComp c) {
        BaseComp[] children = c.getChildren();
        int currentY = 0;
        for (BaseComp child : children) {
            child.setBounds(0, currentY, child.getWidth(), child.getHeight());
            currentY += child.getHeight();
        }
    }
}
