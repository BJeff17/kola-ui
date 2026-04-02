package kola.components;

import java.awt.Font;

public class H extends Label {
    public H(String text, int x, int y, int width, int height) {
        super(text, x, y, width, height);
        setFont(new Font("Dialog", Font.BOLD, 22));
    }
}
