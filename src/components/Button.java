package components;

import event.UiEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import main.BaseComp;

public class Button extends BaseComp {
    private final Runnable onClick;
    private String text;
    private Color background;
    private Color foreground;
    private int radius;
    private boolean pressed;

    public Button(String text, int x, int y, int width, int height, Runnable onClick) {
        super(null);
        this.text = text;
        this.onClick = onClick;
        this.background = new Color(61, 126, 245);
        this.foreground = Color.WHITE;
        this.radius = 12;
        setCursor(java.awt.Cursor.HAND_CURSOR);
        setBounds(x, y, width, height);

        getEventManager().register(UiEvent.Type.POINTER_DOWN, (component, event) -> {
            if (event.getTarget() != this) {
                return;
            }
            pressed = true;
            event.getWindow().requestRender();
        });

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            boolean shouldClick = pressed && event.getTarget() == this;
            pressed = false;
            if (shouldClick && onClick != null) {
                onClick.run();
            }
            event.getWindow().requestRender();
        });
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(pressed ? background.darker() : background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(new Color(0, 0, 0, 30));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

        g2.setColor(foreground);
        g2.setFont(new Font("Dialog", Font.BOLD, 13));
        int textWidth = g2.getFontMetrics().stringWidth(text);
        int textX = Math.max(8, (getWidth() - textWidth) / 2);
        int textY = (getHeight() / 2) + 5;
        g2.drawString(text, textX, textY);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }
}
