package kola.components;

import kola.event.UiEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import kola.main.BaseComp;
import kola.main.BaseWindow;

public class SelectInput extends BaseComp {
    private final List<String> options;
    private int selectedIndex;
    private Color background;
    private Color border;
    private Color focusBorder;
    private Color textColor;
    private boolean popupOpen;
    private BaseComp popupLayer;
    private Consumer<String> onChange;

    public SelectInput(int x, int y, int width, int height) {
        super(null);
        this.options = new ArrayList<>();
        this.selectedIndex = 0;
        this.background = Color.WHITE;
        this.border = new Color(200, 205, 211);
        this.focusBorder = new Color(66, 133, 244);
        this.textColor = new Color(40, 46, 54);
        this.popupOpen = false;
        this.popupLayer = null;
        this.onChange = null;

        setBounds(x, y, width, height);
        setFocusable(true);
        setCursor(java.awt.Cursor.HAND_CURSOR);

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (event.getTarget() != this) {
                return;
            }
            togglePopup();
            event.stopPropagation();
        });
    }

    @Override
    public boolean onKeyPressed(int keyCode, char keyChar) {
        if (!isFocused()) {
            return false;
        }
        if (options.isEmpty()) {
            return true;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_RIGHT || keyCode == java.awt.event.KeyEvent.VK_DOWN) {
            selectedIndex = (selectedIndex + 1) % options.size();
            fireOnChange();
            invalidate();
            return true;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_LEFT || keyCode == java.awt.event.KeyEvent.VK_UP) {
            selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
            fireOnChange();
            invalidate();
            return true;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
            closePopup();
            return true;
        }
        return false;
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int radius = 10;

        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

        g2.setColor(isFocused() ? focusBorder : border);
        g2.setStroke(new java.awt.BasicStroke(isFocused() ? 2.0f : 1.5f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

        g2.setColor(textColor);
        g2.setFont(new Font("Dialog", Font.PLAIN, 14));
        String value = getSelectedOption();
        int baseline = Math.max(18, (getHeight() / 2) + 6);
        g2.drawString(value, 12, baseline);

        int arrowX = getWidth() - 20;
        int arrowY = (getHeight() / 2) - 2;
        if (popupOpen) {
            g2.drawLine(arrowX - 4, arrowY + 4, arrowX, arrowY);
            g2.drawLine(arrowX, arrowY, arrowX + 4, arrowY + 4);
        } else {
            g2.drawLine(arrowX - 4, arrowY, arrowX, arrowY + 4);
            g2.drawLine(arrowX, arrowY + 4, arrowX + 4, arrowY);
        }
    }

    public void setOptions(List<String> values) {
        this.options.clear();
        if (values != null) {
            this.options.addAll(values);
        }
        if (selectedIndex >= options.size()) {
            selectedIndex = Math.max(0, options.size() - 1);
        }
        invalidate();
    }

    public void setSelectedOption(String value) {
        if (value == null) {
            return;
        }
        int idx = options.indexOf(value);
        if (idx >= 0) {
            selectedIndex = idx;
            invalidate();
            fireOnChange();
        }
    }

    public String getSelectedOption() {
        if (options.isEmpty()) {
            return "No option";
        }
        return options.get(selectedIndex);
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    private void togglePopup() {
        if (popupOpen) {
            closePopup();
            return;
        }
        openPopup();
    }

    private void openPopup() {
        if (options.isEmpty() || popupOpen) {
            return;
        }
        BaseWindow window = getOwnerWindow();
        if (window == null || window.getLayerHost() == null) {
            return;
        }

        final int rowHeight = 30;
        final int panelHeight = Math.min(220, Math.max(rowHeight, options.size() * rowHeight) + 8);

        BaseComp layer = new BaseComp(null) {
            @Override
            public void customGraphics(Graphics g) {
                // transparent click-catcher layer
            }
        };
        layer.setBounds(0, 0, window.getLayerHost().getWidth(), window.getLayerHost().getHeight());

        Div panel = new Div(0, 0, Math.max(120, getWidth()), panelHeight, Color.WHITE, 10);
        int panelX = getGlobalX() - layer.getGlobalX();
        int panelY = getGlobalY() - layer.getGlobalY() + getHeight() + 4;
        panel.setBounds(panelX, panelY, panel.getWidth(), panel.getHeight());

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            final int idx = i;
            Button row = new Button(option, 4, 4 + (i * rowHeight), panel.getWidth() - 8, rowHeight - 2, () -> {
                selectedIndex = idx;
                fireOnChange();
                closePopup();
                invalidate();
            });
            row.setBackground(idx == selectedIndex ? new Color(66, 133, 244) : new Color(244, 247, 252));
            row.setForeground(idx == selectedIndex ? Color.WHITE : new Color(54, 66, 82));
            panel.addChild(row);
        }

        layer.addChild(panel);
        layer.getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (event.getTarget() == layer) {
                closePopup();
                event.stopPropagation();
            }
        });

        popupLayer = layer;
        popupOpen = true;
        window.openLayer(layer);
        invalidate();
    }

    private void closePopup() {
        if (!popupOpen) {
            return;
        }
        if (popupLayer != null && popupLayer.getParent() != null) {
            popupLayer.getParent().removeChild(popupLayer);
        }
        popupLayer = null;
        popupOpen = false;
        invalidate();
    }

    private void fireOnChange() {
        if (onChange != null && !options.isEmpty()) {
            onChange.accept(options.get(selectedIndex));
        }
    }
}
