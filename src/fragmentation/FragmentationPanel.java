package fragmentation;

import ui.ModuleHeader;
import ui.ModulePanel;
import ui.Theme;
import util.Util;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;

public class FragmentationPanel extends ModulePanel {
    private static final int MAX_BLOCKS = 40, MAX_PROCESSES = 60, MAX_SIZE = 1_000_000;
    private static final String FIXED = "Fixed partitions", DYNAMIC = "Dynamic partitions";

    private static final String[] OUT_NAMES = {"Process", "Size (KB)", "Block", "Wasted", "Status"};
    private static final String[] OUT_TIPS = {"Process number", "How much memory the process needs",
            "The memory block the process was placed in (- if none)",
            "Wasted = internal fragmentation: space inside the block that the process does not use (fixed partitions only)",
            "Allocated, or why the process had to wait: External = enough memory in total but not in one piece; No memory = not enough free memory at all"};
    private static final String[] CMP_NAMES = {"Strategy", "Placed", "Internal", "External"};
    private static final String[] CMP_TIPS = {"Placement strategy", "How many processes found a place",
            "Total internal fragmentation", "External fragmentation: free memory that is too scattered to use"};

    private final DefaultTableModel out = Util.readOnlyModel(OUT_NAMES), cmp = Util.readOnlyModel(CMP_NAMES);
    private final JTable outTable = tipTable(out, OUT_TIPS, true), cmpTable = tipTable(cmp, CMP_TIPS, true);
    private final JTextField blocks = new Theme.RoundField("100 500 200 300 600", 20), procs = new Theme.RoundField("212 417 112 426", 20),
            blockCount = new Theme.RoundField("5", 4), procCount = new Theme.RoundField("4", 4), maxSize = new Theme.RoundField("600", 6);
    private final JComboBox<MemoryAllocator.Strategy> strategy = new JComboBox<>(MemoryAllocator.Strategy.values());
    private final JComboBox<String> memoryType = new JComboBox<>(new String[]{FIXED, DYNAMIC});
    private final JComboBox<String> unitBox = new JComboBox<>(new String[]{"KB", "MB", "GB"});
    private final JLabel blocksLabel = Theme.label("Memory blocks (KB, in order):", Theme.INK, 15f),
            procsLabel = Theme.label("Processes (KB, in arrival order):", Theme.INK, 15f),
            maxLabel = Theme.label("Largest size (KB):", Theme.INK, 15f);
    private final JLabel internalValue = bigValue(), externalValue = bigValue();
    private final AllocationDiagram diagram = new AllocationDiagram();

