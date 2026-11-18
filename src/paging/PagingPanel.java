package paging;

import ui.ModuleHeader;
import ui.ModulePanel;
import ui.Theme;
import util.Util;
import ui.Icons;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PagingPanel extends ModulePanel {

    private final JTextField pageSize = new Theme.RoundField("4", 4);
    private final JTextField pages = new Theme.RoundField("4", 4);
    private final JTextField frames = new Theme.RoundField("8", 4);
    private final JTextField addrs = new Theme.RoundField("0 3 4 13", 14);

    private final DefaultTableModel ptModel = new DefaultTableModel(new String[]{"Page", "Frame"}, 0) {
        public boolean isCellEditable(int r, int c) { return c == 1; }
    };
    private final JTable ptTable = new JTable(ptModel);

    private final DefaultTableModel results = Util.readOnlyModel(
            new String[]{"Logical", "Page", "Offset", "Frame", "Physical"});
    private final JTable resultsTable = new JTable(results);

    private final JTextArea log = new JTextArea();
    private final MemoryDiagram diagram = new MemoryDiagram();
    private final Theme.SkinPanel memCard = new Theme.SkinPanel("Memory", null, false);

    public PagingPanel() {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton gen = Icons.on(ModuleHeader.navyButton("Generate", 120), "sheet", Theme.CREAM);
        JButton rnd = Icons.on(ModuleHeader.navyButton("Randomize", 130), "dice", Theme.CREAM);
        JButton add = Icons.on(ModuleHeader.navyButton("Add page", 130), "plus", Theme.CREAM);
        JButton del = Icons.on(ModuleHeader.navyButton("Remove page", 150), "minus", Theme.CREAM);
        JButton all = Icons.on(ModuleHeader.navyButton("Translate all", 170), "sheet", Theme.CREAM);
        JButton run = ModuleHeader.runButton("Translate");

        addrs.setToolTipText("Logical addresses, separated by spaces or commas");
        ptTable.setToolTipText("Edit the Frame column. Blank = not in memory.");

        // HEADER: banner + tool bar
        JPanel tools = ModuleHeader.toolRow();
        tools.add(Theme.label("Page size", Theme.CREAM, 16f));
        tools.add(pageSize);
        tools.add(Theme.label("Pages", Theme.CREAM, 16f));
        tools.add(pages);
        tools.add(Theme.label("Frames", Theme.CREAM, 16f));
        tools.add(frames);
        add(ModuleHeader.north("Paging", "Address translation", "club", tools, run), BorderLayout.NORTH);

        // TABLES
        Theme.styleTable(ptTable, false);
        Theme.styleTable(resultsTable, true);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        log.setBackground(Theme.DEEP);
        log.setForeground(Theme.CREAM);
        log.setCaretColor(Theme.CREAM);
        log.setMargin(new Insets(6, 8, 6, 8));

        // PAGE TABLE card (sits in a split pane, so it keeps a fixed share of the height
        // and the table scrolls when there are more rows than fit)
        Theme.SkinPanel ptCard = new Theme.SkinPanel("Page Table", null, false);
        ptCard.body.add(Theme.scroll(ptTable, Theme.CREAM), BorderLayout.CENTER);
        JPanel btns = new JPanel(new GridLayout(2, 2, 6, 6));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        btns.add(add); btns.add(del);
        btns.add(gen); btns.add(rnd);
        ptCard.body.add(btns, BorderLayout.SOUTH);

        // RESULTS card (moved to the left, under the page table)
        Theme.SkinPanel resCard = new Theme.SkinPanel("Results", null, false);
        resCard.body.add(Theme.scroll(resultsTable, Theme.DEEP), BorderLayout.CENTER);

        // ADDRESSES card
        Theme.SkinPanel addrCard = new Theme.SkinPanel("Addresses", null, false);
        JPanel addrRow = new JPanel(new BorderLayout(8, 0));
        addrRow.setOpaque(false);
        addrRow.add(addrs, BorderLayout.CENTER);
        addrRow.add(all, BorderLayout.EAST);
        addrCard.body.add(addrRow, BorderLayout.CENTER);

        // LEFT column: [page table | results] split, addresses pinned at the bottom
        JSplitPane leftSplit = Theme.split(ptCard, resCard);
        leftSplit.setResizeWeight(0.45);

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(leftSplit, BorderLayout.CENTER);
        left.add(addrCard, BorderLayout.SOUTH);

        // RIGHT column: big memory diagram, steps underneath
        memCard.body.add(Theme.scroll(diagram, Theme.DEEP), BorderLayout.CENTER);
        memCard.body.add(MemoryDiagram.legend(), BorderLayout.SOUTH);

        Theme.SkinPanel stepsCard = new Theme.SkinPanel("Steps", null, false);
        stepsCard.body.add(Theme.scroll(log, Theme.DEEP), BorderLayout.CENTER);

        JSplitPane right = Theme.split(memCard, stepsCard);
        right.setResizeWeight(0.75);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.4);
        main.setOpaque(false);
        main.setBorder(null);
        main.setDividerSize(10);
        main.setContinuousLayout(true);
        add(main, BorderLayout.CENTER);

        gen.addActionListener(e -> { if (generate(false)) refresh(null, false); });
        rnd.addActionListener(e -> randomizeData());
        add.addActionListener(e -> addPage());
        del.addActionListener(e -> removePage());

        // pressing Enter in a setting field applies it (rebuilds the page table)
        java.awt.event.ActionListener apply = e -> { if (generate(false)) refresh(null, false); };
        pageSize.addActionListener(apply);
        pages.addActionListener(apply);
        frames.addActionListener(apply);
        run.addActionListener(e -> runSimulation());
        all.addActionListener(e -> translateAll());

        // live redraw while editing the Frame column (silent on half-typed input)
        ptModel.addTableModelListener(e -> { if (e.getType() == TableModelEvent.UPDATE) refresh(null, true); });

        if (generate(false)) refresh(null, false);
    }

    @Override public String getTitle() { return "Paging"; }

    private boolean generate(boolean random) {
        try {
            int n = Util.parseInt(pages.getText(), "Pages", 1);
            int f = Util.parseInt(frames.getText(), "Frames", 1);
            Util.stopEditing(ptTable);
            List<Integer> order = new ArrayList<>();
            for (int k = 0; k < f; k++) order.add(k);
            if (random) Collections.shuffle(order, Util.RNG);
            ptModel.setRowCount(0);
            for (int i = 0; i < n; i++) ptModel.addRow(new Object[]{i, i < f ? "" + order.get(i) : ""});
            results.setRowCount(0);
            log.setText("");
            return true;
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return false; }
    }

    /**
     * Appends one page with a random free frame, without touching the frames already entered.
     * If every frame is already taken, the new page is left blank (not in memory).
     */
    private void addPage() {
        Util.stopEditing(ptTable);
        int n = ptModel.getRowCount();

        String frame = "";
        try {
            int f = Util.parseInt(frames.getText(), "Frames", 1);

            // collect frames that are not already assigned to another page
            Set<Integer> used = new HashSet<>();
            for (int i = 0; i < n; i++) {
                String s = Util.str(ptModel.getValueAt(i, 1)).trim();
                if (!s.isEmpty() && !s.equals("-")) {
                    try { used.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) { }
                }
            }
            List<Integer> free = new ArrayList<>();
            for (int k = 0; k < f; k++) if (!used.contains(k)) free.add(k);

            if (!free.isEmpty()) frame = "" + free.get(Util.RNG.nextInt(free.size()));
        } catch (IllegalArgumentException ex) {
            // bad Frames field: just add the page with no frame
        }

        ptModel.addRow(new Object[]{n, frame});
        pages.setText("" + (n + 1));
        int last = ptModel.getRowCount() - 1;
        ptTable.scrollRectToVisible(ptTable.getCellRect(last, 0, true));
        ptTable.setRowSelectionInterval(last, last);
        refresh(null, true);

        // more pages than frames: remind the user to add a frame too
        try {
            int f = Util.parseInt(frames.getText(), "Frames", 1);
            if (n + 1 > f) {
                JOptionPane.showMessageDialog(this,
                        "You now have " + (n + 1) + " pages but only " + f + " frame" + (f == 1 ? "" : "s") + ".\n"
                                + "Increase the Frames field too (and press Enter) so every page can be loaded.",
                        "Not enough frames", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IllegalArgumentException ignored) { }
    }

    /** Removes the last page. */
    private void removePage() {
        Util.stopEditing(ptTable);
        int n = ptModel.getRowCount();
        if (n <= 1) return;
        ptModel.removeRow(n - 1);
        pages.setText("" + (n - 1));
        refresh(null, true);
    }

    @Override
    protected void randomizeData() {
        if (!generate(true)) return;
        try {
            long total = (long) Util.parseInt(pageSize.getText(), "Page size", 1) * ptModel.getRowCount();
            int bound = (int) Math.min(total, Integer.MAX_VALUE);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 4; k++) sb.append(k > 0 ? " " : "").append(Util.RNG.nextInt(bound));
            addrs.setText(sb.toString());
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
        refresh(null, false);
    }

    private PageTable readTable() {
        Util.stopEditing(ptTable);
        int size = Util.parseInt(pageSize.getText(), "Page size", 1);
        int f = Util.parseInt(frames.getText(), "Frames", 1);
        int n = ptModel.getRowCount();
        int[] frameOf = new int[n];
        for (int i = 0; i < n; i++) {
            String s = Util.str(ptModel.getValueAt(i, 1)).trim();
            frameOf[i] = (s.isEmpty() || s.equals("-")) ? -1 : Util.parseInt(s, "Frame of page " + i, 0);
        }
        return new PageTable(size, f, frameOf);
    }

    private void refresh(Set<Integer> highlight, boolean quiet) {
        try {
            PageTable t = readTable();
            diagram.show(t, highlight == null ? new HashSet<>() : highlight);
            memCard.setTitle("Memory  ·  " + t.getLogicalSize() + " B logical  ·  " + t.getPhysicalSize() + " B physical");
        } catch (IllegalArgumentException ex) { if (!quiet) Util.error(this, ex.getMessage()); }
    }

    @Override
    protected void runSimulation() {
        PageTable t;
        List<PageTable.Translation> list = new ArrayList<>();
        try {
            t = readTable();
            String text = addrs.getText().trim();
            if (text.isEmpty()) throw new IllegalArgumentException("Address is empty.");
            for (String part : text.split("[\\s,]+"))
                list.add(t.translate(Util.parseInt(part, "Address", 0)));
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        show(t, list, true);
    }

    private void translateAll() {
        PageTable t;
        List<PageTable.Translation> list = new ArrayList<>();
        try {
            t = readTable();
            for (int a = 0; a < t.getLogicalSize(); a++) list.add(t.translate(a));
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        show(t, list, false);
    }

    private void show(PageTable t, List<PageTable.Translation> list, boolean detailed) {
        results.setRowCount(0);
        Set<Integer> hl = new HashSet<>();
        Set<Integer> hotL = new HashSet<>();
        Set<Long> hotP = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        int faults = 0;
        for (PageTable.Translation x : list) {
            results.addRow(new Object[]{x.logical, x.page, x.offset,
                    x.inMemory() ? "" + x.frame : "-", x.inMemory() ? "" + x.physical : "Page fault"});
            if (!x.inMemory()) faults++;
            if (detailed) {
                hl.add(x.page);
                hotL.add(x.logical);
                if (x.inMemory()) hotP.add(x.physical);
                for (String s : x.steps) sb.append(s).append('\n');
                sb.append('\n');
            }
        }
        if (!detailed) sb.append(list.size()).append(" addresses translated, ").append(faults).append(" page faults.\n");
        log.setText(sb.toString());
        log.setCaretPosition(0);
        diagram.show(t, hl, hotL, hotP);
    }
}