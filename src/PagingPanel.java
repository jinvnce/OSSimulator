import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;


public class PagingPanel extends ModulePanel {

    private final JTextField pageSize = new JTextField("4", 4);
    private final JTextField pages = new JTextField("4", 4);
    private final JTextField frames = new JTextField("8", 4);
    private final JTextField addrs = new JTextField("0 3 4 13", 20);

    private final DefaultTableModel ptModel = new DefaultTableModel(new String[]{"Page", "Frame"}, 0) {
        public boolean isCellEditable(int r, int c) { return c == 1; }
    };
    private final JTable ptTable = new JTable(ptModel);

    private final DefaultTableModel results = Util.readOnlyModel(
            new String[]{"Logical", "Page", "Offset", "Frame", "Physical"});
    private final JTable resultsTable = new JTable(results);

    private final JTextArea log = new JTextArea();
    private final MemoryDiagram diagram = new MemoryDiagram();
    private final JLabel info = new JLabel(" ");

    public PagingPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // TOP BAR (row 1: setup, row 2: translate)
        JButton gen = new JButton("Generate");
        JButton rnd = new JButton("Randomize");
        JButton sample = new JButton("Sample");
        JButton apply = new JButton("Apply");
        JButton tr = new JButton("▶ Translate");
        JButton all = new JButton("Translate all");

        pageSize.setToolTipText("Bytes per page");
        pages.setToolTipText("Pages in logical memory");
        frames.setToolTipText("Frames in physical memory");
        addrs.setToolTipText("Logical addresses, separated by spaces or commas");
        gen.setToolTipText("Build an empty page table");
        rnd.setToolTipText("Build a table with shuffled frames");
        sample.setToolTipText("Load a ready-made example");
        apply.setToolTipText("Redraw the diagram using the edited table");
        all.setToolTipText("Translate every logical address");
        tr.setFont(tr.getFont().deriveFont(Font.BOLD));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row1.add(new JLabel("Page size"));
        row1.add(pageSize);
        row1.add(new JLabel("Pages"));
        row1.add(pages);
        row1.add(new JLabel("Frames"));
        row1.add(frames);
        row1.add(gen);
        row1.add(rnd);
        row1.add(sample);
        row1.add(apply);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row2.add(new JLabel("Address"));
        row2.add(addrs);
        row2.add(tr);
        row2.add(all);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(row1);
        top.add(row2);
        add(top, BorderLayout.NORTH);

        // TABLE TWEAKS
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        for (JTable t : new JTable[]{ptTable, resultsTable}) {
            t.setRowHeight(22);
            t.setFillsViewportHeight(true);
            for (int c = 0; c < t.getColumnCount(); c++) {
                t.getColumnModel().getColumn(c).setCellRenderer(center);
            }
        }

        ptTable.setToolTipText("Edit the Frame column. Blank = not in memory.");

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        // LEFT: input + output tables
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(ptTable, "Page table"),
                Util.scroll(resultsTable, "Results"));
        left.setResizeWeight(0.45);

        // RIGHT: diagram + steps
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(diagram, "Memory"),
                Util.scroll(log, "Steps"));
        right.setResizeWeight(0.65);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.4);
        add(main, BorderLayout.CENTER);

        // BOTTOM: memory sizes
        info.setForeground(Color.DARK_GRAY);
        info.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        add(info, BorderLayout.SOUTH);

        gen.addActionListener(e -> { if (generate(false)) refresh(null); });
        rnd.addActionListener(e -> randomizeData());
        sample.addActionListener(e -> loadSample());
        apply.addActionListener(e -> refresh(null));
        tr.addActionListener(e -> runSimulation());
        all.addActionListener(e -> translateAll());

        if (generate(false)) refresh(null);
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

    @Override
    protected void randomizeData() { if (generate(true)) refresh(null); }

    private void loadSample() {
        pageSize.setText("4"); pages.setText("4"); frames.setText("8"); addrs.setText("0 3 4 13");
        if (!generate(false)) return;
        int[] f = {5, 6, 1, 2};
        for (int i = 0; i < 4; i++) ptModel.setValueAt("" + f[i], i, 1);
        refresh(null);
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

    private void refresh(Set<Integer> highlight) {
        try {
            PageTable t = readTable();
            diagram.show(t, highlight == null ? new HashSet<>() : highlight);
            info.setText("Logical: " + t.getLogicalSize() + " bytes   |   Physical: " + t.getPhysicalSize() + " bytes");
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
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
        StringBuilder sb = new StringBuilder();
        int faults = 0;
        for (PageTable.Translation x : list) {
            results.addRow(new Object[]{x.logical, x.page, x.offset,
                    x.inMemory() ? "" + x.frame : "-", x.inMemory() ? "" + x.physical : "Page fault"});
            if (!x.inMemory()) faults++;
            if (detailed) { hl.add(x.page); for (String s : x.steps) sb.append(s).append('\n'); sb.append('\n'); }
        }
        if (!detailed) sb.append(list.size()).append(" addresses translated, ").append(faults).append(" page faults.\n");
        log.setText(sb.toString());
        log.setCaretPosition(0);
        diagram.show(t, hl);
    }
}