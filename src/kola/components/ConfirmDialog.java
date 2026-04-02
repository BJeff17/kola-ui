package kola.components;

import java.awt.Color;

public class ConfirmDialog extends FormModal {
    public ConfirmDialog(int width, int height, String title, String message, Runnable onConfirm, Runnable onCancel) {
        super(width, height, title, onCancel);

        Label messageLabel = new Label(message == null ? "Confirmer l'action ?" : message, 18, 18, width - 36, 54);
        messageLabel.setColor(new Color(72, 84, 102));
        getBody().addChild(messageLabel);

        Button cancel = new Button("Annuler", width - 244, height - 52 - 54, 104, 34, onCancel);
        cancel.setBackground(new Color(191, 201, 216));
        cancel.setForeground(new Color(58, 70, 86));
        getBody().addChild(cancel);

        Button confirm = new Button("Confirmer", width - 128, height - 52 - 54, 104, 34, onConfirm);
        confirm.setBackground(new Color(216, 102, 102));
        confirm.setForeground(Color.WHITE);
        getBody().addChild(confirm);
    }
}
