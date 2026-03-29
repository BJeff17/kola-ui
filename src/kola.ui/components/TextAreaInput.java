package components;

import event.UiEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import main.BaseComp;

public class TextAreaInput extends BaseComp {
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 12;
    private static final int LINE_HEIGHT = 18;
    private static final Font UI_FONT = new Font("Dialog", Font.PLAIN, 14);
    private static final int MAX_HISTORY = 140;

    private static class EditState {
        final String text;
        final int caret;
        final int anchor;
        final int preferredColumn;

        EditState(String text, int caret, int anchor, int preferredColumn) {
            this.text = text;
            this.caret = caret;
            this.anchor = anchor;
            this.preferredColumn = preferredColumn;
        }
    }

    private String text;
    private String placeholder;
    private int maxLength;
    private Color background;
    private Color border;
    private Color focusBorder;
    private Color textColor;
    private Color placeholderColor;
    private Color selectionColor;

    private int caretIndex;
    private int selectionAnchor;
    private int preferredColumn;
    private boolean draggingSelection;

    private final Deque<EditState> undoStack;
    private final Deque<EditState> redoStack;

    public TextAreaInput(int x, int y, int width, int height) {
        super(null);
        this.text = "";
        this.placeholder = "";
        this.maxLength = 1200;
        this.background = Color.WHITE;
        this.border = new Color(200, 205, 211);
        this.focusBorder = new Color(66, 133, 244);
        this.textColor = new Color(40, 46, 54);
        this.placeholderColor = new Color(140, 146, 156);
        this.selectionColor = new Color(66, 133, 244, 90);

        this.caretIndex = 0;
        this.selectionAnchor = 0;
        this.preferredColumn = 0;
        this.draggingSelection = false;

        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();

        setBounds(x, y, width, height);
        setFocusable(true);
        setCursor(java.awt.Cursor.TEXT_CURSOR);

        getEventManager().register(UiEvent.Type.POINTER_DOWN, (component, event) -> {
            if (event.getTarget() != this) {
                return;
            }
            int localX = toLocalX(event.getX());
            int localY = toLocalY(event.getY());
            int index = indexFromLocalPoint(localX, localY);
            setCaret(index, false);
            preferredColumn = lineColFromIndex(caretIndex)[1];
            draggingSelection = true;
            if (event.getWindow() != null) {
                event.getWindow().capturePointer(this);
            }
            event.stopPropagation();
            invalidate();
        });

        getEventManager().register(UiEvent.Type.POINTER_MOVE, (component, event) -> {
            if (!draggingSelection) {
                return;
            }
            int localX = toLocalX(event.getX());
            int localY = toLocalY(event.getY());
            int index = indexFromLocalPoint(localX, localY);
            setCaret(index, true);
            preferredColumn = lineColFromIndex(caretIndex)[1];
            event.stopPropagation();
            invalidate();
        });

        getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
            if (!draggingSelection) {
                return;
            }
            draggingSelection = false;
            if (event.getWindow() != null) {
                event.getWindow().releasePointer(this);
            }
            event.stopPropagation();
            invalidate();
        });
    }

    @Override
    public boolean onKeyPressed(java.awt.event.KeyEvent e) {
        if (!isFocused() || e == null) {
            return false;
        }

        boolean ctrl = e.isControlDown() || e.isMetaDown();
        boolean shift = e.isShiftDown();
        int key = e.getKeyCode();

        if (ctrl) {
            if (isCtrlShortcut(e, key, 'a', 1)) {
                selectionAnchor = 0;
                caretIndex = text.length();
                preferredColumn = lineColFromIndex(caretIndex)[1];
                invalidate();
                return true;
            }
            if (isCtrlShortcut(e, key, 'c', 3)) {
                copySelection();
                return true;
            }
            if (isCtrlShortcut(e, key, 'x', 24)) {
                cutSelection();
                return true;
            }
            if (isCtrlShortcut(e, key, 'v', 22)) {
                pasteFromClipboard();
                return true;
            }
            if (isCtrlShortcut(e, key, 'z', 26)) {
                if (shift) {
                    redo();
                } else {
                    undo();
                }
                return true;
            }
            if (isCtrlShortcut(e, key, 'y', 25)) {
                redo();
                return true;
            }
        }

        if (key == java.awt.event.KeyEvent.VK_LEFT) {
            setCaret(caretIndex - 1, shift);
            preferredColumn = lineColFromIndex(caretIndex)[1];
            invalidate();
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_RIGHT) {
            setCaret(caretIndex + 1, shift);
            preferredColumn = lineColFromIndex(caretIndex)[1];
            invalidate();
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_UP) {
            moveCaretVertical(-1, shift);
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_DOWN) {
            moveCaretVertical(1, shift);
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_HOME) {
            int[] lc = lineColFromIndex(caretIndex);
            setCaret(indexFromLineCol(lc[0], 0), shift);
            preferredColumn = 0;
            invalidate();
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_END) {
            int[] lc = lineColFromIndex(caretIndex);
            List<String> lines = getLines();
            String line = lines.get(Math.max(0, Math.min(lc[0], lines.size() - 1)));
            setCaret(indexFromLineCol(lc[0], line.length()), shift);
            preferredColumn = line.length();
            invalidate();
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_PAGE_UP) {
            moveCaretPage(-1, shift);
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_PAGE_DOWN) {
            moveCaretPage(1, shift);
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_BACK_SPACE) {
            backspace();
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_DELETE) {
            deleteForward();
            return true;
        }
        if (key == java.awt.event.KeyEvent.VK_ENTER) {
            replaceSelectionOrInsert("\n");
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyTyped(java.awt.event.KeyEvent e) {
        if (!isFocused() || e == null) {
            return false;
        }
        if (e.isControlDown() || e.isMetaDown() || e.isAltDown()) {
            return false;
        }

        char keyChar = e.getKeyChar();
        if (Character.isISOControl(keyChar)) {
            return false;
        }

        replaceSelectionOrInsert(String.valueOf(keyChar));
        return true;
    }

    @Override
    public void customGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int r = 10;

        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);

        g2.setColor(isFocused() ? focusBorder : border);
        g2.setStroke(new java.awt.BasicStroke(isFocused() ? 2.0f : 1.5f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);

        g2.setFont(UI_FONT);
        FontMetrics fm = g2.getFontMetrics();

        if (text.isEmpty()) {
            g2.setColor(placeholderColor);
            g2.drawString(placeholder, PADDING_X, PADDING_Y + 12);
        } else {
            List<String> lines = getLines();
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            int globalIndex = 0;
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                int baseline = PADDING_Y + 12 + (lineIndex * LINE_HEIGHT);
                if (baseline > getHeight() - 8) {
                    break;
                }

                String line = lines.get(lineIndex);
                int lineStart = globalIndex;
                int lineEnd = lineStart + line.length();

                if (hasSelection()) {
                    int paintStart = Math.max(selStart, lineStart);
                    int paintEnd = Math.min(selEnd, lineEnd);
                    if (paintStart < paintEnd) {
                        int startCol = paintStart - lineStart;
                        int endCol = paintEnd - lineStart;
                        int x = PADDING_X + fm.stringWidth(line.substring(0, startCol));
                        int w = fm.stringWidth(line.substring(startCol, endCol));
                        int top = Math.max(6, baseline - fm.getAscent());
                        int h = Math.max(16, fm.getHeight());
                        g2.setColor(selectionColor);
                        g2.fillRoundRect(x, top, Math.max(1, w), h, 6, 6);
                    }
                }

                g2.setColor(textColor);
                g2.drawString(line, PADDING_X, baseline);
                globalIndex = lineEnd + 1;
            }

            if (isFocused()) {
                int[] lc = lineColFromIndex(caretIndex);
                int lineIdx = lc[0];
                int col = lc[1];
                if (!lines.isEmpty()) {
                    lineIdx = Math.max(0, Math.min(lineIdx, lines.size() - 1));
                    String line = lines.get(lineIdx);
                    int baseline = PADDING_Y + 12 + (lineIdx * LINE_HEIGHT);
                    int caretX = Math.min(getWidth() - 8,
                            PADDING_X + fm.stringWidth(line.substring(0, Math.min(col, line.length()))) + 1);
                    int top = Math.max(8, baseline - fm.getAscent());
                    int bottom = Math.min(getHeight() - 8, top + fm.getHeight());
                    g2.setColor(focusBorder);
                    g2.drawLine(caretX, top, caretX, bottom);
                }
            }
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        setCaret(this.text.length(), false);
        preferredColumn = lineColFromIndex(caretIndex)[1];
        clearHistory();
        invalidate();
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        invalidate();
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
    }

    private void setCaret(int index, boolean keepSelection) {
        caretIndex = Math.max(0, Math.min(index, text.length()));
        if (!keepSelection) {
            selectionAnchor = caretIndex;
        }
    }

    private boolean hasSelection() {
        return caretIndex != selectionAnchor;
    }

    private int getSelectionStart() {
        return Math.min(caretIndex, selectionAnchor);
    }

    private int getSelectionEnd() {
        return Math.max(caretIndex, selectionAnchor);
    }

    private void clearSelection() {
        selectionAnchor = caretIndex;
    }

    private void pushUndoState() {
        undoStack.push(new EditState(text, caretIndex, selectionAnchor, preferredColumn));
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
    }

    private void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private void restoreState(EditState state) {
        if (state == null) {
            return;
        }
        text = state.text;
        caretIndex = Math.max(0, Math.min(state.caret, text.length()));
        selectionAnchor = Math.max(0, Math.min(state.anchor, text.length()));
        preferredColumn = Math.max(0, state.preferredColumn);
        invalidate();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(new EditState(text, caretIndex, selectionAnchor, preferredColumn));
        restoreState(undoStack.pop());
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(new EditState(text, caretIndex, selectionAnchor, preferredColumn));
        restoreState(redoStack.pop());
    }

    private void replaceSelectionOrInsert(String inserted) {
        if (inserted == null || inserted.isEmpty()) {
            return;
        }

        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        int replaced = selEnd - selStart;
        int allowed = maxLength - (text.length() - replaced);
        if (allowed <= 0) {
            return;
        }

        String normalized = inserted;
        if (normalized.length() > allowed) {
            normalized = normalized.substring(0, allowed);
        }

        pushUndoState();
        redoStack.clear();

        if (hasSelection()) {
            text = text.substring(0, selStart) + normalized + text.substring(selEnd);
            caretIndex = selStart + normalized.length();
            clearSelection();
        } else {
            text = text.substring(0, caretIndex) + normalized + text.substring(caretIndex);
            caretIndex += normalized.length();
            clearSelection();
        }

        preferredColumn = lineColFromIndex(caretIndex)[1];
        invalidate();
    }

    private void deleteSelectionIfAny() {
        if (!hasSelection()) {
            return;
        }
        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        text = text.substring(0, selStart) + text.substring(selEnd);
        caretIndex = selStart;
        clearSelection();
        preferredColumn = lineColFromIndex(caretIndex)[1];
    }

    private void backspace() {
        if (hasSelection()) {
            pushUndoState();
            redoStack.clear();
            deleteSelectionIfAny();
            invalidate();
            return;
        }
        if (caretIndex <= 0) {
            return;
        }
        pushUndoState();
        redoStack.clear();
        text = text.substring(0, caretIndex - 1) + text.substring(caretIndex);
        caretIndex -= 1;
        clearSelection();
        preferredColumn = lineColFromIndex(caretIndex)[1];
        invalidate();
    }

    private void deleteForward() {
        if (hasSelection()) {
            pushUndoState();
            redoStack.clear();
            deleteSelectionIfAny();
            invalidate();
            return;
        }
        if (caretIndex >= text.length()) {
            return;
        }
        pushUndoState();
        redoStack.clear();
        text = text.substring(0, caretIndex) + text.substring(caretIndex + 1);
        clearSelection();
        preferredColumn = lineColFromIndex(caretIndex)[1];
        invalidate();
    }

    private void copySelection() {
        if (!hasSelection()) {
            return;
        }
        String selected = text.substring(getSelectionStart(), getSelectionEnd());
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
        } catch (IllegalStateException ignored) {
        }
    }

    private void cutSelection() {
        if (!hasSelection()) {
            return;
        }
        copySelection();
        pushUndoState();
        redoStack.clear();
        deleteSelectionIfAny();
        invalidate();
    }

    private void pasteFromClipboard() {
        try {
            Object clip = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (clip instanceof String value && !value.isEmpty()) {
                replaceSelectionOrInsert(value.replace("\r", ""));
            }
        } catch (Exception ignored) {
        }
    }

    private void moveCaretVertical(int lineDelta, boolean keepSelection) {
        int[] lc = lineColFromIndex(caretIndex);
        int nextLine = lc[0] + lineDelta;
        int nextIndex = indexFromLineCol(nextLine, preferredColumn);
        setCaret(nextIndex, keepSelection);
        preferredColumn = lineColFromIndex(caretIndex)[1];
        invalidate();
    }

    private void moveCaretPage(int direction, boolean keepSelection) {
        int visibleLines = Math.max(1, (getHeight() - (PADDING_Y * 2)) / LINE_HEIGHT);
        int[] lc = lineColFromIndex(caretIndex);
        int nextLine = lc[0] + (visibleLines * direction);
        int nextIndex = indexFromLineCol(nextLine, preferredColumn);
        setCaret(nextIndex, keepSelection);
        preferredColumn = lineColFromIndex(caretIndex)[1];
        invalidate();
    }

    private List<String> getLines() {
        List<String> lines = new ArrayList<>();
        String[] split = text.split("\\n", -1);
        for (String line : split) {
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private int indexFromLocalPoint(int localX, int localY) {
        List<String> lines = getLines();
        int line = Math.max(0, Math.min(lines.size() - 1, (localY - PADDING_Y) / LINE_HEIGHT));
        String lineText = lines.get(line);
        int col = caretFromLocalX(lineText, localX);
        return indexFromLineCol(line, col);
    }

    private int caretFromLocalX(String lineText, int localX) {
        int x = Math.max(0, localX - PADDING_X);
        if (lineText.isEmpty() || x <= 0) {
            return 0;
        }
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(UI_FONT);
        for (int i = 0; i < lineText.length(); i++) {
            int left = fm.stringWidth(lineText.substring(0, i));
            int right = fm.stringWidth(lineText.substring(0, i + 1));
            if (x <= ((left + right) / 2)) {
                return i;
            }
        }
        return lineText.length();
    }

    private int[] lineColFromIndex(int index) {
        List<String> lines = getLines();
        int remaining = Math.max(0, Math.min(index, text.length()));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineLen = line.length();
            if (remaining <= lineLen) {
                return new int[] { i, remaining };
            }
            remaining -= (lineLen + 1);
        }
        int lastLine = Math.max(0, lines.size() - 1);
        return new int[] { lastLine, lines.get(lastLine).length() };
    }

    private int indexFromLineCol(int line, int col) {
        List<String> lines = getLines();
        int clampedLine = Math.max(0, Math.min(line, lines.size() - 1));
        int clampedCol = Math.max(0, Math.min(col, lines.get(clampedLine).length()));

        int idx = 0;
        for (int i = 0; i < clampedLine; i++) {
            idx += lines.get(i).length();
            idx += 1;
        }
        return idx + clampedCol;
    }

    private boolean isCtrlShortcut(java.awt.event.KeyEvent e, int keyCode, char expectedLower, int expectedCtrlChar) {
        if (e == null) {
            return false;
        }
        char ch = Character.toLowerCase(e.getKeyChar());
        if (ch == expectedLower || ch == (char) expectedCtrlChar) {
            return true;
        }

        return switch (expectedLower) {
            case 'a' -> keyCode == java.awt.event.KeyEvent.VK_A;
            case 'c' -> keyCode == java.awt.event.KeyEvent.VK_C;
            case 'x' -> keyCode == java.awt.event.KeyEvent.VK_X;
            case 'v' -> keyCode == java.awt.event.KeyEvent.VK_V;
            case 'z' -> keyCode == java.awt.event.KeyEvent.VK_Z;
            case 'y' -> keyCode == java.awt.event.KeyEvent.VK_Y;
            default -> false;
        };
    }
}
