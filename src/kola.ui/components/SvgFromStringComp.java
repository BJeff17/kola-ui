package components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import main.BaseComp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SvgFromStringComp extends BaseComp {
    private static class Primitive {
        String type;
        double[] values;
        String points;
        Color fill;
        Color stroke;
        float strokeWidth;

        Primitive(String type) {
            this.type = type;
            this.values = new double[8];
            this.points = "";
            this.fill = new Color(0, 0, 0, 0);
            this.stroke = new Color(0, 0, 0, 0);
            this.strokeWidth = 1.0f;
        }
    }

    private final List<Primitive> primitives;
    private String svgSource;
    private String errorMessage;
    private double viewWidth;
    private double viewHeight;

    public SvgFromStringComp(String svgSource, int x, int y, int width, int height) {
        super(null);
        this.primitives = new ArrayList<>();
        this.svgSource = svgSource == null ? "" : svgSource;
        this.errorMessage = null;
        this.viewWidth = 100.0;
        this.viewHeight = 100.0;
        setBounds(x, y, width, height);
        parse();
    }

    public void setSvgSource(String svgSource) {
        this.svgSource = svgSource == null ? "" : svgSource;
        parse();
        invalidate();
    }

    public String getSvgSource() {
        return svgSource;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(245, 247, 250));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        if (errorMessage != null) {
            g2.setColor(new Color(201, 74, 74));
            g2.drawString("SVG error", 12, 20);
            g2.setColor(new Color(120, 128, 138));
            g2.drawString(errorMessage, 12, 38);
            return;
        }

        double sx = getWidth() / Math.max(1.0, viewWidth);
        double sy = getHeight() / Math.max(1.0, viewHeight);

        Graphics2D layer = (Graphics2D) g2.create();
        layer.scale(sx, sy);

        for (Primitive p : primitives) {
            switch (p.type) {
                case "rect" -> drawRect(layer, p);
                case "circle" -> drawCircle(layer, p);
                case "ellipse" -> drawEllipse(layer, p);
                case "line" -> drawLine(layer, p);
                case "polyline" -> drawPolyline(layer, p, false);
                case "polygon" -> drawPolyline(layer, p, true);
                default -> {
                }
            }
        }

        layer.dispose();
    }

    private void drawRect(Graphics2D g2, Primitive p) {
        Rectangle2D rect = new Rectangle2D.Double(p.values[0], p.values[1], p.values[2], p.values[3]);
        if (p.fill.getAlpha() > 0) {
            g2.setColor(p.fill);
            g2.fill(rect);
        }
        if (p.stroke.getAlpha() > 0 && p.strokeWidth > 0f) {
            g2.setColor(p.stroke);
            g2.setStroke(new java.awt.BasicStroke(p.strokeWidth));
            g2.draw(rect);
        }
    }

    private void drawCircle(Graphics2D g2, Primitive p) {
        double cx = p.values[0];
        double cy = p.values[1];
        double r = p.values[2];
        Ellipse2D shape = new Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0);
        if (p.fill.getAlpha() > 0) {
            g2.setColor(p.fill);
            g2.fill(shape);
        }
        if (p.stroke.getAlpha() > 0 && p.strokeWidth > 0f) {
            g2.setColor(p.stroke);
            g2.setStroke(new java.awt.BasicStroke(p.strokeWidth));
            g2.draw(shape);
        }
    }

    private void drawEllipse(Graphics2D g2, Primitive p) {
        double cx = p.values[0];
        double cy = p.values[1];
        double rx = p.values[2];
        double ry = p.values[3];
        Ellipse2D shape = new Ellipse2D.Double(cx - rx, cy - ry, rx * 2.0, ry * 2.0);
        if (p.fill.getAlpha() > 0) {
            g2.setColor(p.fill);
            g2.fill(shape);
        }
        if (p.stroke.getAlpha() > 0 && p.strokeWidth > 0f) {
            g2.setColor(p.stroke);
            g2.setStroke(new java.awt.BasicStroke(p.strokeWidth));
            g2.draw(shape);
        }
    }

    private void drawLine(Graphics2D g2, Primitive p) {
        if (p.stroke.getAlpha() == 0 || p.strokeWidth <= 0f) {
            return;
        }
        g2.setColor(p.stroke);
        g2.setStroke(new java.awt.BasicStroke(p.strokeWidth));
        g2.draw(new java.awt.geom.Line2D.Double(p.values[0], p.values[1], p.values[2], p.values[3]));
    }

    private void drawPolyline(Graphics2D g2, Primitive p, boolean closed) {
        double[] pts = parsePoints(p.points);
        if (pts.length < 4) {
            return;
        }
        Path2D path = new Path2D.Double();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i + 1 < pts.length; i += 2) {
            path.lineTo(pts[i], pts[i + 1]);
        }
        if (closed) {
            path.closePath();
        }

        if (closed && p.fill.getAlpha() > 0) {
            g2.setColor(p.fill);
            g2.fill(path);
        }
        if (p.stroke.getAlpha() > 0 && p.strokeWidth > 0f) {
            g2.setColor(p.stroke);
            g2.setStroke(new java.awt.BasicStroke(p.strokeWidth));
            g2.draw(path);
        }
    }

    private double[] parsePoints(String raw) {
        if (raw == null || raw.isBlank()) {
            return new double[0];
        }
        String normalized = raw.replace(',', ' ').trim();
        String[] parts = normalized.split("\\s+");
        double[] pts = new double[parts.length];
        int count = 0;
        for (String p : parts) {
            try {
                pts[count++] = Double.parseDouble(p);
            } catch (NumberFormatException ignored) {
            }
        }
        double[] out = new double[count];
        System.arraycopy(pts, 0, out, 0, count);
        return out;
    }

    private void parse() {
        primitives.clear();
        errorMessage = null;
        viewWidth = 100.0;
        viewHeight = 100.0;

        if (svgSource == null || svgSource.isBlank()) {
            errorMessage = "empty svg";
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svgSource)));
            Element root = doc.getDocumentElement();
            if (root == null || !"svg".equalsIgnoreCase(root.getTagName())) {
                errorMessage = "root <svg> missing";
                return;
            }

            readViewBox(root);
            collectPrimitives(root);
        } catch (Exception ex) {
            errorMessage = ex.getMessage() == null ? "parse failed" : ex.getMessage();
        }
    }

    private void readViewBox(Element root) {
        String viewBox = root.getAttribute("viewBox");
        if (viewBox != null && !viewBox.isBlank()) {
            String[] parts = viewBox.trim().split("\\s+");
            if (parts.length == 4) {
                try {
                    viewWidth = Double.parseDouble(parts[2]);
                    viewHeight = Double.parseDouble(parts[3]);
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        viewWidth = parseDouble(root.getAttribute("width"), 100.0);
        viewHeight = parseDouble(root.getAttribute("height"), 100.0);
    }

    private void collectPrimitives(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) {
                continue;
            }

            String tag = child.getTagName().toLowerCase();
            Primitive p = switch (tag) {
                case "rect", "circle", "ellipse", "line", "polyline", "polygon" -> new Primitive(tag);
                default -> null;
            };

            if (p != null) {
                fillPrimitiveAttributes(child, p);
                primitives.add(p);
            }

            collectPrimitives(child);
        }
    }

    private void fillPrimitiveAttributes(Element el, Primitive p) {
        p.fill = parseColor(el.getAttribute("fill"), new Color(0, 0, 0, 0));
        p.stroke = parseColor(el.getAttribute("stroke"), new Color(0, 0, 0, 0));
        p.strokeWidth = (float) parseDouble(el.getAttribute("stroke-width"), 1.0);

        switch (p.type) {
            case "rect" -> {
                p.values[0] = parseDouble(el.getAttribute("x"), 0.0);
                p.values[1] = parseDouble(el.getAttribute("y"), 0.0);
                p.values[2] = parseDouble(el.getAttribute("width"), 0.0);
                p.values[3] = parseDouble(el.getAttribute("height"), 0.0);
            }
            case "circle" -> {
                p.values[0] = parseDouble(el.getAttribute("cx"), 0.0);
                p.values[1] = parseDouble(el.getAttribute("cy"), 0.0);
                p.values[2] = parseDouble(el.getAttribute("r"), 0.0);
            }
            case "ellipse" -> {
                p.values[0] = parseDouble(el.getAttribute("cx"), 0.0);
                p.values[1] = parseDouble(el.getAttribute("cy"), 0.0);
                p.values[2] = parseDouble(el.getAttribute("rx"), 0.0);
                p.values[3] = parseDouble(el.getAttribute("ry"), 0.0);
            }
            case "line" -> {
                p.values[0] = parseDouble(el.getAttribute("x1"), 0.0);
                p.values[1] = parseDouble(el.getAttribute("y1"), 0.0);
                p.values[2] = parseDouble(el.getAttribute("x2"), 0.0);
                p.values[3] = parseDouble(el.getAttribute("y2"), 0.0);
            }
            case "polyline", "polygon" -> p.points = el.getAttribute("points");
            default -> {
            }
        }
    }

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            String normalized = raw.trim().replace("px", "");
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Color parseColor(String raw, Color fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim().toLowerCase();
        if ("none".equals(value)) {
            return new Color(0, 0, 0, 0);
        }
        if (value.startsWith("#")) {
            return parseHexColor(value, fallback);
        }
        if (value.startsWith("rgb(")) {
            return parseRgbColor(value, fallback);
        }
        return switch (value) {
            case "black" -> new Color(0, 0, 0);
            case "white" -> new Color(255, 255, 255);
            case "red" -> new Color(239, 68, 68);
            case "green" -> new Color(34, 197, 94);
            case "blue" -> new Color(59, 130, 246);
            case "yellow" -> new Color(245, 158, 11);
            case "gray", "grey" -> new Color(148, 163, 184);
            default -> fallback;
        };
    }

    private Color parseHexColor(String value, Color fallback) {
        String hex = value.substring(1);
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.length() != 6) {
            return fallback;
        }
        try {
            int rgb = Integer.parseInt(hex, 16);
            return new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Color parseRgbColor(String value, Color fallback) {
        try {
            String inside = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String[] parts = inside.split(",");
            if (parts.length < 3) {
                return fallback;
            }
            int r = (int) parseDouble(parts[0], 0);
            int g = (int) parseDouble(parts[1], 0);
            int b = (int) parseDouble(parts[2], 0);
            return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)),
                    Math.max(0, Math.min(255, b)));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
