package main;
import java.awt.*;
public class BaseWindow extends BaseComp {
    private final Window window;
    public BaseWindow() {
        super(null);
        this.window = new Window((Frame) null);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        window.paint(g);    
    }

}
