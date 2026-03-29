package components;

import event.UiEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import main.BaseComp;

public class CheckBox extends BaseComp {
    private String label;
    private boolean checked;

    public CheckBox(String label, int x, int y, int width, int height, boolean initialValue) {
        super(null);
        this.label = label;
        this.checked = initialValue;
        setCursor(java.awt.Cursor.HAND_CURSOR);
        setBounds(x, y, width, height);

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (event.getTarget() != this) {
                return;
            }
            checked = !checked;
            invalidate();
        });
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        int boxSize = 18;
        int boxY = (getHeight() - boxSize) / 2;
        int boxX = 0; // Align left

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(boxX, boxY, boxSize, boxSize, 6, 6);

        if (checked) {
            g2.setColor(new Color(66, 133, 244)); // Nice blue
            g2.fillRoundRect(boxX, boxY, boxSize, boxSize, 6, 6);

            // Draw checkmark
            g2.setColor(Color.WHITE);
            g2.setStroke(
                    new java.awt.BasicStroke(2.0f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.drawLine(boxX + 4, boxY + 9, boxX + 8, boxY + 13);
            g2.drawLine(boxX + 8, boxY + 13, boxX + 14, boxY + 5);
        } else {
            g2.setColor(new Color(200, 205, 211));
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawRoundRect(boxX, boxY, boxSize, boxSize, 6, 6);
        }

        g2.setColor(new Color(59, 63, 70));
        g2.setFont(new Font("Dialog", Font.BOLD, 13));
        int textY = boxY + boxSize - 4; // approximate baseline
        g2.drawString(label, boxX + boxSize + 10, textY);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
