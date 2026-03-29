package components;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import main.BaseComp;

public class ImageComp extends BaseComp {
    private Image image;
    private String altText;
    private String source;
    private float alpha = 1.0f;

    public ImageComp(String path, int x, int y, int width, int height) {
        this(path, x, y, width, height, "Image indisponible");
    }

    public ImageComp(String source, int x, int y, int width, int height, String altText) {
        super(null);
        this.altText = altText == null ? "Image indisponible" : altText;
        setBounds(x, y, width, height);
        setImageSource(source);
    }

    private void loadFromPath(String path) {
        if (path == null || path.isBlank()) {
            image = null;
            return;
        }
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException ignored) {
            image = null;
        }
    }

    private void loadFromUrl(String url) {
        if (url == null || url.isBlank()) {
            image = null;
            return;
        }
        try {
            image = ImageIO.read(new URL(url));
        } catch (IOException ignored) {
            image = null;
        }
    }

    private boolean isWebUrl(String value) {
        if (value == null) {
            return false;
        }
        try {
            URL parsed = new URL(value);
            String protocol = parsed.getProtocol();
            return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
        } catch (MalformedURLException ignored) {
            return false;
        }
    }

    public void setImagePath(String path) {
        this.source = path;
        loadFromPath(path);
        invalidate();
    }

    public void setImageUrl(String url) {
        this.source = url;
        loadFromUrl(url);
        invalidate();
    }

    public void setImageSource(String source) {
        this.source = source;
        if (isWebUrl(source)) {
            loadFromUrl(source);
        } else {
            loadFromPath(source);
        }
        invalidate();
    }

    public void setAltText(String altText) {
        this.altText = altText == null || altText.isBlank() ? "Image indisponible" : altText;
        invalidate();
    }

    public String getSource() {
        return source;
    }

    public String getAltText() {
        return altText;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        invalidate();
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
            String fallback = (altText == null || altText.isBlank()) ? "Image indisponible" : altText;
            FontMetrics fm = g2.getFontMetrics();
            int maxWidth = Math.max(20, getWidth() - 24);
            int y = 22;
            for (String line : wrapLines(fallback, fm, maxWidth)) {
                g2.drawString(line, 12, y);
                y += fm.getHeight();
                if (y > getHeight() - 8) {
                    break;
                }
            }
        }
    }

    private java.util.List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("Image indisponible");
            return lines;
        }

        StringBuilder line = new StringBuilder();
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (line.length() == 0) {
                line.append(word);
                continue;
            }
            String candidate = line + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
