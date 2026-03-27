package style;

import java.awt.Color;
import java.awt.Graphics;
import layout.*;
import main.BaseComp;

public class StyleManager {
    private final BaseLayoutEngine layoutEngine;
    private Color color = Color.WHITE;
    private int borderRadius = 0;
    private int width = 0;
    private int height = 0;
    private int x = 0;
    private int y = 0;
    private boolean isColumnFlex = false;
    private boolean isRowFirst = false;
    private int numRows = 0;
    private int numCols = 0;
    private int gap = 0;

    public StyleManager(Color color, int borderRadius, int width, int height, int x, int y, String display) {
        this.color = color;
        this.borderRadius = borderRadius;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.layoutEngine = createLayoutEngine(display);

    }
    public void setFlexProps(boolean isColumnFlex, int gap) {
        this.isColumnFlex = isColumnFlex;
        this.gap = gap;
    }
    public void setBlockProps(int gap) {
        this.gap = gap;
    }
    public void setGridProps(boolean isRowFirst, int numRows, int numCols) {
        this.isRowFirst = isRowFirst;
        this.numRows = numRows;
        this.numCols = numCols;
    }
    
    public StyleManager(StyleManager parentStyle) {
        this.color = parentStyle.color;
        this.layoutEngine = createLayoutEngine("block");

    }

    private static BaseLayoutEngine createLayoutEngine(String display) {
        return switch (display) {
            case "flex" -> new FlexLayoutEngine();
            case "block" -> new BlockLayoutEngine();
            default -> new BaseLayoutEngine();
        };
    }
    
    public void apply(Graphics g) {
        g.setColor(color);
        if (borderRadius != 0) {
            g.fillRoundRect(x, y, width, height, borderRadius, borderRadius);
        }
    }

    public void doLayout(BaseComp c) {
        layoutEngine.doLayout(c);
    }

    public Graphics createChildGraphics(BaseComp parent, BaseComp child, Graphics g) {
        Graphics child_g = g.create();
        return child_g;
    }


    public void manageInheritStylePropagation(BaseComp parent){
        BaseComp[] children = parent.getChildren();
        for (BaseComp child : children) {
            useAsDefaultStyleIfNotSet(child, this);
        }
    }

    public static void useAsDefaultStyleIfNotSet(BaseComp child, StyleManager parentStyle) {
        if (child.getStyleManager() == null) {
            child.setStyleManager(new StyleManager(parentStyle));
            return;
        }
        StyleManager childStyle = child.getStyleManager();


        if (childStyle.color == null) {
            childStyle.color = parentStyle.color;
        }
        
    }


    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;  
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public boolean isColumnFlex() {
        return isColumnFlex;
    }
    public int getGap() {
        return gap;
    }
    public boolean isRowFirst() {
        return isRowFirst;
    }
    public int getNumRows() {
        return numRows;
    }
    public int getNumCols() {
        return numCols;
    }
}
