package apps.worksuite;

import components.Button;
import components.CheckBox;
import components.ConfirmDialog;
import components.Div;
import components.FormModal;
import components.H;
import components.Label;
import components.LiveClockLabel;
import components.NavMenuBar;
import components.ScrollView;
import components.SegmentedSelect;
import components.SelectInput;
import components.TextAreaInput;
import components.TextField;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import main.BaseComp;
import main.BaseWindow;

public class WorkSuiteApp {
    private enum Page {
        DASHBOARD,
        NOTES,
        LOGBOOK,
        SETTINGS
    }

    private static class TaskItem {
        String title;
        String description;
        String status;
        boolean highPriority;
        String owner;
        LocalDateTime createdAt;

        TaskItem(String title, String description, String status, boolean highPriority, String owner) {
            this.title = title;
            this.description = description;
            this.status = status;
            this.highPriority = highPriority;
            this.owner = owner;
            this.createdAt = LocalDateTime.now();
        }
    }

    private static class NoteItem {
        String title;
        String content;
        LocalDateTime updatedAt;

        NoteItem(String title, String content) {
            this.title = title;
            this.content = content;
            this.updatedAt = LocalDateTime.now();
        }
    }

    private static class LogEntry {
        String category;
        String message;
        LocalDateTime time;

        LogEntry(String category, String message) {
            this.category = category;
            this.message = message;
            this.time = LocalDateTime.now();
        }
    }

    private static class App {
        private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        private static final int HEADER_COMPACT_BREAKPOINT = 980;

        private final BaseWindow window;
        private final BaseComp content;

        private final List<TaskItem> tasks;
        private final List<NoteItem> notes;
        private final List<LogEntry> logbook;

        private Div topBar;
        private Div body;
        private Div sidePanel;
        private Div viewHost;
        private Div statusBar;
        private NavMenuBar menuBar;
        private Button burgerButton;
        private Div mobileMenuPanel;
        private final Map<Page, Button> mobileMenuButtons;
        private Div floatingClockCard;
        private LiveClockLabel floatingClockLabel;
        private Label statusLabel;
        private Label statsTasks;
        private Label statsNotes;
        private Label statsOpen;

        private Div dashboardView;
        private Button dashboardAddTaskBtn;
        private ScrollView taskScroll;
        private BaseComp taskList;

        private Div notesView;
        private Button notesAddNoteBtn;
        private ScrollView noteScroll;
        private BaseComp noteList;

        private Div logbookView;
        private Button logbookAddBtn;
        private Button logbookClearBtn;
        private ScrollView logScroll;
        private BaseComp logList;

        private Div settingsView;
        private Label settingsInfo;

        private Page currentPage;
        private boolean compactHeader;
        private boolean mobileMenuOpen;

        App(BaseWindow window) {
            this.window = window;
            this.content = window.getContent();
            this.tasks = new ArrayList<>();
            this.notes = new ArrayList<>();
            this.logbook = new ArrayList<>();
            this.mobileMenuButtons = new EnumMap<>(Page.class);
            this.currentPage = Page.DASHBOARD;
            this.compactHeader = false;
            this.mobileMenuOpen = false;

            buildShell();
            seedData();
            refreshAllViews();
            switchTo(Page.DASHBOARD);

            this.window.addResizeListener(this::relayout);
            relayout();
            this.window.invalidateAll();
            this.window.requestRenderIfNeeded();
        }

