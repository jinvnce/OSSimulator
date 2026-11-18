package paging;

import ui.Icons;
import ui.ModuleHeader;
import ui.ModulePanel;
import ui.Theme;
import util.Util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PagingPanel extends ModulePanel {

    private static final int START_PAGES = 4;
    private final JTextField pageSize = new Theme.RoundField("4", 4);
    private final JTextField frames = new Theme.RoundField("8", 4);
    private final JTextField addrs = new Theme.RoundField("0 3 4 13", 14);

    private final DefaultTableModel ptModel = new DefaultTableModel(new String[]{"Page", "Frame"}, 0) {
        public boolean isCellEditable(int r, int c) {
            return c == 1;
        }
    };
    private final JTable ptTable = new JTable(ptModel);

    private final DefaultTableModel results = Util.readOnlyModel(
            new String[]{"Logical", "Page", "Offset", "Frame", "Physical"});
    private final JTable resultsTable = new JTable(results);


    private final MemoryDiagram diagram = new MemoryDiagram();
    private final Theme.SkinPanel memCard = new Theme.SkinPanel("Memory map", null, false);

    public PagingPanel() {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton add = Icons.on(ModuleHeader.navyButton("Add page", 130), "plus", Theme.CREAM);
        JButton del = outlineButton("Remove page", "minus");
        JButton rnd = Icons.on(ModuleHeader.navyButton("Randomize", 130), "dice", Theme.CREAM);
        JButton run = ModuleHeader.runButton("Run Simulation");
        JButton clr = outlineButton("Clear all", "trash");
        clr.setToolTipText("Empty the Frame column, the addresses, the results and the steps");
        pageSize.setToolTipText("Size of one page / frame in bytes");
        frames.setToolTipText("Number of frames in physical memory");
        addrs.setToolTipText("Logical addresses, separated by spaces or commas");
        ptTable.setToolTipText("Edit the Frame column. Blank = not in memory.");
        run.setToolTipText("Translate the addresses above. Leave them empty to translate every address.");

        JPanel tools = ModuleHeader.toolRow();
        JButton none = new JButton();
        none.setVisible(false);
        add(ModuleHeader.north("Paging", "Address translation", "club", tools, none), BorderLayout.NORTH);
        Theme.styleTable(ptTable, false);
        Theme.styleTable(resultsTable, true);


        Theme.SkinPanel setupCard = new Theme.SkinPanel("Memory setup", null, false);
        setupCard.setTitleIcon(Icons.glyph("sliders", Theme.INK));
        JPanel setupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        setupRow.setOpaque(false);
        setupRow.add(Theme.label("Page size", Theme.INK, 15f));
        setupRow.add(pageSize);
        setupRow.add(Theme.label("Frames", Theme.INK, 15f));
        setupRow.add(frames);
        setupCard.body.add(setupRow, BorderLayout.CENTER);

        Theme.SkinPanel ptCard = new Theme.SkinPanel("Page table", null, false);
        ptCard.setTitleIcon(Icons.glyph("table", Theme.INK));
        ptCard.body.add(Theme.scroll(ptTable, Theme.CREAM), BorderLayout.CENTER);
        JPanel btns = new JPanel(new GridLayout(2, 2, 6, 6));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        btns.add(add);
        btns.add(del);
        btns.add(rnd);
        btns.add(clr);
        ptCard.body.add(btns, BorderLayout.SOUTH);

        Theme.SkinPanel addrCard = new Theme.SkinPanel("Addresses", null, false);
        addrCard.setTitleIcon(Icons.glyph("sheet", Theme.INK));
        JPanel addrRow = new JPanel(new BorderLayout(8, 0));
        addrRow.setOpaque(false);
        addrRow.add(addrs, BorderLayout.CENTER);
        JPanel addrBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        addrBtns.setOpaque(false);
        addrBtns.add(run);
        addrRow.add(addrBtns, BorderLayout.EAST);
        addrCard.body.add(addrRow, BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(setupCard, BorderLayout.NORTH);
        left.add(ptCard, BorderLayout.CENTER);
        left.add(addrCard, BorderLayout.SOUTH);

        memCard.body.add(Theme.scroll(diagram, Theme.DEEP), BorderLayout.CENTER);
        memCard.body.add(MemoryDiagram.legend(), BorderLayout.SOUTH);

        Theme.SkinPanel resCard = new Theme.SkinPanel("Results", null, false);
        resCard.setTitleIcon(Icons.glyph("list", Theme.INK));
        resCard.body.add(Theme.scroll(resultsTable, Theme.DEEP), BorderLayout.CENTER);

        JSplitPane right = Theme.split(memCard, resCard);
        right.setResizeWeight(0.65);


        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.35);
        main.setOpaque(false);
        main.setBorder(null);
        main.setDividerSize(10);
        main.setContinuousLayout(true);
        add(main, BorderLayout.CENTER);

        rnd.addActionListener(e -> randomizeData());
        add.addActionListener(e -> addPage());
        del.addActionListener(e -> removePage());
        run.addActionListener(e -> runSimulation());
        clr.addActionListener(e -> clearAll());

        onChange(pageSize, () -> refresh(null, true));
        onChange(frames, () -> refresh(null, true));

        ptModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) refresh(null, true);
        });

        fillTable(START_PAGES, false);
        refresh(null, false);
    }

    private static JButton outlineButton(String text, String icon) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
                g2.setColor(getModel().isRollover() && isEnabled() ? new Color(0xFBEBD2) : Theme.CREAM);
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.setColor(Theme.INK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setForeground(Theme.CRIMSON);
        b.setFont(Theme.body(15f).deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(130, 40));
        return Icons.on(b, icon, Theme.CRIMSON);
    }

    private void clearAll() {
        Util.stopEditing(ptTable);
        int ans = JOptionPane.showConfirmDialog(this,
                "Clear all pages, addresses, results and steps?\nThis cannot be undone.",
                "Clear all", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        ptModel.setRowCount(0);
        addrs.setText("");
        results.setRowCount(0);
        refresh(null, true);
    }

    private void show(PageTable t, List<PageTable.Translation> list, boolean detailed) {
        results.setRowCount(0);
        Set<Integer> hl = new HashSet<>();
        Set<Integer> hotL = new HashSet<>();
        Set<Long> hotP = new HashSet<>();
        for (PageTable.Translation x : list) {
            results.addRow(new Object[]{x.logical, x.page, x.offset,
                    x.inMemory() ? "" + x.frame : "-", x.inMemory() ? "" + x.physical : "Page fault"});
            if (detailed) {
                hl.add(x.page);
                hotL.add(x.logical);
                if (x.inMemory()) hotP.add(x.physical);
            }
        }
        diagram.show(t, hl, hotL, hotP);
    }

    private void removePage() {
        Util.stopEditing(ptTable);
        int n = ptModel.getRowCount();
        if (n == 0) return;

        int[] rows = ptTable.getSelectedRows();
        if (rows.length == 0) rows = new int[]{n - 1};
        Arrays.sort(rows);

        String what = rows.length == 1 ? "page " + rows[0] : rows.length + " selected pages";
        int ans = JOptionPane.showConfirmDialog(this,
                "Remove " + what + "?",
                "Remove page", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        for (int i = rows.length - 1; i >= 0; i--) ptModel.removeRow(rows[i]);
        for (int i = 0; i < ptModel.getRowCount(); i++) ptModel.setValueAt(i, i, 0); // renumber pages
        refresh(null, true);
    }

    @Override
    public String getTitle() {
        return "Paging";
    }

    private static void onChange(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }


    private boolean fillTable(int n, boolean random) {
        try {
            int f = Util.parseInt(frames.getText(), "Frames", 1);
            Util.stopEditing(ptTable);
            List<Integer> order = new ArrayList<>();
            for (int k = 0; k < f; k++) order.add(k);
            if (random) Collections.shuffle(order, Util.RNG);
            ptModel.setRowCount(0);
            for (int i = 0; i < n; i++) ptModel.addRow(new Object[]{i, i < f ? "" + order.get(i) : ""});
            results.setRowCount(0);
            return true;
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
            return false;
        }
    }


    private void addPage() {
        Util.stopEditing(ptTable);
        int n = ptModel.getRowCount();

        String frame = "";
        int f = -1;
        try {
            f = Util.parseInt(frames.getText(), "Frames", 1);
            Set<Integer> used = new HashSet<>();
            for (int i = 0; i < n; i++) {
                String s = Util.str(ptModel.getValueAt(i, 1)).trim();
                if (!s.isEmpty() && !s.equals("-")) {
                    try {
                        used.add(Integer.parseInt(s));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            List<Integer> free = new ArrayList<>();
            for (int k = 0; k < f; k++) if (!used.contains(k)) free.add(k);
            if (!free.isEmpty()) frame = "" + free.get(Util.RNG.nextInt(free.size()));
        } catch (IllegalArgumentException ex) {
        }

        ptModel.addRow(new Object[]{n, frame});
        int last = ptModel.getRowCount() - 1;
        ptTable.scrollRectToVisible(ptTable.getCellRect(last, 0, true));
        ptTable.setRowSelectionInterval(last, last);
        refresh(null, true);

        if (f > 0 && n + 1 > f) {
            JOptionPane.showMessageDialog(this,
                    "You now have " + (n + 1) + " pages but only " + f + " frame" + (f == 1 ? "" : "s") + ".\n"
                            + "Increase the Frames field so every page can be loaded.",
                    "Not enough frames", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    @Override
    protected void randomizeData() {
        if (!fillTable(Math.max(1, ptModel.getRowCount()), true)) return;
        try {
            long total = (long) Util.parseInt(pageSize.getText(), "Page size", 1) * ptModel.getRowCount();
            int bound = (int) Math.min(total, Integer.MAX_VALUE);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 4; k++) sb.append(k > 0 ? " " : "").append(Util.RNG.nextInt(bound));
            addrs.setText(sb.toString());
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
        }
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
            memCard.setTitle("Memory map  \u00b7  " + t.getLogicalSize() + " B logical  \u00b7  "
                    + t.getPhysicalSize() + " B physical");

        } catch (IllegalArgumentException ex) {
            if (!quiet) Util.error(this, ex.getMessage());
        }
    }

    @Override
    protected void runSimulation() {
        PageTable t;
        List<PageTable.Translation> list = new ArrayList<>();
        try {
            t = readTable();
            String text = addrs.getText().trim();
            if (text.isEmpty()) {
                translateAll();
                return;
            }
            for (String part : text.split("[\\s,]+"))
                list.add(t.translate(Util.parseInt(part, "Address", 0)));
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
            return;
        }
        show(t, list, true);
    }

    private void translateAll() {
        PageTable t;
        List<PageTable.Translation> list = new ArrayList<>();
        try {
            t = readTable();
            for (int a = 0; a < t.getLogicalSize(); a++) list.add(t.translate(a));
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
            return;
        }
        show(t, list, false);
    }


}