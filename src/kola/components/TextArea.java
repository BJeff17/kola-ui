package kola.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import kola.main.BaseComp;

public class TextArea extends BaseComp {
    private String text;

    public TextArea(String text, int x, int y, int width, int height) {
        super(null);
        this.text = text;
        setBounds(x, y, width, height);
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(new Color(200, 205, 211));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

        g2.setColor(new Color(55, 60, 68));
        g2.setFont(new Font("Dialog", Font.PLAIN, 13));
        String[] lines = text.split("\\n");
        int y = 22;
        for (String line : lines) {
            if (y > getHeight() - 8) {
                break;
            }
            g2.drawString(line, 12, y);
            y += 17;
        }
    }

    public void setText(String text) {
        this.text = text;
    }
}
