import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;


public class PagingPanel extends ModulePanel {
    private final JTextField pageSize = new JTextField("4", 5), pages = new JTextField("4", 5),
            frames = new JTextField("8", 5), addrs = new JTextField("0 3 4 13", 18);
    private final DefaultTableModel ptModel = new DefaultTableModel(new String[]{"Page", "Frame (blank = not in memory)"}, 0) {
        public boolean isCellEditable(int r, int c) { return c == 1; }
    };
    private final JTable ptTable = new JTable(ptModel);
    private final DefaultTableModel results = Util.readOnlyModel(
            new String[]{"Logical address", "Page number", "Offset", "Frame", "Physical address"});
    private final JTextArea log = new JTextArea();
    private final MemoryDiagram diagram = new MemoryDiagram();
    private final JLabel info = new JLabel(" ");

    public PagingPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new GridLayout(2, 1));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton gen = new JButton("Generate Table"), rnd = new JButton("Randomize"),
                sample = new JButton("Load Sample"), apply = new JButton("Apply Table");
        r1.add(new JLabel("Page size (bytes):")); r1.add(pageSize);
        r1.add(new JLabel("Pages in logical memory:")); r1.add(pages);
        r1.add(new JLabel("Frames in physical memory:")); r1.add(frames);
        r1.add(gen); r1.add(rnd); r1.add(sample); r1.add(apply);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton tr = new JButton("Translate"), all = new JButton("Translate All");
        r2.add(new JLabel("Logical address(es), separated by spaces:")); r2.add(addrs); r2.add(tr); r2.add(all);
        r2.add(info);
        top.add(r1); top.add(r2);
        add(top, BorderLayout.NORTH);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(ptTable, "Page table (edit the Frame column)"), Util.scroll(new JTable(results), "Translation results"));
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Util.scroll(diagram, "Memory diagram"), Util.scroll(log, "Calculation steps"));
        left.setResizeWeight(0.5); right.setResizeWeight(0.65);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.4);
        add(main, BorderLayout.CENTER);

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
            int n = Util.parseInt(pages.getText(), "Number of pages", 1);
            int f = Util.parseInt(frames.getText(), "Number of frames", 1);
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
        int f = Util.parseInt(frames.getText(), "Number of frames", 1);
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
            info.setText("Logical memory: " + t.getLogicalSize() + " bytes    Physical memory: " + t.getPhysicalSize() + " bytes");
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    @Override
    protected void runSimulation() {
        PageTable t;
        List<PageTable.Translation> list = new ArrayList<>();
        try {
            t = readTable();
            String text = addrs.getText().trim();
            if (text.isEmpty()) throw new IllegalArgumentException("Logical address is blank.");
            for (String part : text.split("[\\s,]+"))
                list.add(t.translate(Util.parseInt(part, "Logical address", 0)));
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
        if (!detailed) sb.append("Translated all ").append(list.size()).append(" logical addresses (").append(faults).append(" not in memory).\n");
        log.setText(sb.toString());
        log.setCaretPosition(0);
        diagram.show(t, hl);
    }
}
