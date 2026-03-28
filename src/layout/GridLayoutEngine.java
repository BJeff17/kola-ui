package layout;

import main.BaseComp;

public class GridLayoutEngine extends BaseLayoutEngine {
    public GridLayoutEngine() {

    }

    @Override
    public void doLayout(BaseComp c) {
        BaseComp[] children = c.getChildren();
        if (children == null || children.length == 0 || c.getStyleManager() == null) {
            return;
        }
        int numChildren = children.length;
        boolean isRowFirst = c.getStyleManager().isRowFirst();
        int gap = c.getStyleManager().getGap();

        if (isRowFirst) {
            int numRows = Math.max(1, c.getStyleManager().getNumRows());
            int numCols = (int) Math.ceil((double) numChildren / numRows);
            int colWidth = Math.max(1, c.getWidth() / Math.max(1, numCols) - gap);
            int rowHeight = Math.max(1, c.getHeight() / Math.max(1, numRows) - gap);
            for (int i = 0; i < numChildren; i++) {
                int row = i / numCols;
                int col = i % numCols;
                children[i].setBounds(col * (colWidth + gap), row * (rowHeight + gap), colWidth, rowHeight);
            }
        } else {
            int numCols = Math.max(1, c.getStyleManager().getNumCols());
            int numRows = (int) Math.ceil((double) numChildren / numCols);
            int colWidth = Math.max(1, c.getWidth() / Math.max(1, numCols) - gap);
            int rowHeight = Math.max(1, c.getHeight() / Math.max(1, numRows) - gap);
            for (int i = 0; i < numChildren; i++) {
                int col = i / numRows;
                int row = i % numRows;
                children[i].setBounds(col * (colWidth + gap), row * (rowHeight + gap), colWidth, rowHeight);
            }
        }
    }
}