        private void buildShell() {
            content.setStyleManager(
                    new style.StyleManager(new Color(236, 240, 246), 0, content.getWidth(), content.getHeight(), 0,
                            0, "absolute"));

            topBar = new Div(0, 0, content.getWidth(), 78, new Color(20, 30, 44), 0);
            content.addChild(topBar);

            H title = new H("WorkSuite Pro", 18, 16, 280, 28);
            title.setColor(Color.WHITE);
            topBar.addChild(title);

            Label subtitle = new Label("Dashboard, journal de notes, journal de bord et paramètres", 18, 44, 620, 20);
            subtitle.setColor(new Color(167, 185, 208));
            subtitle.setFont(new Font("Dialog", Font.PLAIN, 13));
            topBar.addChild(subtitle);

            menuBar = new NavMenuBar(360, 16, 680, 46);
            menuBar.addItem(Page.DASHBOARD.name(), "Dashboard");
            menuBar.addItem(Page.NOTES.name(), "Journal de Notes");
            menuBar.addItem(Page.LOGBOOK.name(), "Journal de Bord");
            menuBar.addItem(Page.SETTINGS.name(), "Paramètres");
            menuBar.setSelectionListener(this::onMenuSelection);
            topBar.addChild(menuBar);

            burgerButton = new Button("≡", content.getWidth() - 58, 16, 42, 42, () -> {
                mobileMenuOpen = !mobileMenuOpen;
                syncResponsiveMenuState();
            });
            burgerButton.setBackground(new Color(58, 74, 99));
            burgerButton.setForeground(Color.WHITE);
            burgerButton.setVisible(false);
            topBar.addChild(burgerButton);

            mobileMenuPanel = new Div(content.getWidth() - 244, 78, 232, 192, new Color(24, 35, 51, 240), 12);
            mobileMenuPanel.setVisible(false);
            buildMobileMenuButtons();

            topBar.addWidthContainerQuery(HEADER_COMPACT_BREAKPOINT, () -> applyHeaderMode(true),
                    () -> applyHeaderMode(false));

            body = new Div(12, 88, content.getWidth() - 24, content.getHeight() - 132, new Color(0, 0, 0, 0), 0);
            content.addChild(body);

            sidePanel = new Div(0, 0, 260, body.getHeight(), new Color(250, 253, 255), 16);
            body.addChild(sidePanel);

            H sideTitle = new H("Pilotage", 16, 16, 220, 24);
            sideTitle.setColor(new Color(20, 37, 57));
            sidePanel.addChild(sideTitle);

            statsTasks = new Label("Tâches: 0", 16, 58, 220, 22);
            statsNotes = new Label("Notes: 0", 16, 82, 220, 22);
            statsOpen = new Label("Ouvertes: 0", 16, 106, 220, 22);
            statsTasks.setColor(new Color(62, 76, 94));
            statsNotes.setColor(new Color(62, 76, 94));
            statsOpen.setColor(new Color(62, 76, 94));
            sidePanel.addChild(statsTasks);
            sidePanel.addChild(statsNotes);
            sidePanel.addChild(statsOpen);

            Button quickTask = new Button("+ Ajouter une tâche", 16, 152, 220, 34, this::openTaskModal);
            quickTask.setBackground(new Color(52, 132, 255));
            sidePanel.addChild(quickTask);

            Button quickNote = new Button("+ Ajouter une note", 16, 194, 220, 34, this::openNoteModal);
            quickNote.setBackground(new Color(91, 125, 255));
            sidePanel.addChild(quickNote);

            Button quickLog = new Button("+ Ajouter un log", 16, 236, 220, 34, this::openLogModal);
            quickLog.setBackground(new Color(35, 163, 122));
            sidePanel.addChild(quickLog);

            viewHost = new Div(274, 0, body.getWidth() - 274, body.getHeight(), new Color(0, 0, 0, 0), 0);
            body.addChild(viewHost);

            statusBar = new Div(12, content.getHeight() - 40, content.getWidth() - 24, 28, new Color(225, 232, 242),
                    12);
            content.addChild(statusBar);

            statusLabel = new Label("Ready", 12, 4, 1000, 20);
            statusLabel.setColor(new Color(57, 67, 81));
            statusLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
            statusBar.addChild(statusLabel);

            floatingClockCard = new Div(content.getWidth() - 308, content.getHeight() - 132, 296, 80,
                    new Color(22, 33, 48, 235), 16);
            content.addChild(floatingClockCard);

            Label clockTitle = new Label("Heure locale", 14, 8, 180, 18);
            clockTitle.setColor(new Color(171, 190, 214));
            clockTitle.setFont(new Font("Dialog", Font.BOLD, 12));
            floatingClockCard.addChild(clockTitle);

            floatingClockLabel = new LiveClockLabel(14, 26, 268, 44, "HH:mm:ss");
            floatingClockLabel.setColor(new Color(241, 246, 255));
            floatingClockLabel.setFont(new Font("Dialog", Font.BOLD, 32));
            floatingClockCard.addChild(floatingClockLabel);

            dashboardView = buildDashboardView();
            notesView = buildNotesView();
            logbookView = buildLogbookView();
            settingsView = buildSettingsView();
        }

        private void buildMobileMenuButtons() {
            clearChildren(mobileMenuPanel);
            addMobileMenuButton(Page.DASHBOARD, "Dashboard", 10);
            addMobileMenuButton(Page.NOTES, "Journal de Notes", 52);
            addMobileMenuButton(Page.LOGBOOK, "Journal de Bord", 94);
            addMobileMenuButton(Page.SETTINGS, "Paramètres", 136);
            refreshMobileMenuSelection();
        }

        private void addMobileMenuButton(Page page, String label, int y) {
            Button item = new Button(label, 10, y, 212, 34, () -> {
                onMenuSelection(page.name());
                mobileMenuOpen = false;
                syncResponsiveMenuState();
            });
            item.setBackground(new Color(53, 64, 85));
            item.setForeground(new Color(230, 236, 245));
            mobileMenuButtons.put(page, item);
            mobileMenuPanel.addChild(item);
        }

        private void applyHeaderMode(boolean compact) {
            if (compactHeader == compact) {
                return;
            }
            compactHeader = compact;
            if (!compact) {
                mobileMenuOpen = false;
            }
            syncResponsiveMenuState();
        }

