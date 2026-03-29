package components;

import event.UiEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import main.BaseComp;

public class SegmentedSelect extends BaseComp {
    private final List<String> options;
    private int selectedIndex;
    private Consumer<String> onChange;

    public SegmentedSelect(int x, int y, int width, int height) {
        super(null);
        this.options = new ArrayList<>();
        this.selectedIndex = 0;
        this.onChange = null;
        setBounds(x, y, width, height);
        setCursor(java.awt.Cursor.HAND_CURSOR);

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (event.getTarget() != this || options.isEmpty()) {
                return;
            }
            int lx = toLocalX(event.getX());
            int segment = Math.max(0, Math.min(options.size() - 1, (int) ((lx / (double) Math.max(1, getWidth())) * options.size())));
            setSelectedIndex(segment, true);
            event.stopPropagation();
        });
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int radius = 10;
        g2.setColor(new Color(236, 240, 248));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(new Color(198, 208, 222));
        g2.setStroke(new java.awt.BasicStroke(1.2f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

        if (options.isEmpty()) {
            g2.setColor(new Color(110, 122, 139));
            g2.setFont(new Font("Dialog", Font.PLAIN, 13));
            g2.drawString("No options", 10, Math.max(18, (getHeight() / 2) + 5));
            return;
        }

        int segmentWidth = Math.max(1, getWidth() / options.size());
        for (int i = 0; i < options.size(); i++) {
            int sx = i * segmentWidth;
            int sw = (i == options.size() - 1) ? getWidth() - sx : segmentWidth;
            boolean selected = i == selectedIndex;
            if (selected) {
                g2.setColor(new Color(66, 133, 244));
                g2.fillRoundRect(sx + 1, 1, Math.max(1, sw - 2), Math.max(1, getHeight() - 2), radius, radius);
            }

            g2.setColor(selected ? Color.WHITE : new Color(62, 75, 93));
            g2.setFont(new Font("Dialog", Font.BOLD, 13));
            String text = options.get(i);
            int tw = g2.getFontMetrics().stringWidth(text);
            int tx = sx + Math.max(8, (sw - tw) / 2);
            int ty = Math.max(18, (getHeight() / 2) + 5);
            g2.drawString(text, tx, ty);

            if (i < options.size() - 1) {
                g2.setColor(new Color(205, 214, 226));
                g2.drawLine(sx + sw, 6, sx + sw, getHeight() - 7);
            }
        }
    }

    public void setOptions(List<String> values) {
        options.clear();
        if (values != null) {
            options.addAll(values);
        }
        if (selectedIndex >= options.size()) {
            selectedIndex = Math.max(0, options.size() - 1);
        }
        invalidate();
    }

    public void setSelectedOption(String value) {
        int idx = options.indexOf(value);
        if (idx >= 0) {
            setSelectedIndex(idx, false);
        }
    }

    public String getSelectedOption() {
        if (options.isEmpty()) {
            return "";
        }
        return options.get(selectedIndex);
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    private void setSelectedIndex(int index, boolean fireChange) {
        int normalized = Math.max(0, Math.min(index, Math.max(0, options.size() - 1)));
        if (selectedIndex == normalized) {
            return;
        }
        selectedIndex = normalized;
        invalidate();
        if (fireChange && onChange != null && !options.isEmpty()) {
            onChange.accept(options.get(selectedIndex));
        }
    }
}
