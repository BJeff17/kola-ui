package kola.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import javax.imageio.ImageIO;
import kola.main.BaseComp;

public class Div extends BaseComp {
    private Color background;
    private int radius;
    private float alpha;
    private Image backgroundImage;
    private float backgroundImageAlpha;

    public Div(int x, int y, int width, int height, Color background, int radius) {
        super(null);
        setBounds(x, y, width, height);
        this.background = background;
        this.radius = Math.max(0, radius);
        this.alpha = 1.0f;
        this.backgroundImageAlpha = 1.0f;
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        java.awt.Composite previous = g2.getComposite();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
        g2.setColor(background);
        if (radius > 0) {
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        } else {
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        if (backgroundImage != null) {
            g2.setComposite(
                    java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, backgroundImageAlpha));
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        }
        g2.setComposite(previous);
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public void setRadius(int radius) {
        this.radius = Math.max(0, radius);
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public void setBackgroundImage(String path) {
        if (path == null || path.isBlank()) {
            this.backgroundImage = null;
            return;
        }
        try {
            this.backgroundImage = ImageIO.read(new File(path));
        } catch (java.io.IOException ignored) {
            this.backgroundImage = null;
        }
    }

    public void setBackgroundImageAlpha(float alpha) {
        this.backgroundImageAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }
}