        private void syncResponsiveMenuState() {
            menuBar.setVisible(!compactHeader);
            burgerButton.setVisible(compactHeader);
            boolean showMobileMenu = compactHeader && mobileMenuOpen;
            if (showMobileMenu) {
                if (mobileMenuPanel.getParent() == null) {
                    window.openLayer(mobileMenuPanel);
                }
                mobileMenuPanel.setVisible(true);
            } else {
                if (mobileMenuPanel.getParent() != null) {
                    mobileMenuPanel.getParent().removeChild(mobileMenuPanel);
                }
                mobileMenuPanel.setVisible(false);
            }
            refreshMobileMenuSelection();
            content.invalidate();
        }

        private void refreshMobileMenuSelection() {
            for (Page p : Page.values()) {
                Button item = mobileMenuButtons.get(p);
                if (item == null) {
                    continue;
                }
                boolean active = p == currentPage;
                item.setBackground(active ? new Color(70, 145, 255) : new Color(53, 64, 85));
                item.setForeground(active ? Color.WHITE : new Color(230, 236, 245));
                item.invalidate();
            }
        }

        private Div buildDashboardView() {
            Div view = new Div(0, 0, viewHost.getWidth(), viewHost.getHeight(), new Color(0, 0, 0, 0), 0);

            H heading = new H("Dashboard Projet", 12, 8, 320, 28);
            heading.setColor(new Color(24, 36, 52));
            view.addChild(heading);

            dashboardAddTaskBtn = new Button("Nouvelle tâche", 420, 8, 160, 34, this::openTaskModal);
            dashboardAddTaskBtn.setBackground(new Color(52, 132, 255));
            view.addChild(dashboardAddTaskBtn);

            taskScroll = new ScrollView(0, 48, view.getWidth(), view.getHeight() - 52);
            view.addChild(taskScroll);
            taskList = taskScroll.getContent();
            return view;
        }

        private Div buildNotesView() {
            Div view = new Div(0, 0, viewHost.getWidth(), viewHost.getHeight(), new Color(0, 0, 0, 0), 0);

            H heading = new H("Journal de Notes", 12, 8, 360, 28);
            heading.setColor(new Color(24, 36, 52));
            view.addChild(heading);

            notesAddNoteBtn = new Button("Nouvelle note", 420, 8, 160, 34, this::openNoteModal);
            notesAddNoteBtn.setBackground(new Color(91, 125, 255));
            view.addChild(notesAddNoteBtn);

            noteScroll = new ScrollView(0, 48, view.getWidth(), view.getHeight() - 52);
            view.addChild(noteScroll);
            noteList = noteScroll.getContent();
            return view;
        }

        private Div buildLogbookView() {
            Div view = new Div(0, 0, viewHost.getWidth(), viewHost.getHeight(), new Color(0, 0, 0, 0), 0);

            H heading = new H("Journal de Bord", 12, 8, 360, 28);
            heading.setColor(new Color(24, 36, 52));
            view.addChild(heading);

            logbookAddBtn = new Button("Nouvelle entrée", 420, 8, 160, 34, this::openLogModal);
            logbookAddBtn.setBackground(new Color(35, 163, 122));
            view.addChild(logbookAddBtn);

            logbookClearBtn = new Button("Effacer", 590, 8, 110, 34, () -> {
                openConfirmModal("Vider le journal",
                        "Confirmer la suppression de toutes les entrées du journal de bord ?",
                        () -> {
                            logbook.clear();
                            appendLog("SYSTEM", "Journal de bord vidé");
                            refreshLogbookView();
                            setStatus("Journal vidé");
                        });
            });
            logbookClearBtn.setBackground(new Color(210, 98, 98));
            view.addChild(logbookClearBtn);

            logScroll = new ScrollView(0, 48, view.getWidth(), view.getHeight() - 52);
            view.addChild(logScroll);
            logList = logScroll.getContent();
            return view;
        }

