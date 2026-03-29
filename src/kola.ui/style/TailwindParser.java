package style;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class TailwindParser {
    private static final Map<String, Color> COLOR_MAP = buildColorMap();

    public static void applyTailwind(StyleManager style, String tailwindClasses) {
        if (style == null || tailwindClasses == null || tailwindClasses.isBlank()) {
            return;
        }

        String[] classes = tailwindClasses.trim().split("\\s+");
        for (String token : classes) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String c = token.trim();

            if (c.startsWith("bg-")) {
                applyBackground(style, c.substring(3));
                continue;
            }

            if ("flex".equals(c)) {
                style.setLayoutEngineType("flex");
                continue;
            }
            if ("grid".equals(c)) {
                style.setLayoutEngineType("grid");
                continue;
            }
            if ("block".equals(c)) {
                style.setLayoutEngineType("block");
                continue;
            }
            if ("absolute".equals(c) || "relative".equals(c) || "fixed".equals(c) || "sticky".equals(c)) {
                // Our renderer uses absolute layout semantics for positioned content.
                style.setLayoutEngineType("absolute");
                continue;
            }

            if ("flex-col".equals(c)) {
                style.setFlexProps(true, style.getGap());
                continue;
            }
            if ("flex-row".equals(c)) {
                style.setFlexProps(false, style.getGap());
                continue;
            }

            if (c.startsWith("gap-")) {
                int gap = parseSpacing(c.substring(4));
                style.setFlexProps(style.isColumnFlex(), gap);
                style.setBlockProps(gap);
                continue;
            }

            if (c.startsWith("grid-cols-")) {
                int cols = Math.max(1, parseNumericToken(c.substring("grid-cols-".length()), 1));
                style.setGridProps(false, style.getNumRows(), cols);
                continue;
            }
            if (c.startsWith("grid-rows-")) {
                int rows = Math.max(1, parseNumericToken(c.substring("grid-rows-".length()), 1));
                style.setGridProps(true, rows, style.getNumCols());
                continue;
            }

            if (c.startsWith("rounded")) {
                Integer radius = parseRounded(c);
                if (radius != null) {
                    style.setBorderRadius(radius);
                }
                continue;
            }

            if (c.startsWith("w-")) {
                applyWidth(style, c.substring(2));
                continue;
            }
            if (c.startsWith("h-")) {
                applyHeight(style, c.substring(2));
            }
        }
    }

    private static void applyBackground(StyleManager style, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        String value = raw;
        Integer alpha = null;
        int slash = raw.indexOf('/');
        if (slash > 0 && slash < raw.length() - 1) {
            value = raw.substring(0, slash);
            alpha = parseAlpha(raw.substring(slash + 1));
        }

        Color base = parseColor(value);
        if (base == null) {
            return;
        }
        if (alpha != null) {
            int a = Math.max(0, Math.min(255, alpha));
            base = new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
        }
        style.setColor(base);
    }

    private static void applyWidth(StyleManager style, String token) {
        Integer px = parseSizeToken(token);
        if (px != null) {
            style.setWidth(px);
        }
    }

    private static void applyHeight(StyleManager style, String token) {
        Integer px = parseSizeToken(token);
        if (px != null) {
            style.setHeight(px);
        }
    }

    private static Integer parseSizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if ("full".equals(token)) {
            return -1;
        }
        if ("screen".equals(token)) {
            return 1000;
        }

        if (token.contains("/")) {
            String[] parts = token.split("/");
            if (parts.length == 2) {
                try {
                    double n = Double.parseDouble(parts[0]);
                    double d = Double.parseDouble(parts[1]);
                    if (d != 0.0) {
                        return (int) Math.max(1, Math.round((n / d) * 1000.0));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return parseNumericToken(token, null);
    }

    private static Integer parseRounded(String token) {
        if ("rounded-none".equals(token)) {
            return 0;
        }
        if ("rounded-sm".equals(token)) {
            return 4;
        }
        if ("rounded".equals(token) || "rounded-md".equals(token)) {
            return 8;
        }
        if ("rounded-lg".equals(token)) {
            return 12;
        }
        if ("rounded-xl".equals(token)) {
            return 16;
        }
        if ("rounded-2xl".equals(token)) {
            return 24;
        }
        if ("rounded-3xl".equals(token)) {
            return 32;
        }
        if ("rounded-full".equals(token)) {
            return 9999;
        }
        if (token.startsWith("rounded-[") && token.endsWith("]")) {
            String raw = token.substring("rounded-[".length(), token.length() - 1);
            return parseNumericToken(raw, 8);
        }
        return null;
    }

    private static int parseSpacing(String token) {
        Integer value = parseNumericToken(token, 0);
        return Math.max(0, value == null ? 0 : value);
    }

    private static Integer parseNumericToken(String token, Integer fallback) {
        if (token == null || token.isBlank()) {
            return fallback;
        }

        String raw = token.trim();
        boolean bracketed = false;
        if (raw.startsWith("[") && raw.endsWith("]") && raw.length() > 2) {
            raw = raw.substring(1, raw.length() - 1);
            bracketed = true;
        }

        if (raw.endsWith("px")) {
            raw = raw.substring(0, raw.length() - 2);
        }

        if (raw.endsWith("rem")) {
            try {
                double rem = Double.parseDouble(raw.substring(0, raw.length() - 3));
                return (int) Math.round(rem * 16.0);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        try {
            if (raw.contains(".")) {
                return (int) Math.round(Double.parseDouble(raw));
            }
            int n = Integer.parseInt(raw);
            if (bracketed) {
                return n;
            }
            // Tailwind scale token (e.g. w-12 => 48px) when plain number and no unit.
            return n * 4;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Integer parseAlpha(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            int pct = Integer.parseInt(token.trim());
            pct = Math.max(0, Math.min(100, pct));
            return (int) Math.round((pct / 100.0) * 255.0);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Color parseColor(String colorToken) {
        if (colorToken == null || colorToken.isBlank()) {
            return null;
        }

        String token = colorToken.trim().toLowerCase();
        if (token.startsWith("[") && token.endsWith("]") && token.length() > 2) {
            token = token.substring(1, token.length() - 1).trim();
        }

        if ("transparent".equals(token)) {
            return new Color(0, 0, 0, 0);
        }

        if (token.startsWith("#")) {
            return parseHexColor(token);
        }

        if (token.startsWith("rgb(")) {
            return parseRgbColor(token, false);
        }
        if (token.startsWith("rgba(")) {
            return parseRgbColor(token, true);
        }

        return COLOR_MAP.getOrDefault(token, null);
    }

    private static Color parseHexColor(String token) {
        String hex = token.substring(1);
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            int rgb = Integer.parseInt(hex, 16);
            return new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Color parseRgbColor(String token, boolean rgba) {
        try {
            String inside = token.substring(token.indexOf('(') + 1, token.lastIndexOf(')'));
            String[] parts = inside.split(",");
            if (parts.length < 3) {
                return null;
            }
            int r = clamp255((int) Math.round(Double.parseDouble(parts[0].trim())));
            int g = clamp255((int) Math.round(Double.parseDouble(parts[1].trim())));
            int b = clamp255((int) Math.round(Double.parseDouble(parts[2].trim())));
            if (!rgba || parts.length < 4) {
                return new Color(r, g, b);
            }
            double alpha = Double.parseDouble(parts[3].trim());
            int a = clamp255((int) Math.round(alpha <= 1.0 ? alpha * 255.0 : alpha));
            return new Color(r, g, b, a);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static Map<String, Color> buildColorMap() {
        Map<String, Color> map = new HashMap<>();
        map.put("black", new Color(0, 0, 0));
        map.put("white", new Color(255, 255, 255));
        map.put("gray-50", new Color(249, 250, 251));
        map.put("gray-100", new Color(243, 244, 246));
        map.put("gray-200", new Color(229, 231, 235));
        map.put("gray-400", new Color(156, 163, 175));
        map.put("gray-600", new Color(75, 85, 99));
        map.put("gray-800", new Color(31, 41, 55));
        map.put("slate-700", new Color(51, 65, 85));
        map.put("slate-900", new Color(15, 23, 42));
        map.put("blue-400", new Color(96, 165, 250));
        map.put("blue-500", new Color(59, 130, 246));
        map.put("blue-600", new Color(37, 99, 235));
        map.put("green-400", new Color(74, 222, 128));
        map.put("green-500", new Color(34, 197, 94));
        map.put("red-400", new Color(248, 113, 113));
        map.put("red-500", new Color(239, 68, 68));
        map.put("yellow-400", new Color(250, 204, 21));
        map.put("purple-500", new Color(168, 85, 247));
        map.put("cyan-500", new Color(6, 182, 212));
        map.put("amber-500", new Color(245, 158, 11));
        return map;
    }
}
