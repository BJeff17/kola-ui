package style;

import java.awt.Color;

public class TailwindParser {
    public static void applyTailwind(StyleManager style, String tailwindClasses) {
        if (tailwindClasses == null || tailwindClasses.isEmpty()) return;
        
        String[] classes = tailwindClasses.split("\\s+");
        for (String c : classes) {
            c = c.trim();
            if (c.isEmpty()) continue;
            
            // Colors
            if (c.startsWith("bg-")) {
                style.setColor(parseColor(c.substring(3)));
            }
            // Layout (display)
            else if (c.equals("flex")) {
                style.setLayoutEngineType("flex");
            } else if (c.equals("grid")) {
                style.setLayoutEngineType("grid");
            } else if (c.equals("block")) {
                style.setLayoutEngineType("block");
            } else if (c.equals("absolute")) {
                style.setLayoutEngineType("absolute");
            }
            // Flex directions
            else if (c.equals("flex-col")) {
                style.setFlexProps(true, style.getGap());
            } else if (c.equals("flex-row")) {
                style.setFlexProps(false, style.getGap());
            }
            // Gaps
            else if (c.startsWith("gap-")) {
                int gap = parseIntValue(c.substring(4)) * 4; // 1 unit = 4px
                style.setFlexProps(style.isColumnFlex(), gap);
                style.setBlockProps(gap);
            }
            // Border radius
            else if (c.startsWith("rounded")) {
                if (c.equals("rounded-sm")) style.setBorderRadius(4);
                else if (c.equals("rounded") || c.equals("rounded-md")) style.setBorderRadius(8);
                else if (c.equals("rounded-lg")) style.setBorderRadius(12);
                else if (c.equals("rounded-xl")) style.setBorderRadius(16);
                else if (c.equals("rounded-2xl")) style.setBorderRadius(24);
                else if (c.equals("rounded-full")) style.setBorderRadius(9999);
            }
            // Width and Height
            else if (c.startsWith("w-")) {
                if (c.equals("w-full")) style.setWidth(-1); // -1 means 100%
                else style.setWidth(parseIntValue(c.substring(2)) * 4);
            }
            else if (c.startsWith("h-")) {
                if (c.equals("h-full")) style.setHeight(-1); // -1 means 100%
                else style.setHeight(parseIntValue(c.substring(2)) * 4);
            }
            // Padding & Alignment can be expanded...
        }
    }
    
    private static Color parseColor(String colorStr) {
        // Handle basic tailwind colors
        if (colorStr.equals("blue-500")) return new Color(59, 130, 246);
        if (colorStr.equals("red-500")) return new Color(239, 68, 68);
        if (colorStr.equals("green-500")) return new Color(34, 197, 94);
        if (colorStr.equals("gray-100")) return new Color(243, 244, 246);
        if (colorStr.equals("gray-800")) return new Color(31, 41, 55);
        if (colorStr.equals("white")) return Color.WHITE;
        if (colorStr.equals("black")) return Color.BLACK;
        if (colorStr.equals("transparent")) return new Color(0, 0, 0, 0);
        return Color.LIGHT_GRAY;
    }
    
    private static int parseIntValue(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
    }
}