        private Div buildSettingsView() {
            Div view = new Div(0, 0, viewHost.getWidth(), viewHost.getHeight(), new Color(0, 0, 0, 0), 0);

            H heading = new H("Paramètres", 12, 8, 280, 28);
            heading.setColor(new Color(24, 36, 52));
            view.addChild(heading);

            Label appNameLabel = new Label("Nom de l'espace", 16, 56, 220, 20);
            appNameLabel.setColor(new Color(65, 76, 92));
            view.addChild(appNameLabel);

            TextField appName = new TextField(16, 80, 320, 34);
            appName.setText("WorkSuite Pro");
            view.addChild(appName);

            Label ownerLabel = new Label("Responsable", 16, 126, 220, 20);
            ownerLabel.setColor(new Color(65, 76, 92));
            view.addChild(ownerLabel);

            TextField owner = new TextField(16, 150, 240, 34);
            owner.setText("Jeff");
            view.addChild(owner);

            Label modeLabel = new Label("Mode Journal", 16, 196, 220, 20);
            modeLabel.setColor(new Color(65, 76, 92));
            view.addChild(modeLabel);

            SelectInput mode = new SelectInput(16, 220, 260, 34);
            mode.setOptions(List.of("Normal", "Audit", "Verbose"));
            mode.setSelectedOption("Normal");
            view.addChild(mode);

            CheckBox notify = new CheckBox("Notifications locales", 16, 270, 260, 28, true);
            view.addChild(notify);

            settingsInfo = new Label("", 16, 330, 740, 22);
            settingsInfo.setColor(new Color(70, 86, 105));
            view.addChild(settingsInfo);

            Button save = new Button("Sauvegarder", 16, 368, 160, 34, () -> {
                String message = "Paramètres sauvegardés: espace='" + appName.getText() + "', owner='" + owner.getText()
                        + "', mode=" + mode.getSelectedOption() + ", notifications="
                        + (notify.isChecked() ? "on" : "off");
                settingsInfo.setText(message);
                settingsInfo.invalidate();
                appendLog("SETTINGS", message);
                setStatus("Paramètres enregistrés");
            });
            save.setBackground(new Color(66, 133, 244));
            view.addChild(save);

            Button runtimeHelp = new Button("Aide Runtime Scroll", 188, 368, 220, 34, () -> {
                String msg = "Runtime: -Dui.scroll.xFactor=1.0 -Dui.scroll.yFactor=1.0 -Dui.scroll.latchMs=220";
                settingsInfo.setText(msg);
                settingsInfo.invalidate();
                setStatus("Aide runtime scroll affichée");
            });
            runtimeHelp.setBackground(new Color(60, 90, 130));
            view.addChild(runtimeHelp);

            return view;
        }

        private void onMenuSelection(String key) {
            try {
                switchTo(Page.valueOf(key));
            } catch (IllegalArgumentException ignored) {
                switchTo(Page.DASHBOARD);
            }
        }

        private void switchTo(Page page) {
            this.currentPage = page;
            clearChildren(viewHost);

            Div view = switch (page) {
                case DASHBOARD -> dashboardView;
                case NOTES -> notesView;
                case LOGBOOK -> logbookView;
                case SETTINGS -> settingsView;
            };
            view.setBounds(0, 0, viewHost.getWidth(), viewHost.getHeight());
            viewHost.addChild(view);
            menuBar.setSelectedKey(page.name());
            refreshMobileMenuSelection();
            setStatus("Vue active: " + page.name());
        }

        private void relayout() {
            int w = content.getWidth();
            int h = content.getHeight();

            topBar.setBounds(0, 0, w, 78);
            int rightReserved = compactHeader ? 58 : 18;
            int menuX = 360;
            int menuWidth = Math.max(120, w - menuX - rightReserved - 12);
            menuBar.setBounds(menuX, 16, menuWidth, 46);
            burgerButton.setBounds(w - 58, 16, 42, 42);
            mobileMenuPanel.setBounds(Math.max(12, w - 244), 78, 232, 192);

            body.setBounds(12, 88, w - 24, h - 132);
            sidePanel.setBounds(0, 0, 260, body.getHeight());
            viewHost.setBounds(274, 0, body.getWidth() - 274, body.getHeight());

            statusBar.setBounds(12, h - 40, w - 24, 28);
            statusLabel.setBounds(12, 4, statusBar.getWidth() - 24, 20);

            int clockW = Math.min(340, Math.max(232, (int) (w * 0.26)));
            int clockH = Math.max(74, (int) (h * 0.1));
            int clockX = Math.max(12, w - clockW - 12);
            int clockY = Math.max(86, h - 40 - clockH - 10);
            floatingClockCard.setBounds(clockX, clockY, clockW, clockH);
            floatingClockLabel.setBounds(14, 22, Math.max(120, clockW - 24), Math.max(40, clockH - 28));

            dashboardView.setBounds(0, 0, viewHost.getWidth(), viewHost.getHeight());
            relayoutDashboardHeader();

            notesView.setBounds(0, 0, viewHost.getWidth(), viewHost.getHeight());
            relayoutNotesHeader();

            logbookView.setBounds(0, 0, viewHost.getWidth(), viewHost.getHeight());
            relayoutLogbookHeader();

            settingsView.setBounds(0, 0, viewHost.getWidth(), viewHost.getHeight());

            refreshAllViews();
            switchTo(currentPage);
            applyHeaderMode(w <= HEADER_COMPACT_BREAKPOINT);
            syncResponsiveMenuState();
        }

