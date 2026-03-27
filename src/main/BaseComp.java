package main;

import event.EventManager;
import java.awt.Component;
import java.awt.Graphics;
import style.StyleManager;

public class BaseComp extends Component {
    private StyleManager styleManager = null;
    private EventManager eventManager = null;
    private final BaseComp[] children;

    public BaseComp(BaseComp[] children) {
        this.children = children;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (styleManager != null) {
            styleManager.setBounds(x, y, width, height);
        }
    }   

    @Override
    public void doLayout(){
        if(styleManager != null){
            styleManager.doLayout(this);
        }
        else{
            super.doLayout();
        }
    }
    
    @Override
    public void paint(Graphics g) {
        customGraphics(g);
        paintChildren(g);
    }
    private void paintChildren(Graphics g){
        for (BaseComp child : children) {
            Graphics g_ = styleManager.createChildGraphics(this, child, g);
            child.paint(g_);
        }
    }
    public void customGraphics(Graphics g) {
        if (styleManager != null) {
            styleManager.apply(g);
        }
    }



    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public BaseComp[] getChildren() {
        return children;
    }

    public StyleManager getStyleManager() {
        return styleManager;
    }

    public void setStyleManager(StyleManager styleManager) {
        this.styleManager = styleManager;
    }

}
