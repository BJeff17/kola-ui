package components;

import java.awt.Color;
import java.awt.Font;
import main.BaseComp;

public class FormModal extends Div {
    private final H titleLabel;
    private final Button closeButton;
    private final Div body;
    private boolean initialized;

    public FormModal(int width, int height, String title, Runnable onClose) {
        super(0, 0, width, height, new Color(255, 255, 255), 16);
        this.initialized = false;

        titleLabel = new H(title == null ? "" : title, 18, 14, Math.max(80, width - 88), 28);
        titleLabel.setColor(new Color(27, 39, 56));
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        addChild(titleLabel);

        closeButton = new Button("X", width - 50, 14, 32, 28, onClose);
        closeButton.setBackground(new Color(222, 229, 238));
        closeButton.setForeground(new Color(54, 66, 82));
        addChild(closeButton);

        body = new Div(0, 54, width, height - 54, new Color(0, 0, 0, 0), 0);
        addChild(body);

        this.initialized = true;
        layoutInternal(width, height);
    }

    public BaseComp getBody() {
        return body;
    }

    public void setTitle(String title) {
        titleLabel.setText(title == null ? "" : title);
        titleLabel.invalidate();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (!initialized) {
            return;
        }
        layoutInternal(width, height);
    }

    private void layoutInternal(int width, int height) {
        titleLabel.setBounds(18, 14, Math.max(80, width - 88), 28);
        closeButton.setBounds(width - 50, 14, 32, 28);
        body.setBounds(0, 54, width, Math.max(10, height - 54));
    }
}