        private void relayoutDashboardHeader() {
            int viewW = dashboardView.getWidth();
            int actionX = Math.max(12, viewW - 160 - 12);
            int actionY = viewW < 520 ? 46 : 8;
            int scrollTop = viewW < 520 ? 88 : 48;
            dashboardAddTaskBtn.setBounds(actionX, actionY, 160, 34);
            taskScroll.setBounds(0, scrollTop, viewW, Math.max(40, dashboardView.getHeight() - scrollTop - 4));
        }

        private void relayoutNotesHeader() {
            int viewW = notesView.getWidth();
            int actionX = Math.max(12, viewW - 160 - 12);
            int actionY = viewW < 520 ? 46 : 8;
            int scrollTop = viewW < 520 ? 88 : 48;
            notesAddNoteBtn.setBounds(actionX, actionY, 160, 34);
            noteScroll.setBounds(0, scrollTop, viewW, Math.max(40, notesView.getHeight() - scrollTop - 4));
        }

        private void relayoutLogbookHeader() {
            int viewW = logbookView.getWidth();
            boolean stacked = viewW < 360;
            int clearX = Math.max(12, viewW - 110 - 12);
            if (stacked) {
                int addX = Math.max(12, viewW - 160 - 12);
                logbookClearBtn.setBounds(clearX, 8, 110, 34);
                logbookAddBtn.setBounds(addX, 46, 160, 34);
                logScroll.setBounds(0, 88, viewW, Math.max(40, logbookView.getHeight() - 92));
                return;
            }

            int addX = Math.max(12, clearX - 10 - 160);
            logbookAddBtn.setBounds(addX, 8, 160, 34);
            logbookClearBtn.setBounds(clearX, 8, 110, 34);
            logScroll.setBounds(0, 48, viewW, Math.max(40, logbookView.getHeight() - 52));
        }

        private void seedData() {
            tasks.add(
                    new TaskItem("Préparer sprint", "Organiser les stories et dépendances", "Backlog", true, "@jeff"));
            tasks.add(new TaskItem("Refactor moteur UI", "Stabiliser invalidation et scroll", "In Progress", true,
                    "@core"));
            tasks.add(new TaskItem("Démo client", "Assembler écran, notes et journal", "Done", false, "@pm"));

            notes.add(new NoteItem("Kickoff", "Objectif: livrer une app complète et fluide sur Linux."));
            notes.add(new NoteItem("Risque", "Tester trackpad horizontal sur plusieurs machines."));

            appendLog("SYSTEM", "Initialisation de WorkSuite Pro");
            appendLog("TASK", "3 tâches d'exemple chargées");
            appendLog("NOTE", "2 notes initiales chargées");
        }

        private void refreshAllViews() {
            refreshDashboardView();
            refreshNotesView();
            refreshLogbookView();
            refreshStats();
        }

        private void refreshDashboardView() {
            clearChildren(taskList);
            int y = 12;
            int cardWidth = Math.max(400, taskScroll.getWidth() - 30);

            for (TaskItem task : tasks) {
                Div card = new Div(12, y, cardWidth, 118, task.highPriority ? new Color(255, 245, 242) : Color.WHITE,
                        14);
                taskList.addChild(card);

                Label title = new Label(task.title, 14, 12, cardWidth - 200, 24);
                title.setFont(new Font("Dialog", Font.BOLD, 15));
                title.setColor(new Color(33, 42, 56));
                card.addChild(title);

                String metaText = task.status + " | " + task.owner + " | " + TS_FORMAT.format(task.createdAt);
                Label meta = new Label(metaText, 14, 36, cardWidth - 180, 18);
                meta.setColor(new Color(93, 104, 122));
                meta.setFont(new Font("Dialog", Font.PLAIN, 12));
                card.addChild(meta);

                Label desc = new Label(task.description, 14, 58, cardWidth - 30, 20);
                desc.setColor(new Color(82, 90, 104));
                card.addChild(desc);

                Button step = new Button(nextStatusLabel(task.status), cardWidth - 168, 18, 152, 30, () -> {
                    task.status = nextStatus(task.status);
                    appendLog("TASK", "Statut changé: " + task.title + " -> " + task.status);
                    refreshDashboardView();
                    refreshStats();
                });
                step.setBackground(new Color(67, 138, 244));
                card.addChild(step);

                Button edit = new Button("Editer", cardWidth - 236, 60, 104, 28, () -> openTaskModal(task));
                edit.setBackground(new Color(91, 125, 255));
                card.addChild(edit);

                Button remove = new Button("Supprimer", cardWidth - 124, 60, 108, 28, () -> openConfirmModal(
                        "Supprimer la tâche",
                        "Confirmer la suppression de '" + task.title + "' ?",
                        () -> {
                            tasks.remove(task);
                            appendLog("TASK", "Tâche supprimée: " + task.title);
                            refreshDashboardView();
                            refreshStats();
                            setStatus("Tâche supprimée");
                        }));
                remove.setBackground(new Color(216, 102, 102));
                card.addChild(remove);

                y += 130;
            }

            taskScroll.setContentHeight(y + 8);
            taskScroll.setContentWidth(Math.max(taskScroll.getWidth(), cardWidth + 24));
            taskScroll.invalidate();
        }

