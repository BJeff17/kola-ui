package layout;

import main.BaseComp;

public class GridLayoutEngine extends BaseLayoutEngine {
    public GridLayoutEngine() {

    }
    @Override
    public void doLayout(BaseComp c) {
        BaseComp[] children = c.getChildren();
        int numChildren = children.length;
        boolean isRowFirst = c.getStyleManager().isRowFirst();
        int gap = c.getStyleManager().getGap();
        int numRows = 0;
        int numCols = 0;
        int colWidth = c.getWidth() / numCols - gap;
        int rowHeight = c.getHeight() / numRows - gap;

        if(isRowFirst){
            numRows = c.getStyleManager().getNumRows();
            numCols = (int) Math.ceil((double) numChildren / numRows);
            for (int i = 0; i < numChildren; i++) {
                int row = i / numCols;
                int col = i % numCols;
                children[i].setBounds(col * (colWidth + gap), row * (rowHeight + gap), children[i].getWidth(), children[i].getHeight());
            }
            
        } else {
            numCols = c.getStyleManager().getNumCols();
            numRows = (int) Math.ceil((double) numChildren / numCols);
            for (int i = 0; i < numChildren; i++) {
                int col = i / numRows;
                int row = i % numRows;
                children[i].setBounds(col * (colWidth + gap), row * (rowHeight + gap), children[i].getWidth(), children[i].getHeight());
            }
        }
    }
}