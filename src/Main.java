import components.Button;
import components.CheckBox;
import components.Div;
import components.H;
import components.ImageComp;
import components.Label;
import components.ScrollView;
import components.TextArea;
import event.UiEvent;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import main.BaseComp;
import main.BaseWindow;

public class Main {
    private static final int CONTENT_PADDING = 14;
    private static MainApp demoApp;
    private static final String DEMO_IMAGE_PATH = "/home/jeff/Images/Fav/illustration-anime-city-v3.jpg";

    private static class TaskCard extends Div {
        private final CheckBox done;
        private final Button deleteButton;
        private final Label titleLabel;
        private final Label descriptionLabel;
        private boolean dragging;
        private boolean dragArmed;
        private int dragStartX;
        private int dragStartY;
        private int dragOffsetX;
        private int dragOffsetY;

        TaskCard(MainApp app, String text, String description, int width, int height) {
            super(0, 0, width, height, new Color(255, 255, 255, 200), 12);
            setDraggable(true);

            this.done = new CheckBox("Done", 16, height - 40, 92, 28, false);
            addChild(done);

            this.deleteButton = new Button("✕", width - 40, 16, 24, 24, () -> app.deleteTask(this));
            this.deleteButton.setBackground(new Color(245, 100, 100));
            addChild(deleteButton);

            this.titleLabel = new Label(text, 12, 10, Math.max(80, width - 54), 22);
            this.titleLabel.setColor(new Color(29, 35, 44));
            this.titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
            addChild(titleLabel);

            this.descriptionLabel = new Label(description, 12, 34, Math.max(80, width - 24), 22);
            this.descriptionLabel.setColor(new Color(116, 122, 131));
            this.descriptionLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
            addChild(descriptionLabel);

            getEventManager().register(UiEvent.Type.POINTER_DOWN, (component, event) -> {
                if (!isDragTarget(event.getTarget())) {
                    return;
                }
                dragArmed = true;
                dragStartX = event.getX();
                dragStartY = event.getY();
                dragOffsetX = event.getX() - getGlobalX();
                dragOffsetY = event.getY() - getGlobalY();
            });

            getEventManager().register(UiEvent.Type.POINTER_MOVE, (component, event) -> {
                if (dragArmed && !dragging) {
                    if (Math.abs(event.getX() - dragStartX) > 5 || Math.abs(event.getY() - dragStartY) > 5) {
                        dragging = true;
                        app.startDrag(this);
                    }
                }
                if (!dragging || event.getTarget() != this) {
                    return;
                }
                app.moveDrag(this, event.getX() - dragOffsetX, event.getY() - dragOffsetY);
                event.getWindow().requestRender();
            });

            getEventManager().register(UiEvent.Type.POINTER_UP, (component, event) -> {
                dragArmed = false;
                if (!dragging || event.getTarget() != this) {
                    return;
                }
                dragging = false;
                app.finishDrag(this, event.getX(), event.getY());
                event.getWindow().requestRender();
            });
        }

        private boolean isDragTarget(BaseComp target) {
            if (target == null) {
                return false;
            }
            if (target == done || target == deleteButton) {
                return false;
            }
            if (target.getParent() == done || target.getParent() == deleteButton) {
                return false;
            }
            return target == this || target.getParent() == this;
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            if (done == null || deleteButton == null || titleLabel == null || descriptionLabel == null) {
                return;
            }
            done.setBounds(16, height - 40, 92, 28);
            deleteButton.setBounds(width - 40, 16, 24, 24);
            titleLabel.setBounds(16, 16, Math.max(80, width - 60), 22);
            descriptionLabel.setBounds(16, 42, Math.max(80, width - 32), 22);
            setBackground(dragging ? new Color(250, 252, 255, 180) : new Color(255, 255, 255, 200));
        }
    }

    private static class TaskColumn extends Div {
        private static final int PADDING = 12;
        private static final int GAP = 12;
        private final ScrollView scroll;
        private final BaseComp list;

        TaskColumn(String title, int x, int y, int width, int height) {
            super(x, y, width, height, new Color(245, 247, 250, 150), 16);
            H heading = new H(title, PADDING, PADDING, width - (PADDING * 2), 30);
            addChild(heading);

            scroll = new ScrollView(0, 48, width, height - 50);
            addChild(scroll);
            list = scroll.getContent();
        }

        void addTask(TaskCard card) {
            list.addChild(card);
            relayoutCards();
        }

        void removeTask(TaskCard card) {
            list.removeChild(card);
            relayoutCards();
        }

