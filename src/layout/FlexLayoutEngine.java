
package layout;
import main.BaseComp;
import style.StyleManager;


public class FlexLayoutEngine extends BaseLayoutEngine {
    public FlexLayoutEngine() {
    }

    @Override
    public void doLayout(BaseComp c) {
        BaseComp[] children = c.getChildren();
        StyleManager s = c.getStyleManager();
        boolean isColumnFlex = s.isColumnFlex();
        int gap = s.getGap();
        int current = 0;
        for (BaseComp child : children) {
            if (isColumnFlex) {
                child.setBounds(0, current, child.getWidth(), child.getHeight());
                current += child.getHeight() + gap;
            } else {
                child.setBounds(current, 0, child.getWidth(), child.getHeight());
                current += child.getWidth() + gap;
            }
        }
        
    }
}
