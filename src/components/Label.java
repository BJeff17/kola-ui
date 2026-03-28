package components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import main.BaseComp;

public class Label extends BaseComp {
    private String text;
    private Font font;
    private Color color;

    public Label(String text, int x, int y, int width, int height) {
        super(null);
        this.text = text;
        this.font = new Font("Dialog", Font.PLAIN, 14);
        this.color = new Color(42, 46, 52);
        setBounds(x, y, width, height);
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(color);
        g2.setFont(font);
        int baseline = Math.max(16, (getHeight() / 2) + 5);
        g2.drawString(text, 0, baseline);
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
