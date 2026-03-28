package components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import main.BaseComp;

public class ImageComp extends BaseComp {
    private Image image;
    private float alpha = 1.0f;

    public ImageComp(String path, int x, int y, int width, int height) {
        super(null);
        setBounds(x, y, width, height);
        load(path);
    }

    private void load(String path) {
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException ignored) {
            image = null;
        }
    }

    public void setImagePath(String path) {
        load(path);
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setColor(new Color(236, 238, 242));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        if (image != null) {
            java.awt.Composite previous = g2.getComposite();
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            g2.setComposite(previous);
        } else {
            g2.setColor(new Color(138, 144, 153));
            g2.drawString("Image", 12, 22);
        }
    }
}
