package segmentation;

import ui.ModuleHeader;
import ui.ModulePanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Segmentation: the user defines segments (base and limit), then types logical addresses written as segment:offset.
 * Each address is checked against the limit and either translated (physical = base + offset) or reported as an error.
 */
public class SegmentationPanel extends ModulePanel {
    private static final int MAX_SEGMENTS = 30, MAX_ADDRESSES = 200;
    private static final long MAX_NUMBER = 1_000_000_000L;
    private static final Pattern ADDRESS = Pattern.compile("^(-?\\d+):(-?\\d+)$");

    private static final String[] IN_NAMES = {"segmentation.Segment", "Base", "Limit"};
    private static final String[] IN_TIPS = {"segmentation.Segment number (the first half of a logical address)",
            "Base: the physical address where the segment starts", "Limit: the length of the segment. Valid offsets are 0 up to limit - 1"};
    private static final String[] OUT_NAMES = {"Address", "Limit", "Base", "Physical", "Status"};
    private static final String[] OUT_TIPS = {"Logical address written as segment:offset", "Limit of that segment",
            "Base of that segment", "Physical address = base + offset (only when the offset is below the limit)",
            "Valid, or the reason the address was rejected"};

    private final DefaultTableModel in = new DefaultTableModel(IN_NAMES, 0) {
        public boolean isCellEditable(int r, int c) { return c > 0; }
    };
    private final DefaultTableModel out = Util.readOnlyModel(OUT_NAMES);
    private final JTable inTable = tipTable(in, IN_TIPS, false), outTable = tipTable(out, OUT_TIPS, true);
    private final JTextField addresses = new Theme.RoundField("", 30);
    private final JTextField memSize = new Theme.RoundField("8192", 8), count = new Theme.RoundField("5", 4),
            maxLimit = new Theme.RoundField("1000", 6);
    private final JTextArea steps = new JTextArea();
    private final JLabel note = Theme.label(" ", Theme.CREAM_SHADOW, 14f);
    private final SegmentDiagram diagram = new SegmentDiagram();