        private void refreshNotesView() {
            clearChildren(noteList);
            int y = 12;
            int cardWidth = Math.max(400, noteScroll.getWidth() - 30);

            for (NoteItem note : notes) {
                Div card = new Div(12, y, cardWidth, 110, new Color(255, 255, 255), 14);
                noteList.addChild(card);

                Label title = new Label(note.title, 14, 12, cardWidth - 160, 22);
                title.setFont(new Font("Dialog", Font.BOLD, 14));
                title.setColor(new Color(37, 49, 67));
                card.addChild(title);

                Label ts = new Label(TS_FORMAT.format(note.updatedAt), cardWidth - 130, 12, 116, 20);
                ts.setColor(new Color(100, 112, 130));
                ts.setFont(new Font("Dialog", Font.PLAIN, 12));
                card.addChild(ts);

                Label contentPreview = new Label(trimOneLine(note.content, 90), 14, 40, cardWidth - 24, 20);
                contentPreview.setColor(new Color(84, 94, 107));
                card.addChild(contentPreview);

                Button edit = new Button("Editer", cardWidth - 228, 70, 96, 28, () -> openEditNoteModal(note));
                edit.setBackground(new Color(91, 125, 255));
                card.addChild(edit);

                Button remove = new Button("Supprimer", cardWidth - 124, 70, 108, 28, () -> openConfirmModal(
                        "Supprimer la note",
                        "Confirmer la suppression de '" + note.title + "' ?",
                        () -> {
                            notes.remove(note);
                            appendLog("NOTE", "Note supprimée: " + note.title);
                            refreshNotesView();
                            refreshStats();
                            setStatus("Note supprimée");
                        }));
                remove.setBackground(new Color(216, 102, 102));
                card.addChild(remove);

                y += 122;
            }

            noteScroll.setContentHeight(y + 8);
            noteScroll.setContentWidth(Math.max(noteScroll.getWidth(), cardWidth + 24));
            noteScroll.invalidate();
        }

        private void refreshLogbookView() {
            clearChildren(logList);
            int y = 12;
            int cardWidth = Math.max(420, logScroll.getWidth() - 30);

            for (int i = logbook.size() - 1; i >= 0; i--) {
                LogEntry row = logbook.get(i);
                Div card = new Div(12, y, cardWidth, 98, new Color(255, 255, 255), 12);
                logList.addChild(card);

                Label head = new Label("[" + row.category + "] " + TS_FORMAT.format(row.time), 12, 10, cardWidth - 24,
                        18);
                head.setFont(new Font("Dialog", Font.BOLD, 12));
                head.setColor(new Color(60, 92, 126));
                card.addChild(head);

                Label message = new Label(trimOneLine(row.message, 120), 12, 34, cardWidth - 24, 20);
                message.setColor(new Color(74, 84, 102));
                card.addChild(message);

                Button edit = new Button("Editer", cardWidth - 228, 62, 96, 26, () -> openEditLogModal(row));
                edit.setBackground(new Color(91, 125, 255));
                card.addChild(edit);

                Button remove = new Button("Supprimer", cardWidth - 124, 62, 108, 26, () -> openConfirmModal(
                        "Supprimer l'entrée",
                        "Confirmer la suppression de cette entrée du journal ?",
                        () -> {
                            logbook.remove(row);
                            refreshLogbookView();
                            setStatus("Entrée journal supprimée");
                        }));
                remove.setBackground(new Color(216, 102, 102));
                card.addChild(remove);

                y += 108;
            }

            logScroll.setContentHeight(y + 8);
            logScroll.setContentWidth(Math.max(logScroll.getWidth(), cardWidth + 24));
            logScroll.invalidate();
        }

        private void refreshStats() {
            int open = 0;
            for (TaskItem task : tasks) {
                if (!"Done".equals(task.status)) {
                    open++;
                }
            }
            statsTasks.setText("Tâches: " + tasks.size());
            statsNotes.setText("Notes: " + notes.size());
            statsOpen.setText("Ouvertes: " + open);
            statsTasks.invalidate();
            statsNotes.invalidate();
            statsOpen.invalidate();
        }

        private void openTaskModal() {
            openTaskModal(null);
        }