    public FragmentationPanel() {
        setLayout(new BorderLayout(10, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton run = ModuleHeader.runButton("Run Simulation");
        Theme.styleCombo(strategy);
        Theme.styleCombo(memoryType);
        Theme.styleCombo(unitBox);
        strategy.setPreferredSize(new Dimension(150, 34));
        memoryType.setPreferredSize(new Dimension(190, 34));
        unitBox.setPreferredSize(new Dimension(80, 34));
        strategy.setToolTipText("How to choose between several blocks that are big enough");
        memoryType.setToolTipText("Fixed: one process per block, wasted space inside it. Dynamic: a block is split to fit the process.");
        unitBox.setToolTipText("The unit the sizes are written in (KB, MB or GB)");
        JPanel tools = ModuleHeader.toolRow();
        tools.add(Theme.label("Strategy:", Theme.CREAM, 15f));
        tools.add(strategy);
        tools.add(Theme.label("Memory:", Theme.CREAM, 15f));
        tools.add(memoryType);
        tools.add(Theme.label("Unit:", Theme.CREAM, 15f));
        tools.add(unitBox);
        add(ModuleHeader.north("Fragmentation", "Place processes in memory and spot the wasted space!", "hourglass", tools, run), BorderLayout.NORTH);

        JPanel left = new LeftPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(inputPanel());
        left.add(Box.createVerticalStrut(2));
        left.add(comparePanel());
        JScrollPane leftScroll = new JScrollPane(left, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(BorderFactory.createEmptyBorder());
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setPreferredSize(new Dimension(400, 100));
        Theme.styleScroll(leftScroll);
        add(leftScroll, BorderLayout.WEST);

        Theme.SkinPanel mapPanel = new Theme.SkinPanel("Memory Map", "memory", false);
        JScrollPane mapScroll = Theme.scroll(diagram, Theme.DEEP);
        mapScroll.setPreferredSize(new Dimension(400, 230));
        mapPanel.body.add(mapScroll, BorderLayout.CENTER);

        Theme.SkinPanel tablePanel = new Theme.SkinPanel("Allocation Results", "cpu", false);
        JScrollPane tableScroll = Theme.scroll(outTable, Theme.DEEP);
        tableScroll.setPreferredSize(new Dimension(400, 170));
        tablePanel.body.add(tableScroll, BorderLayout.CENTER);
        outTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        JSplitPane right = Theme.split(mapPanel, tablePanel);
        right.setResizeWeight(0.5);

        JPanel summary = new JPanel(new GridLayout(1, 2, 12, 0));
        summary.setOpaque(false);
        summary.add(card("Internal fragmentation", internalValue, "Space wasted INSIDE blocks that were given to a process", "hourglass"));
        summary.add(card("External fragmentation", externalValue,
                "Free memory that is too scattered to hold a waiting process, although the total would be enough", "memory"));
        JPanel bottom = new JPanel(new BorderLayout(0, 0));
        bottom.setOpaque(false);
        bottom.add(summary, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(right, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        run.addActionListener(e -> runSimulation());
        unitBox.addActionListener(e -> applyUnit());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
        super.paintComponent(g);
    }

    private static class LeftPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return 64;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private String unit() {
        return (String) unitBox.getSelectedItem();
    }

    private void applyUnit() {
        String u = unit();
        blocksLabel.setText("Memory blocks (" + u + ", in order):");
        procsLabel.setText("Processes (" + u + ", in arrival order):");
        maxLabel.setText("Largest size (" + u + "):");
        outTable.getColumnModel().getColumn(1).setHeaderValue("Size (" + u + ")");
        outTable.getTableHeader().repaint();
        clearResults();
    }

    private JPanel inputPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Blocks and Processes", "cards", false);
        p.body.setLayout(new GridBagLayout());
        GridBagConstraints c = gbc();
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;
        p.body.add(blocksLabel, c);
        c.gridy = 1;
        c.weightx = 1;
        p.body.add(blocks, c);
        c.gridy = 2;
        c.weightx = 0;
        p.body.add(procsLabel, c);
        c.gridy = 3;
        c.weightx = 1;
        p.body.add(procs, c);
        blocks.setToolTipText("Sizes separated by spaces or commas, e.g.  100 500 200 300 600");
        procs.setToolTipText("Sizes separated by spaces or commas, e.g.  212 417 112 426");

        JPanel counts = new JPanel(new GridLayout(3, 2, 6, 4));
        counts.setOpaque(false);
        counts.add(Theme.label("How many blocks:", Theme.INK, 15f));
        counts.add(blockCount);
        counts.add(Theme.label("How many processes:", Theme.INK, 15f));
        counts.add(procCount);
        counts.add(maxLabel);
        counts.add(maxSize);
        c.gridy = 4;
        c.insets = new Insets(10, 4, 3, 4);
        p.body.add(counts, c);
        blockCount.setToolTipText("Randomize picks these three values itself and fills them in");
        procCount.setToolTipText("Randomize picks these three values itself and fills them in");
        maxSize.setToolTipText("Randomize picks these three values itself and fills them in");

        JButton rnd = ModuleHeader.withIcon(ModuleHeader.navyButton("Randomize", 150), "dice", Theme.CREAM);
        rnd.setToolTipText("Random number of blocks and processes, random largest size, random block and process sizes");
        c.gridy = 5;
        c.insets = new Insets(6, 4, 3, 4);
        p.body.add(rnd, c);
        rnd.addActionListener(e -> randomizeData());
        return p;
    }

    private JPanel comparePanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Compare Strategies", "diamond", false);
        p.body.setLayout(new BorderLayout(0, 8));
        JScrollPane sp = Theme.scroll(cmpTable, Theme.DEEP);
        sp.setPreferredSize(new Dimension(200, 150));
        p.body.add(sp, BorderLayout.CENTER);
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 4, 3, 4);
        return c;
    }

    @Override
    public String getTitle() {
        return "Fragmentation";
    }

    @Override
    protected void randomizeData() {
        int nb = 3 + Util.RNG.nextInt(6);
        int np = 3 + Util.RNG.nextInt(6);
        int max = 100 * (2 + Util.RNG.nextInt(19));
        blockCount.setText(String.valueOf(nb));
        procCount.setText(String.valueOf(np));
        maxSize.setText(String.valueOf(max));

        int lo = Math.max(1, max / 10);
        StringBuilder b = new StringBuilder(), p = new StringBuilder();
        for (int i = 0; i < nb; i++) b.append(lo + Util.RNG.nextInt(max - lo + 1)).append(' ');
        int pmax = Math.max(lo, max * 3 / 4);
        for (int i = 0; i < np; i++) p.append(lo + Util.RNG.nextInt(pmax - lo + 1)).append(' ');
        blocks.setText(b.toString().trim());
        procs.setText(p.toString().trim());
        clearResults();
    }

    @Override
    protected void runSimulation() {
        try {
            int[] b = parseSizes(blocks.getText(), "Memory block", MAX_BLOCKS), p = parseSizes(procs.getText(), "Process", MAX_PROCESSES);
            MemoryAllocator.Strategy s = (MemoryAllocator.Strategy) strategy.getSelectedItem();
            boolean fixed = FIXED.equals(memoryType.getSelectedItem());
            MemoryAllocator.Result r = MemoryAllocator.run(b, p, s, fixed);

            out.setRowCount(0);
            for (MemoryAllocator.Placement pl : r.placements) {
                String status = pl.allocated() ? "Allocated" : pl.external ? "External" : "No memory";
                out.addRow(new Object[]{"P" + (pl.process + 1), String.valueOf(pl.size), pl.allocated() ? String.valueOf(pl.block + 1) : "-",
                        fixed && pl.allocated() ? String.valueOf(pl.internal) : "-", status});
            }
            diagram.show(r, p);
            internalValue.setText(r.internalFragmentation + " " + unit());
            externalValue.setText(r.externalFragmentation + " " + unit());
            compareAll();
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
        }
    }

    private void compareAll() {
        try {
            int[] b = parseSizes(blocks.getText(), "Memory block", MAX_BLOCKS), p = parseSizes(procs.getText(), "Process", MAX_PROCESSES);
            boolean fixed = FIXED.equals(memoryType.getSelectedItem());
            cmp.setRowCount(0);
            for (MemoryAllocator.Strategy s : MemoryAllocator.Strategy.values()) {
                MemoryAllocator.Result r = MemoryAllocator.run(b, p, s, fixed);
                cmp.addRow(new Object[]{s.toString(), (p.length - r.waiting) + " of " + p.length,
                        r.internalFragmentation + " " + unit(), r.externalFragmentation + " " + unit()});
            }
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
        }
    }

    private int[] parseSizes(String text, String what, int max) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Type at least one " + what.toLowerCase() + " size.");
        String[] tok = s.split("[\\s,;]+");
        if (tok.length > max)
            throw new IllegalArgumentException("At most " + max + " " + what.toLowerCase() + "s are allowed (got " + tok.length + ").");
        int[] v = new int[tok.length];
        for (int i = 0; i < tok.length; i++) {
            v[i] = Util.parseInt(tok[i], what + " " + (i + 1), 1);
            if (v[i] > MAX_SIZE)
                throw new IllegalArgumentException(what + " " + (i + 1) + " is too large (at most " + MAX_SIZE + " " + unit() + ").");
        }
        return v;
    }

    private void clearResults() {
        out.setRowCount(0);
        cmp.setRowCount(0);
        internalValue.setText("-");
        externalValue.setText("-");
        diagram.clear();
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(tb, v, sel, false, r, c);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Theme.heading(12f));
            if (!sel) setBackground(r % 2 == 0 ? new Color(0x16293F) : new Color(0x1C344F));
            setForeground("Allocated".equals(v) ? new Color(0x7BE08F) : new Color(0xFF8A95));
            return this;
        }
    }

    private JPanel card(String title, JLabel value, String tip, String iconName) {
        Theme.SkinPanel p = new Theme.SkinPanel(null, null, true);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 18, 20));
        p.setPreferredSize(new Dimension(200, 96));
        p.body.setLayout(new BorderLayout(14, 0));
        p.body.add(new JLabel(Theme.icon(iconName, 36, Theme.CREAM)), BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = Theme.label(title, Theme.CREAM, 14f);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t);
        text.add(value);
        p.body.add(text, BorderLayout.CENTER);
        p.setToolTipText(tip);
        return p;
    }

    private static JLabel bigValue() {
        JLabel l = new JLabel("-", SwingConstants.LEFT);
        l.setFont(Theme.heading(28f));
        l.setForeground(Theme.CREAM);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        return l;
    }

    private static JTable tipTable(DefaultTableModel model, String[] tips, boolean dark) {
        JTable t = new JTable(model) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
                    public String getToolTipText(MouseEvent e) {
                        int col = columnModel.getColumnIndexAtX(e.getX());
                        if (col < 0) return null;
                        int mi = columnModel.getColumn(col).getModelIndex();
                        return mi < tips.length ? tips[mi] : null;
                    }
                };
            }
        };
        Theme.styleTable(t, dark);
        return t;
    }
}