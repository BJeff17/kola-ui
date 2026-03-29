package components;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;

public class LiveClockLabel extends Label {
    private final DateTimeFormatter formatter;
    private final Timer timer;

    public LiveClockLabel(int x, int y, int width, int height, String pattern) {
        super("", x, y, width, height);
        this.formatter = DateTimeFormatter
                .ofPattern(pattern == null || pattern.isBlank() ? "dd/MM/yyyy HH:mm:ss" : pattern);
        this.timer = new Timer(800, e -> tick());
        this.timer.setRepeats(true);
        tick();
        this.timer.start();
    }

    private void tick() {
        setText(LocalDateTime.now().format(formatter));
        invalidate();
    }
}