        void relayoutCards() {
            int y = PADDING;
            int cardWidth = scroll.getWidth() - (PADDING * 2);
            for (BaseComp child : list.getChildren()) {
                child.setBounds(PADDING, y, cardWidth, 100);
                y += 100 + GAP;
            }
            scroll.setContentHeight(y + PADDING);
        }

        boolean containsPoint(int gx, int gy) {
            return scroll.containsGlobalPoint(gx, gy);
        }

        int localYInList(int globalY) {
            return globalY - list.getGlobalY();
        }

        void insertTaskAt(TaskCard card, int localY) {
            List<BaseComp> items = new ArrayList<>(list.getChildrenList());
            int index = items.size();
            for (int i = 0; i < items.size(); i++) {
                BaseComp item = items.get(i);
                if (localY < item.getY() + (item.getHeight() / 2)) {
                    index = i;
                    break;
                }
            }
            list.addChild(card);
            list.moveChild(list.getChildrenList().size() - 1, index);
            relayoutCards();
        }

        BaseComp getList() {
            return list;
        }
    }

    private static class MainApp {
        private final BaseWindow window;
        private final BaseComp content;
        private final Div overlay;
        private final List<TaskColumn> columns;

        private Div toolbar;
        private Div board;
        private Button addBtn;
        private Button openModalBtn;
        private Button openChildWindowBtn;
        private CheckBox perfMode;
        private TextArea notes;
        private ImageComp preview;

        private int taskCounter;
        private TaskCard draggingCard;
        private TaskColumn originColumn;

        MainApp(BaseWindow window) {
            this.window = window;
            this.content = window.getContent();
            this.overlay = new Div(0, 0, content.getWidth(), content.getHeight(), new Color(0, 0, 0, 0), 0) {
                @Override
                public boolean containsGlobalPoint(int globalX, int globalY) {
                    return !getChildrenList().isEmpty() && super.containsGlobalPoint(globalX, globalY);
                }
            };
            this.columns = new ArrayList<>();
            this.taskCounter = 1;
            buildUI();
            window.addResizeListener(this::relayoutRoot);
        }

        private void buildUI() {
            int C_WIDTH = content.getWidth();
            int C_HEIGHT = content.getHeight();
            
            // Full screen background for main window content
            content.setStyleManager(new style.StyleManager(new Color(245, 246, 248), 0, C_WIDTH, C_HEIGHT, 0, 0, "absolute"));
            
            // Background Image covering everything with a dark glass effect
            preview = new ImageComp(DEMO_IMAGE_PATH, 0, 0, C_WIDTH, C_HEIGHT);
            preview.setAlpha(0.95f);
            content.addChild(preview);
            
            // Glasspane over the image to ensure readability
            Div glass = new Div(0, 0, C_WIDTH, C_HEIGHT, new Color(20, 25, 35, 70), 0);
            content.addChild(glass);

            // Modern Toolbar / Header
            toolbar = new Div(CONTENT_PADDING, CONTENT_PADDING, C_WIDTH - (CONTENT_PADDING * 2), 90,
                    new Color(30, 36, 48, 160), 16);
            content.addChild(toolbar);

            H title = new H("Nexus Task Studio", 20, 20, 300, 28);
            title.setColor(Color.WHITE);
            toolbar.addChild(title);

            Label subtitle = new Label("High-performance UI Renderer V2", 20, 52, 300, 20);
            subtitle.setColor(new Color(150, 160, 175));
            toolbar.addChild(subtitle);

            // Modern actions aligned to the right or next to title
            addBtn = new Button("+ New Task", 340, 20, 120, 36, this::addTaskToBacklog);
            addBtn.setBackground(new Color(66, 133, 244));
            toolbar.addChild(addBtn);

            openModalBtn = new Button("View Stats", 470, 20, 110, 36, this::openTaskModal);
            openModalBtn.setBackground(new Color(60, 70, 85));
            toolbar.addChild(openModalBtn);

            perfMode = new CheckBox("Eco Mode", 590, 20, 130, 36, window.getFps() == 0);
            toolbar.addChild(perfMode);

            // Board Container for Kanban columns
            board = new Div(CONTENT_PADDING, 100 + CONTENT_PADDING, C_WIDTH - (CONTENT_PADDING * 2), C_HEIGHT - (100 + CONTENT_PADDING * 2), new Color(0, 0, 0, 0), 0);
            content.addChild(board);

            int gap = 16;
            int boardWidth = board.getWidth();
            int boardHeight = board.getHeight();
            int colWidth = (boardWidth - (gap * 2)) / 3;

            TaskColumn backlog = new TaskColumn("📌 Backlog", 0, 0, colWidth, boardHeight);
            TaskColumn doing = new TaskColumn("🚀 In Progress", colWidth + gap, 0, colWidth, boardHeight);
            TaskColumn done = new TaskColumn("✅ Done", (colWidth + gap) * 2, 0, colWidth, boardHeight);

            columns.add(backlog);
            columns.add(doing);
            columns.add(done);

            board.addChild(backlog);
            board.addChild(doing);
            board.addChild(done);

            overlay.setBounds(0, 0, C_WIDTH, C_HEIGHT);
            content.addChild(overlay);

            seedTasks(backlog, doing, done);
            
            // Add a resize listener on window to adjust the background and children
            relayoutRoot();
            window.requestRender();
        }