        private void openTaskModal(TaskItem editing) {
            closeMobileMenuOverlay();
            boolean editMode = editing != null;
            FormModal modal = new FormModal(560, 430, editMode ? "Editer la tâche" : "Ajouter une tâche",
                    window::closeTopLayer);
            BaseComp body = modal.getBody();

            Label titleLabel = new Label("Titre", 20, 10, 120, 20);
            body.addChild(titleLabel);

            TextField titleInput = new TextField(20, 34, 520, 36);
            titleInput.setPlaceholder("Titre de la tâche");
            if (editMode) {
                titleInput.setText(editing.title);
            }
            body.addChild(titleInput);

            Label ownerLabel = new Label("Owner", 20, 82, 120, 20);
            body.addChild(ownerLabel);

            TextField ownerInput = new TextField(20, 106, 250, 34);
            ownerInput.setPlaceholder("@responsable");
            if (editMode) {
                ownerInput.setText(editing.owner);
            }
            body.addChild(ownerInput);

            Label statusLabel = new Label("Statut", 290, 82, 120, 20);
            body.addChild(statusLabel);

            SegmentedSelect statusInput = new SegmentedSelect(290, 106, 250, 34);
            statusInput.setOptions(List.of("Backlog", "In Progress", "Done"));
            statusInput.setSelectedOption(editMode ? editing.status : "Backlog");
            body.addChild(statusInput);

            CheckBox priority = new CheckBox("Priorité haute", 20, 150, 220, 30, editMode && editing.highPriority);
            body.addChild(priority);

            Label descLabel = new Label("Description", 20, 184, 160, 20);
            body.addChild(descLabel);

            TextAreaInput desc = new TextAreaInput(20, 208, 520, 102);
            desc.setPlaceholder("Description détaillée");
            if (editMode) {
                desc.setText(editing.description);
            }
            body.addChild(desc);

            Button cancel = new Button("Annuler", 300, 324, 110, 34, window::closeTopLayer);
            cancel.setBackground(new Color(188, 198, 212));
            cancel.setForeground(new Color(56, 67, 82));
            body.addChild(cancel);

            Button save = new Button(editMode ? "Sauver" : "Créer", 422, 324, 118, 34, () -> {
                String titleValue = titleInput.getText().trim().isEmpty() ? "Tâche sans titre"
                        : titleInput.getText().trim();
                String ownerValue = ownerInput.getText().trim().isEmpty() ? "@unassigned" : ownerInput.getText().trim();
                String descValue = desc.getText().trim().isEmpty() ? "Aucune description" : desc.getText().trim();

                if (editMode) {
                    editing.title = titleValue;
                    editing.owner = ownerValue;
                    editing.description = descValue;
                    editing.status = statusInput.getSelectedOption();
                    editing.highPriority = priority.isChecked();
                    appendLog("TASK", "Tâche mise à jour: " + editing.title);
                    setStatus("Tâche modifiée");
                } else {
                    TaskItem task = new TaskItem(titleValue, descValue, statusInput.getSelectedOption(),
                            priority.isChecked(), ownerValue);
                    tasks.add(task);
                    appendLog("TASK", "Tâche créée: " + titleValue + " (" + task.status + ")");
                    setStatus("Tâche ajoutée");
                }
                refreshDashboardView();
                refreshStats();
                window.closeTopLayer();
            });
            save.setBackground(new Color(55, 139, 255));
            body.addChild(save);

            window.openModal(modal);
        }

        private void openNoteModal() {
            openNoteModal(null);
        }

        private void openNoteModal(NoteItem editing) {
            closeMobileMenuOverlay();
            boolean editMode = editing != null;
            FormModal modal = new FormModal(560, 390, editMode ? "Editer la note" : "Ajouter une note",
                    window::closeTopLayer);
            BaseComp body = modal.getBody();

            Label titleLabel = new Label("Titre", 20, 10, 120, 20);
            body.addChild(titleLabel);

            TextField titleInput = new TextField(20, 34, 520, 36);
            titleInput.setPlaceholder("Titre de la note");
            if (editMode) {
                titleInput.setText(editing.title);
            }
            body.addChild(titleInput);

            Label contentLabel = new Label("Contenu", 20, 82, 120, 20);
            body.addChild(contentLabel);

            TextAreaInput contentInput = new TextAreaInput(20, 106, 520, 170);
            contentInput.setPlaceholder("Rédige ta note");
            if (editMode) {
                contentInput.setText(editing.content);
            }
            body.addChild(contentInput);

            Button cancel = new Button("Annuler", 300, 286, 110, 34, window::closeTopLayer);
            cancel.setBackground(new Color(188, 198, 212));
            cancel.setForeground(new Color(56, 67, 82));
            body.addChild(cancel);

            Button save = new Button(editMode ? "Sauver" : "Enregistrer", 422, 286, 118, 34, () -> {
                String t = titleInput.getText().trim().isEmpty() ? "Note sans titre" : titleInput.getText().trim();
                String c = contentInput.getText().trim().isEmpty() ? "(vide)" : contentInput.getText().trim();
                if (editMode) {
                    editing.title = t;
                    editing.content = c;
                    editing.updatedAt = LocalDateTime.now();
                    appendLog("NOTE", "Note mise à jour: " + t);
                    setStatus("Note modifiée");
                } else {
                    notes.add(new NoteItem(t, c));
                    appendLog("NOTE", "Note créée: " + t);
                    setStatus("Note ajoutée");
                }
                refreshNotesView();
                refreshStats();
                window.closeTopLayer();
            });
            save.setBackground(new Color(91, 125, 255));
            body.addChild(save);

            window.openModal(modal);
        }

