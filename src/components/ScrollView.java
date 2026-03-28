package components;

import event.UiEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import main.BaseComp;

public class ScrollView extends BaseComp {
    private final BaseComp content;
    private int scrollY;
    private int contentHeight;

    public ScrollView(int x, int y, int width, int height) {
        super(null);
        setBounds(x, y, width, height);
        this.content = new BaseComp(null);
        this.content.setBounds(0, 0, width, height);
        addChild(content);

        getEventManager().register(UiEvent.Type.WHEEL, (component, event) -> {
            int delta = event.getWheelRotation() * 24;
            setScrollY(scrollY + delta);
            event.getWindow().requestRender();
        });
    }

    public BaseComp getContent() {
        return content;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(getHeight(), contentHeight);
        syncContentPosition();
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int newScrollY) {
        int max = Math.max(0, contentHeight - getHeight());
        this.scrollY = Math.max(0, Math.min(newScrollY, max));
        syncContentPosition();
    }

    private void syncContentPosition() {
        content.setBounds(0, -scrollY, getWidth(), contentHeight);
    }

    @Override
    public void paint(Graphics g) {
        customGraphics(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clipRect(0, 0, getWidth(), getHeight());
        paintChildren(g2);
        g2.dispose();
    }
}