        private void relayoutRoot() {
            if (toolbar == null || board == null) {
                return;
            }

            int W = content.getWidth();
            int H = content.getHeight();

            // Resize images and backgrounds
            if (preview != null) {
                preview.setBounds(0, 0, W, H);
            }
            // The glass pane is the 2nd child
            BaseComp glass = content.getChildren()[1];
            if (glass instanceof Div) {
                glass.setBounds(0, 0, W, H);
            }

            toolbar.setBounds(CONTENT_PADDING, CONTENT_PADDING, Math.max(700, W - (CONTENT_PADDING * 2)), 90);

            int boardY = toolbar.getY() + toolbar.getHeight() + CONTENT_PADDING;
            int boardW = Math.max(800, W - (CONTENT_PADDING * 2));
            int boardH = Math.max(300, H - boardY - CONTENT_PADDING);
            
            board.setBounds(CONTENT_PADDING, boardY, boardW, boardH);

            int gap = 16;
            int colWidth = (boardW - (gap * 2)) / 3;

            if (columns.size() >= 3) {
                columns.get(0).setBounds(0, 0, colWidth, boardH);
                columns.get(1).setBounds(colWidth + gap, 0, colWidth, boardH);
                columns.get(2).setBounds((colWidth + gap) * 2, 0, colWidth, boardH);

                for (TaskColumn column : columns) {
                    BaseComp[] children = column.getChildren();
                    if (children.length >= 2) {
                        if (children[0] instanceof H heading) {
                            heading.setBounds(12, 12, column.getWidth() - 24, 30);
                        }
                        if (children[1] instanceof ScrollView scrollView) {
                            scrollView.setBounds(0, 48, column.getWidth(), column.getHeight() - 50);
                            column.relayoutCards();
                        }
                    }
                }
            }

            overlay.setBounds(0, 0, W, H);
        }

        private void openTaskModal() {
            Div modalCard = new Div(0, 0, 460, 260, new Color(252, 252, 253), 16);

            H title = new H("Create Task", 16, 14, 220, 32);
            title.setFont(new Font("Dialog", Font.BOLD, 20));
            modalCard.addChild(title);

            TextArea details = new TextArea(
                    "Layered modal demo:\n- The modal is above all content\n- UI keeps a stack of layers\n- Click close to pop top layer",
                    16, 54, 426, 120);
            modalCard.addChild(details);

            Button close = new Button("Close", 332, 212, 110, 32, window::closeTopLayer);
            close.setBackground(new Color(215, 80, 80));
            modalCard.addChild(close);

            window.openModal(modalCard);
        }

        private void openSecondaryWindow() {
            BaseWindow child = window.openChildWindow("Secondary - Inspector", 640, 420, window.getFps());
            BaseComp childContent = child.getContent();

            Div panel = new Div(14, 14, childContent.getWidth() - 28, childContent.getHeight() - 28,
                    new Color(239, 244, 248), 14);
            childContent.addChild(panel);

            H title = new H("Inspector", 14, 12, 220, 30);
            panel.addChild(title);

            Label desc = new Label("Secondary windows can host independent UI trees.", 14, 46, 420, 22);
            desc.setColor(new Color(89, 97, 108));
            panel.addChild(desc);

            Button openModal = new Button("Open Local Modal", 14, 80, 160, 32, () -> {
                Div modal = new Div(0, 0, 360, 180, Color.WHITE, 14);
                modal.addChild(new H("Local Modal", 14, 10, 220, 28));
                modal.addChild(new Label("This modal belongs to the secondary window.", 14, 46, 300, 20));
                Button close = new Button("Close", 250, 136, 90, 30, child::closeTopLayer);
                close.setBackground(new Color(215, 80, 80));
                modal.addChild(close);
                child.openModal(modal);
            });
            panel.addChild(openModal);

            child.requestRender();
        }