        private void openEditNoteModal(NoteItem note) {
            openNoteModal(note);
        }

        private void openLogModal() {
            openLogModal(null);
        }

        private void openLogModal(LogEntry editing) {
            closeMobileMenuOverlay();
            boolean editMode = editing != null;
            FormModal modal = new FormModal(560, 360,
                    editMode ? "Editer entrée journal" : "Nouvelle entrée journal", window::closeTopLayer);
            BaseComp body = modal.getBody();

            Label typeLabel = new Label("Catégorie", 20, 10, 120, 20);
            body.addChild(typeLabel);

            SelectInput category = new SelectInput(20, 34, 220, 34);
            category.setOptions(List.of("SYSTEM", "TASK", "NOTE", "RELEASE", "INCIDENT"));
            category.setSelectedOption(editMode ? editing.category : "SYSTEM");
            body.addChild(category);

            Label msgLabel = new Label("Message", 20, 80, 120, 20);
            body.addChild(msgLabel);

            TextAreaInput message = new TextAreaInput(20, 104, 520, 130);
            message.setPlaceholder("Détail de l'événement");
            if (editMode) {
                message.setText(editing.message);
            }
            body.addChild(message);

            Button cancel = new Button("Annuler", 300, 252, 110, 34, window::closeTopLayer);
            cancel.setBackground(new Color(188, 198, 212));
            cancel.setForeground(new Color(56, 67, 82));
            body.addChild(cancel);

            Button add = new Button(editMode ? "Sauver" : "Ajouter", 422, 252, 118, 34, () -> {
                String msg = message.getText().trim().isEmpty() ? "Entrée sans description" : message.getText().trim();
                if (editMode) {
                    editing.category = category.getSelectedOption();
                    editing.message = msg;
                    editing.time = LocalDateTime.now();
                    appendLog("SYSTEM", "Entrée journal modifiée");
                    setStatus("Entrée journal modifiée");
                } else {
                    appendLog(category.getSelectedOption(), msg);
                    setStatus("Entrée journal ajoutée");
                }
                refreshLogbookView();
                window.closeTopLayer();
            });
            add.setBackground(new Color(35, 163, 122));
            body.addChild(add);

            window.openModal(modal);
        }

        private void openEditLogModal(LogEntry row) {
            openLogModal(row);
        }

        private void openConfirmModal(String title, String message, Runnable onConfirm) {
            closeMobileMenuOverlay();
            ConfirmDialog dialog = new ConfirmDialog(480, 220, title, message, () -> {
                if (onConfirm != null) {
                    onConfirm.run();
                }
                window.closeTopLayer();
            }, window::closeTopLayer);
            window.openModal(dialog);
        }

        private void closeMobileMenuOverlay() {
            if (!mobileMenuOpen && mobileMenuPanel.getParent() == null) {
                return;
            }
            mobileMenuOpen = false;
            syncResponsiveMenuState();
        }

        private void appendLog(String category, String message) {
            logbook.add(new LogEntry(category, message));
            if (logbook.size() > 250) {
                logbook.remove(0);
            }
            refreshLogbookView();
        }

        private void setStatus(String message) {
            statusLabel.setText("" + TS_FORMAT.format(LocalDateTime.now()) + " | " + message);
            statusLabel.invalidate();
            statusBar.invalidate();
        }

        private static void clearChildren(BaseComp parent) {
            List<BaseComp> copy = new ArrayList<>(parent.getChildrenList());
            for (BaseComp child : copy) {
                parent.removeChild(child);
            }
        }

        private static String nextStatus(String status) {
            return switch (status) {
                case "Backlog" -> "In Progress";
                case "In Progress" -> "Done";
                default -> "Backlog";
            };
        }

        private static String nextStatusLabel(String status) {
            return switch (status) {
                case "Backlog" -> "Passer en cours";
                case "In Progress" -> "Passer terminé";
                default -> "Réouvrir";
            };
        }

        private static String trimOneLine(String source, int max) {
            if (source == null) {
                return "";
            }
            String cleaned = source.replace("\n", " ").trim();
            if (cleaned.length() <= max) {
                return cleaned;
            }
            return cleaned.substring(0, Math.max(0, max - 3)) + "...";
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
        BaseWindow window = new BaseWindow("WorkSuite Pro", 1320, 860, fps);
        new App(window);
        window.show();
    }
}