    public SegmentationPanel() {
        setLayout(new BorderLayout(10, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton run = ModuleHeader.runButton("Translate");
        JPanel tools = ModuleHeader.toolRow();
        tools.add(Theme.label("Logical addresses (segment:offset):", Theme.CREAM, 15f));
        tools.add(addresses);
        addresses.setToolTipText("Write each address as segment:offset, separated by spaces, e.g.  2:53  3:852  0:1222");
        add(ModuleHeader.north("Segmentation", "Translate segment:offset addresses and catch the ones that are out of range!",
                "cards", tools, run), BorderLayout.NORTH);

        JPanel left = new LeftPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(segmentsPanel());
        left.add(Box.createVerticalStrut(2));
        left.add(stepsPanel());
        left.add(Box.createVerticalStrut(2));
        left.add(memoryPanel());
        JScrollPane leftScroll = new JScrollPane(left, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(BorderFactory.createEmptyBorder());
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setPreferredSize(new Dimension(400, 100));
        Theme.styleScroll(leftScroll);
        add(leftScroll, BorderLayout.WEST);

        // right side: memory map, results, calculation steps
        Theme.SkinPanel mapPanel = new Theme.SkinPanel("Physical Memory Map", "memory", false);
        JScrollPane mapScroll = Theme.scroll(diagram, Theme.DEEP);
        mapScroll.setPreferredSize(new Dimension(400, 170));
        mapPanel.body.add(mapScroll, BorderLayout.CENTER);

        Theme.SkinPanel resultsPanel = new Theme.SkinPanel("Translation Results", "cpu", false);
        JScrollPane resultsScroll = Theme.scroll(outTable, Theme.DEEP);
        resultsScroll.setPreferredSize(new Dimension(400, 200));
        resultsPanel.body.add(resultsScroll, BorderLayout.CENTER);
        outTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        JSplitPane right = Theme.split(mapPanel, resultsPanel);
        right.setResizeWeight(0.4);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(right, BorderLayout.CENTER);
        note.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 0));
        center.add(note, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        run.addActionListener(e -> runSimulation());
        loadSample();
    }

    @Override protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
        super.paintComponent(g);
    }

    private static class LeftPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ---------------------------------------------------------------- left column
    private JPanel segmentsPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("segmentation.Segment Table", "cards", false);
        p.body.setLayout(new BorderLayout(0, 8));
        JScrollPane sp = Theme.scroll(inTable, new Color(0xFBEBD2));
        sp.setPreferredSize(new Dimension(200, 190));
        p.body.add(sp, BorderLayout.CENTER);
        p.body.setToolTipText("Double-click a Base or Limit cell to edit it");

        JButton add = ModuleHeader.navyButton("Add row", 100), del = ModuleHeader.navyButton("Remove", 100),
                clr = ModuleHeader.navyButton("Clear all", 100);
        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.setOpaque(false);
        buttons.add(add); buttons.add(del); buttons.add(clr);
        p.body.add(buttons, BorderLayout.SOUTH);

        add.addActionListener(e -> {
            Util.stopEditing(inTable);
            if (in.getRowCount() >= MAX_SEGMENTS) { Util.error(this, "At most " + MAX_SEGMENTS + " segments are allowed."); return; }
            long next = 0;                                    // start the new segment after the last one
            for (int r = 0; r < in.getRowCount(); r++) {
                try { next = Math.max(next, Long.parseLong(Util.str(in.getValueAt(r, 1)).trim()) + Long.parseLong(Util.str(in.getValueAt(r, 2)).trim())); }
                catch (NumberFormatException ignored) { }
            }
            in.addRow(new Object[]{String.valueOf(in.getRowCount()), String.valueOf(next), "100"});
            clearResults();
        });
        del.addActionListener(e -> {
            Util.stopEditing(inTable);
            int[] rows = inTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) in.removeRow(rows[i]);
            renumber();
            clearResults();
        });
        clr.addActionListener(e -> {
            Util.stopEditing(inTable);
            in.setRowCount(0);
            clearResults();
        });
        return p;
    }

    private JPanel stepsPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Calculation Steps", "gear", false);
        steps.setEditable(false);
        steps.setLineWrap(true);
        steps.setWrapStyleWord(true);
        steps.setFont(Theme.body(14f));
        steps.setBackground(Theme.DEEP);
        steps.setForeground(Theme.CREAM);
        steps.setCaretColor(Theme.CREAM);
        steps.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane sp = Theme.scroll(steps, Theme.DEEP);
        sp.setPreferredSize(new Dimension(200, 190));
        p.body.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel memoryPanel() {
        Theme.SkinPanel p = new Theme.SkinPanel("Memory and Random Segments", "club", false);
        p.body.setLayout(new BorderLayout(0, 8));
        JPanel grid = new JPanel(new GridLayout(3, 2, 6, 4));
        grid.setOpaque(false);
        grid.add(Theme.label("Memory size (bytes):", Theme.INK, 15f)); grid.add(memSize);
        grid.add(Theme.label("How many segments:", Theme.INK, 15f)); grid.add(count);
        grid.add(Theme.label("Largest limit:", Theme.INK, 15f)); grid.add(maxLimit);
        p.body.add(grid, BorderLayout.CENTER);
        JButton rnd = ModuleHeader.navyButton("Randomize", 150), sample = ModuleHeader.navyButton("Load Sample", 150);
        JPanel two = new JPanel(new GridLayout(1, 2, 6, 0));
        two.setOpaque(false);
        two.add(rnd); two.add(sample);
        p.body.add(two, BorderLayout.SOUTH);
        memSize.setToolTipText("Every segment (base + limit) must fit inside this much memory");
        maxLimit.setToolTipText("Randomized segments get a limit between about a fifth of this and this value");
        rnd.addActionListener(e -> randomizeData());
        sample.addActionListener(e -> loadSample());
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 4, 3, 4);
        return c;
    }

    // ---------------------------------------------------------------- behaviour
    @Override public String getTitle() { return "Segmentation"; }

    /** The textbook example: five segments, plus addresses that are valid and addresses that are not. */
    private void loadSample() {
        Util.stopEditing(inTable);
        in.setRowCount(0);
        long[][] seg = {{1400, 1000}, {6300, 400}, {4300, 400}, {3200, 1100}, {4700, 1000}};
        for (int i = 0; i < seg.length; i++) in.addRow(new Object[]{String.valueOf(i), String.valueOf(seg[i][0]), String.valueOf(seg[i][1])});
        memSize.setText("8192");
        count.setText("5");
        maxLimit.setText("1000");
        addresses.setText("2:53 3:852 0:1222 1:100 4:0 5:10");
        clearResults();
        showMap();
    }

    @Override protected void randomizeData() {
        try {
            long mem = parseNumber(memSize.getText(), "Physical memory size", 1);
            int n = Util.parseInt(count.getText(), "How many segments", 1);
            int maxL = Util.parseInt(maxLimit.getText(), "Largest limit", 1);
            if (n > MAX_SEGMENTS) throw new IllegalArgumentException("How many segments must be at most " + MAX_SEGMENTS + ".");
            long[] limits = new long[n];
            long total = 0;
            for (int i = 0; i < n; i++) {
                int lo = Math.max(1, maxL / 5);
                limits[i] = lo + Util.RNG.nextInt(maxL - lo + 1);
                total += limits[i];
            }
            if (total > mem) throw new IllegalArgumentException("These segments need " + total + " bytes but physical memory is only "
                    + mem + ". Raise the memory size or lower the largest limit.");
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < n; i++) order.add(i);
            Collections.shuffle(order, Util.RNG);                // segments sit in memory in a random order, with gaps between
            long gap = mem - total, pos = 0;
            long[] bases = new long[n];
            for (int k : order) {
                long g = gap > 0 ? (long) (Util.RNG.nextDouble() * (gap / 2 + 1)) : 0;
                pos += g;
                gap -= g;
                bases[k] = pos;
                pos += limits[k];
            }
            Util.stopEditing(inTable);
            in.setRowCount(0);
            for (int i = 0; i < n; i++) in.addRow(new Object[]{String.valueOf(i), String.valueOf(bases[i]), String.valueOf(limits[i])});
            StringBuilder sb = new StringBuilder();             // some valid addresses, some too large, sometimes a missing segment
            for (int j = 0; j < 6; j++) {
                int s = Util.RNG.nextInt(n), roll = Util.RNG.nextInt(10);
                long off;
                if (roll < 7) off = Util.RNG.nextInt((int) limits[s]);
                else if (roll < 9) off = limits[s] + Util.RNG.nextInt((int) (limits[s] / 2 + 1));
                else { s = n + Util.RNG.nextInt(2); off = Util.RNG.nextInt(Math.max(1, maxL)); }
                sb.append(s).append(':').append(off).append(' ');
            }
            addresses.setText(sb.toString().trim());
            clearResults();
            showMap();
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
        }
    }

    @Override protected void runSimulation() {
        try {
            long mem = parseNumber(memSize.getText(), "Physical memory size", 1);
            SegmentTable table = readTable(mem);
            List<long[]> addrs = parseAddresses(addresses.getText());

            out.setRowCount(0);
            StringBuilder log = new StringBuilder();
            List<SegmentTable.Result> results = new ArrayList<>();
            int ok = 0;
            for (long[] a : addrs) {
                int seg = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, a[0]));
                SegmentTable.Result r = table.translate(seg, a[1]);
                results.add(r);
                if (r.valid) ok++;
                String status = r.valid ? "Valid" : r.entry == null ? "No segment" : "Out of range";
                out.addRow(new Object[]{r.logical, r.entry == null ? "-" : String.valueOf(r.entry.limit),
                        r.entry == null ? "-" : String.valueOf(r.entry.base), r.valid ? String.valueOf(r.physical) : "-", status});
                log.append("Logical address ").append(r.logical).append("  (segment ").append(r.segment).append(", offset ").append(r.offset).append(")\n");
                if (r.entry != null) log.append("  segmentation.Segment table: base ").append(r.entry.base).append(", limit ").append(r.entry.limit).append("\n");
                log.append("  ").append(r.message).append("\n\n");
            }
            steps.setText(log.toString());
            steps.setCaretPosition(0);
            diagram.show(table, mem, results);

            StringBuilder msg = new StringBuilder(ok + " of " + addrs.size() + " addresses are valid, " + (addrs.size() - ok) + " caused an addressing error.");
            for (int[] p : table.overlaps()) msg.append("  Warning: segments ").append(p[0]).append(" and ").append(p[1]).append(" overlap in physical memory.");
            note.setText(msg.toString());
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
        }
    }

    /** Reads the segment table from the screen and checks every number. */
    private SegmentTable readTable(long mem) {
        Util.stopEditing(inTable);
        if (in.getRowCount() == 0) throw new IllegalArgumentException("Add at least one segment to the segment table.");
        SegmentTable t = new SegmentTable();
        for (int r = 0; r < in.getRowCount(); r++) {
            long base = parseNumber(Util.str(in.getValueAt(r, 1)), "Base of segment " + r, 0);
            long limit = parseNumber(Util.str(in.getValueAt(r, 2)), "Limit of segment " + r, 1);
            if (base + limit > mem) throw new IllegalArgumentException("segmentation.Segment " + r + " does not fit: base " + base + " + limit " + limit
                    + " = " + (base + limit) + ", but physical memory is only " + mem + " bytes.");
            t.add(base, limit);
        }
        return t;
    }

    /** "2:53 3:852, 0:1222"  ->  [[2,53],[3,852],[0,1222]] */
    private List<long[]> parseAddresses(String text) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Type at least one logical address, written as segment:offset (for example 2:53).");
        List<long[]> list = new ArrayList<>();
        for (String tok : s.split("[\\s,;]+")) {
            Matcher m = ADDRESS.matcher(tok);
            if (!m.matches()) throw new IllegalArgumentException("\"" + tok + "\" is not a logical address. Write it as segment:offset, for example 2:53.");
            try {
                list.add(new long[]{Long.parseLong(m.group(1)), Long.parseLong(m.group(2))});
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The numbers in \"" + tok + "\" are too large.");
            }
        }
        if (list.size() > MAX_ADDRESSES) throw new IllegalArgumentException("At most " + MAX_ADDRESSES + " addresses at a time (got " + list.size() + ").");
        return list;
    }

    private static long parseNumber(String s, String name, long min) {
        s = s == null ? "" : s.trim();
        if (s.isEmpty()) throw new IllegalArgumentException(name + " is blank.");
        long v;
        try {
            v = Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number (got \"" + s + "\").");
        }
        if (v < min) throw new IllegalArgumentException(name + " must be at least " + min + " (got " + v + ").");
        if (v > MAX_NUMBER) throw new IllegalArgumentException(name + " is too large (at most " + MAX_NUMBER + ").");
        return v;
    }

    private void renumber() {
        for (int i = 0; i < in.getRowCount(); i++) in.setValueAt(String.valueOf(i), i, 0);
    }

    private void clearResults() {
        out.setRowCount(0);
        steps.setText("");
        note.setText(" ");
        diagram.clear();
    }

    /** Draws the memory map of the current table (before any address is translated). */
    private void showMap() {
        try {
            long mem = parseNumber(memSize.getText(), "Physical memory size", 1);
            diagram.show(readTable(mem), mem, null);
        } catch (IllegalArgumentException ex) {
            diagram.clear();
        }
    }

    // ---------------------------------------------------------------- small builders
    /** Colours the Status column: green for valid, red for errors. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(tb, v, sel, false, r, c);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Theme.heading(12f));
            if (!sel) setBackground(r % 2 == 0 ? new Color(0x16293F) : new Color(0x1C344F));
            setForeground("Valid".equals(v) ? new Color(0x7BE08F) : new Color(0xFF8A95));
            return this;
        }
    }

    private static JTable tipTable(DefaultTableModel model, String[] tips, boolean dark) {
        JTable t = new JTable(model) {
            @Override protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override public String getToolTipText(MouseEvent e) {
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