        private void seedTasks(TaskColumn backlog, TaskColumn doing, TaskColumn done) {
            backlog.addTask(new TaskCard(this, "Design token palette", "Unifier les couleurs et rayons",
                    backlog.getWidth() - 40, 96));
            backlog.addTask(
                    new TaskCard(this, "Keyboard shortcuts", "Ajouter Ctrl+N et Ctrl+F", backlog.getWidth() - 40, 96));
            doing.addTask(new TaskCard(this, "Refactor event phases", "Capture + target + bubble",
                    doing.getWidth() - 40, 96));
            done.addTask(
                    new TaskCard(this, "Passive/active rendering", "fps switch finalise", done.getWidth() - 40, 96));
        }

        void addTaskToBacklog() {
            Div modal = new Div(0, 0, 400, 240, new Color(255, 255, 255), 16);
            
            H title = new H("Create New Task", 20, 16, 300, 30);
            modal.addChild(title);
            
            Label descLabel = new Label("Task Description (Mock Input):", 20, 56, 300, 20);
            descLabel.setColor(new Color(100, 110, 120));
            modal.addChild(descLabel);
            
            TextArea mockInput = new TextArea("My awesome new task idea...\n(Keyboard input coming soon)", 20, 86, 360, 80);
            modal.addChild(mockInput);
            
            Button confirmBtn = new Button("Add Task", 250, 186, 130, 36, () -> {
                TaskColumn backlog = columns.get(0);
                TaskCard card = new TaskCard(this, "New Task #" + taskCounter++, "Created from Modal",
                        backlog.getWidth() - 40, 96);
                backlog.addTask(card);
                window.closeTopLayer();
            });
            confirmBtn.setBackground(new Color(46, 177, 105));
            modal.addChild(confirmBtn);
            
            Button cancelBtn = new Button("Cancel", 140, 186, 100, 36, window::closeTopLayer);
            cancelBtn.setBackground(new Color(200, 205, 215));
            cancelBtn.setForeground(new Color(80, 80, 80));
            modal.addChild(cancelBtn);

            window.openModal(modal);
        }

        void deleteTask(TaskCard card) {
            for (TaskColumn col : columns) {
                if (card.getParent() == col.getList()) {
                    col.removeTask(card);
                    break;
                }
            }
            if (card.getParent() == overlay) {
                overlay.removeChild(card);
            }
            window.requestRender();
        }

        void startDrag(TaskCard card) {
            draggingCard = card;
            originColumn = findCardColumn(card);
            if (originColumn == null) {
                return;
            }
            int gx = card.getGlobalX();
            int gy = card.getGlobalY();
            originColumn.removeTask(card);
            overlay.addChild(card);
            card.setBounds(gx - content.getGlobalX(), gy - content.getGlobalY(), card.getWidth(), card.getHeight());
            window.requestRender();
        }

        void moveDrag(TaskCard card, int globalX, int globalY) {
            if (draggingCard != card || card.getParent() != overlay) {
                return;
            }
            card.setBounds(globalX - content.getGlobalX(), globalY - content.getGlobalY(), card.getWidth(),
                    card.getHeight());
        }

        void finishDrag(TaskCard card, int dropX, int dropY) {
            if (draggingCard != card) {
                return;
            }
            TaskColumn target = findColumnAt(dropX, dropY);
            if (target == null) {
                target = originColumn != null ? originColumn : columns.get(0);
            }
            overlay.removeChild(card);
            target.insertTaskAt(card, target.localYInList(dropY));
            draggingCard = null;
            originColumn = null;
            window.requestRender();
        }

        private TaskColumn findCardColumn(TaskCard card) {
            for (TaskColumn col : columns) {
                if (card.getParent() == col.getList()) {
                    return col;
                }
            }
            return null;
        }

        private TaskColumn findColumnAt(int gx, int gy) {
            for (TaskColumn col : columns) {
                if (col.containsPoint(gx, gy)) {
                    return col;
                }
            }
            return null;
        }
    }

    public static void main(String[] args) {
        int fps = 0;
        if (args.length > 0) {
            try {
                fps = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                fps = 0;
            }
        }

        BaseWindow window = new BaseWindow("UI Renderer V2", 1180, 760, fps);
        demoApp = new MainApp(window);
        if (demoApp == null) {
            throw new IllegalStateException("Demo app initialization failed");
        }
        window.show();
    }
}
