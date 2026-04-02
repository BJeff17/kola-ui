package kola.components;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NavMenuBar extends Div {
    private static class Item {
        String key;
        int width;
        Button button;

        Item(String key, int width, Button button) {
            this.key = key;
            this.width = width;
            this.button = button;
        }
    }

    private final List<Item> items;
    private Consumer<String> selectionListener;
    private String selectedKey;

    public NavMenuBar(int x, int y, int width, int height) {
        super(x, y, width, height, new Color(27, 34, 48, 185), 14);
        this.items = new ArrayList<>();
        this.selectedKey = null;
    }

    public void addItem(String key, String label) {
        Button b = new Button(label, 0, 0, 120, 34, () -> selectKey(key, true));
        b.setBackground(new Color(53, 64, 85));
        b.setForeground(new Color(225, 230, 240));
        addChild(b);
        int width = Math.max(96, Math.min(180, (label.length() * 8) + 30));
        items.add(new Item(key, width, b));
        relayoutButtons();
        if (selectedKey == null) {
            selectKey(key, false);
        }
    }

    public void setSelectionListener(Consumer<String> selectionListener) {
        this.selectionListener = selectionListener;
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    public void setSelectedKey(String key) {
        selectKey(key, false);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        relayoutButtons();
    }

    private void relayoutButtons() {
        if (items == null || items.isEmpty()) {
            return;
        }
        int x = 10;
        int y = 8;
        int h = Math.max(28, getHeight() - 16);
        for (Item item : items) {
            int w = item.width;
            item.button.setBounds(x, y, w, h);
            x += w + 8;
        }
    }

    private void selectKey(String key, boolean notify) {
        if (key == null || key.equals(selectedKey)) {
            return;
        }
        selectedKey = key;
        applyVisualState();
        if (notify && selectionListener != null) {
            selectionListener.accept(key);
        }
    }

    private void applyVisualState() {
        for (Item item : items) {
            boolean active = item.key.equals(selectedKey);
            item.button.setBackground(active ? new Color(68, 145, 255) : new Color(53, 64, 85));
            item.button.setForeground(active ? Color.WHITE : new Color(225, 230, 240));
            item.button.invalidate();
        }
        invalidate();
    }
}
