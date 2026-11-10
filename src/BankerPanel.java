import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;


public class BankerPanel extends ModulePanel {
    private static final Color LINE = Theme.INK, HEADER = Theme.CREAM_SHADOW,
            ROW_A = new Color(0xFBEBD2), ROW_B = Theme.CREAM, SAFE_GREEN = new Color(0x1E6B34);

    private final JTextField np = new Theme.RoundField("5", 3), nr = new Theme.RoundField("4", 3);
    private final JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));   // one box per resource type
    private JTextField[] totals = new JTextField[0];
    private final JTable tbl = new JTable() {
        @Override protected void configureEnclosingScrollPane() { }
    };
    private DefaultTableModel model;
    private int p = 0, m = 0;
    private boolean updating = false;
    private final JLabel verdict = new JLabel("Click Check Safety to see the result.", SwingConstants.CENTER);

    private int allocCol(int j) { return 1 + j; }
    private int maxCol(int j) { return 1 + m + j; }
    private int availCol(int j) { return 1 + 2 * m + j; }
    private int needCol(int j) { return 1 + 3 * m + j; }

    public BankerPanel() {
        setLayout(new BorderLayout(10, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // title banner
        Theme.SkinPanel banner = new Theme.SkinPanel(null, null, false);
        banner.body.setLayout(new BorderLayout(12, 0));
        banner.body.add(new JLabel(Theme.icon("diamond", 30, Theme.INK)), BorderLayout.WEST);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Banker's Algorithm");
        title.setFont(Theme.heading(28f));
        title.setForeground(Theme.INK);
        titles.add(title);
        titles.add(Theme.label("Check if the system is in a safe state.", Theme.MUTED, 14f));
        banner.body.add(titles, BorderLayout.CENTER);
        banner.setBorder(BorderFactory.createEmptyBorder(8, 14, 16, 18));
        banner.setPreferredSize(new Dimension(400, 86));
        banner.setMinimumSize(new Dimension(200, 86));

        // toolbar
        JButton gen = new Theme.SkinButton("Generate Tables", "navy"), rnd = new Theme.SkinButton("Randomize", "navy"),
                sample = new Theme.SkinButton("Load Sample", "navy"), chk = new Theme.SkinButton("Check Safety", "red");
        gen.setPreferredSize(new Dimension(160, 40));
        rnd.setPreferredSize(new Dimension(130, 40));
        sample.setPreferredSize(new Dimension(140, 40));
        chk.setIcon(Theme.playIcon(12, Theme.CREAM));
        chk.setFont(Theme.body(17f).deriveFont(Font.BOLD));
        chk.setPreferredSize(new Dimension(200, 44));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        r1.setOpaque(false);
        r1.add(Theme.label("Processes (n):", Theme.CREAM, 15f)); r1.add(np);
        r1.add(Theme.label("Resource types (m):", Theme.CREAM, 15f)); r1.add(nr);
        r1.add(gen); r1.add(rnd); r1.add(sample);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(r1, BorderLayout.CENTER);
        bar.add(chk, BorderLayout.EAST);
        totalsPanel.setOpaque(false);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{banner, bar, totalsPanel}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            north.add(c);
        }
        add(north, BorderLayout.NORTH);

        setupTable();
        JPanel header = new JPanel(new BorderLayout());
        header.add(new GroupBar(), BorderLayout.NORTH);
        header.add(tbl.getTableHeader(), BorderLayout.CENTER);
        JScrollPane tableScroll = Theme.scroll(tbl, ROW_A);
        tableScroll.setColumnHeaderView(header);
        Theme.SkinPanel tablePanel = new Theme.SkinPanel("Banker's Table", "diamond", false);
        tablePanel.body.add(tableScroll, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);

        verdict.setFont(Theme.heading(20f));
        verdict.setForeground(Theme.INK);
        verdict.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        Theme.SkinPanel resultPanel = new Theme.SkinPanel("Result", "cpu", false);
        resultPanel.body.add(verdict, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.SOUTH);

        gen.addActionListener(e -> generateSafely());
        rnd.addActionListener(e -> randomizeData());
        sample.addActionListener(e -> loadSample());
        chk.addActionListener(e -> runSimulation());
        generate();
    }

    @Override protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
        super.paintComponent(g);
    }

    @Override public String getTitle() { return "Banker's Algorithm"; }

    private void setupTable() {
        tbl.setRowHeight(30);
        tbl.setFont(Theme.body(15f));
        tbl.setForeground(Theme.INK);
        tbl.setBackground(ROW_A);
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tbl.setShowGrid(true);
        tbl.setGridColor(new Color(0xD8BE96));
        tbl.setCellSelectionEnabled(true);
        tbl.setSelectionBackground(Theme.GOLD);
        tbl.setSelectionForeground(Theme.INK);
        tbl.setFillsViewportHeight(true);
        tbl.getTableHeader().setReorderingAllowed(false);
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tbl.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(c == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBackground(HEADER);
                setForeground(Theme.INK);
                setFont(Theme.heading(13f));
                setBorder(groupBorder(c, 0));
                return this;
            }
        });
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setHorizontalAlignment(c == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setFont(c == 0 ? Theme.heading(13f) : Theme.body(15f));
                if (!sel) { setBackground(r % 2 == 0 ? ROW_A : ROW_B); setForeground(Theme.INK); }
                setBorder(groupBorder(c, r == p && t.getRowCount() > p ? 2 : 0));   // a line where the table "goes back around"
                return this;
            }
        });
        tbl.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) { repaintHeader(); }
            public void columnRemoved(TableColumnModelEvent e) { repaintHeader(); }
            public void columnMoved(TableColumnModelEvent e) { }
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) { repaintHeader(); }
            public void columnSelectionChanged(ListSelectionEvent e) { }
        });
    }

    private void repaintHeader() {
        Container parent = tbl.getTableHeader().getParent();
        if (parent != null) parent.repaint();
    }

    private MatteBorder groupBorder(int c, int topLine) {
        boolean first = m > 0 && (c == allocCol(0) || c == maxCol(0) || c == availCol(0) || c == needCol(0));
        return BorderFactory.createMatteBorder(topLine, first ? 2 : 0, 0, 0, LINE);
    }

    private class GroupBar extends JComponent {
        GroupBar() { setPreferredSize(new Dimension(100, 28)); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            TableColumnModel cm = tbl.getColumnModel();
            String[] names = {"", "Allocation", "Max", "Available", "Need"};
            int[] starts = {0, 1, 1 + m, 1 + 2 * m, 1 + 3 * m, 1 + 4 * m};
            int x = 0;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.heading(14f));
            FontMetrics fm = g2.getFontMetrics();
            for (int k = 0; k < 5; k++) {
                int w = 0;
                for (int c = starts[k]; c < starts[k + 1] && c < cm.getColumnCount(); c++) w += cm.getColumn(c).getWidth();
                g2.setColor(HEADER);
                g2.fillRect(x, 0, w, getHeight());
                g2.setColor(LINE);
                g2.drawLine(x, 0, x, getHeight());
                g2.drawString(names[k], x + (w - fm.stringWidth(names[k])) / 2, getHeight() / 2 + fm.getAscent() / 2 - 1);
                x += w;
            }
            g2.setColor(HEADER);
            g2.fillRect(x, 0, Math.max(0, getWidth() - x), getHeight());
        }
    }

    private void generate() {
        int np2 = Util.parseInt(np.getText(), "Number of processes", 1);
        int nr2 = Util.parseInt(nr.getText(), "Number of resource types", 1);
        p = np2; m = nr2;
        String[] cols = new String[1 + 4 * m];
        cols[0] = "Process";
        for (int g = 0; g < 4; g++) for (int j = 0; j < m; j++) cols[1 + g * m + j] = Util.resourceName(j);
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int c) {      // the extra "go back around" rows are results, not input
                return row < p && c >= allocCol(0) && c < availCol(0);
            }
        };
        for (int i = 0; i < p; i++) {
            Object[] row = new Object[cols.length];
            row[0] = "P" + i;
            for (int c = 1; c < cols.length; c++) row[c] = (c < availCol(0)) ? "0" : (c < needCol(0) ? (i == 0 ? "0" : "") : "0");
            model.addRow(row);
        }
        model.addTableModelListener(e -> userEdited());
        tbl.setModel(model);
        tbl.getColumnModel().getColumn(0).setMinWidth(120);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(120);
        for (int c = 1; c < model.getColumnCount(); c++) tbl.getColumnModel().getColumn(c).setPreferredWidth(44);
        buildTotals();
        setVerdict("Click Check Safety to see the result.", Theme.INK);
        Container hp = tbl.getTableHeader().getParent();
        if (hp != null) { hp.revalidate(); hp.repaint(); }
    }

    private void setVerdict(String text, Color color) {
        verdict.setText(text);
        verdict.setForeground(color);
    }

    private boolean generateSafely() {
        try { generate(); return true; }
        catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return false; }
    }

    @Override
    protected void randomizeData() {
        if (!generateSafely()) return;
        updating = true;
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                int mx = 1 + Util.RNG.nextInt(9);
                model.setValueAt("" + mx, i, maxCol(j));
                model.setValueAt("" + Util.RNG.nextInt(mx + 1), i, allocCol(j));
            }
        for (int j = 0; j < m; j++) {                       // total = what is allocated + a few free ones
            int used = 0;
            for (int i = 0; i < p; i++) used += Integer.parseInt((String) model.getValueAt(i, allocCol(j)));
            totals[j].setText("" + (used + Util.RNG.nextInt(6)));
        }
        computeLive();
        updating = false;
        tbl.repaint();
    }

    private void loadSample() {
        np.setText("5"); nr.setText("4");
        generate();
        int[][] a = {{0, 0, 1, 2}, {1, 0, 0, 0}, {1, 3, 5, 4}, {0, 6, 3, 2}, {0, 0, 1, 4}};
        int[][] mx = {{0, 0, 1, 2}, {1, 7, 5, 0}, {2, 3, 5, 6}, {0, 6, 5, 2}, {0, 6, 5, 6}};
        int[] total = {3, 14, 12, 12};                      // Available = total - allocated = 1 5 2 0
        updating = true;
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 4; j++) { model.setValueAt("" + a[i][j], i, allocCol(j)); model.setValueAt("" + mx[i][j], i, maxCol(j)); }
        for (int j = 0; j < 4; j++) totals[j].setText("" + total[j]);
        computeLive();
        updating = false;
        tbl.repaint();
    }

    private BankersAlgorithm readState() {
        Util.stopEditing(tbl);
        int[][] a = new int[p][m], mx = new int[p][m];
        int[] av = new int[m];
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                String where = "P" + i + " " + Util.resourceName(j);
                a[i][j] = Util.parseInt(Util.str(model.getValueAt(i, allocCol(j))), "Allocation of " + where, 0);
                mx[i][j] = Util.parseInt(Util.str(model.getValueAt(i, maxCol(j))), "Max of " + where, 0);
            }
        for (int j = 0; j < m; j++) {
            int total = Util.parseInt(totals[j].getText(), "Total " + Util.resourceName(j), 0), used = 0;
            for (int i = 0; i < p; i++) used += a[i][j];
            if (used > total)
                throw new IllegalArgumentException("Total " + Util.resourceName(j) + " is " + total + " but " + used
                        + " is already allocated. The total cannot be less than the allocation.");
            av[j] = total - used;
        }
        return new BankersAlgorithm(a, mx, av);
    }

    private void userEdited() {
        if (updating) return;
        updating = true;
        clearWorking();
        computeLive();
        updating = false;
        tbl.repaint();
    }

    private void computeLive() { computeLiveNeed(); computeLiveAvailable(); }

    private void computeLiveAvailable() {
        for (int j = 0; j < m; j++) {
            String av = "";
            try {
                int total = Integer.parseInt(totals[j].getText().trim()), used = 0;
                for (int i = 0; i < p; i++) used += Integer.parseInt(Util.str(model.getValueAt(i, allocCol(j))).trim());
                if (total >= used) av = "" + (total - used);
            } catch (NumberFormatException ignored) { }
            model.setValueAt(av, 0, availCol(j));
        }
    }

    private void buildTotals() {
        totalsPanel.removeAll();
        totals = new JTextField[m];
        JLabel title = Theme.label("Total resources:", Theme.CREAM, 15f);
        title.setToolTipText("How many of each resource exist in the whole system. Available = Total - everyone's Allocation.");
        totalsPanel.add(title);
        for (int j = 0; j < m; j++) {
            totals[j] = new Theme.RoundField("0", 3);
            totals[j].setHorizontalAlignment(SwingConstants.CENTER);
            totals[j].getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { userEdited(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { userEdited(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { userEdited(); }
            });
            totalsPanel.add(Theme.label(Util.resourceName(j) + ":", Theme.CREAM, 15f));
            totalsPanel.add(totals[j]);
        }
        totalsPanel.revalidate();
        totalsPanel.repaint();
    }

    private void computeLiveNeed() {
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                String need = "";
                try {
                    int a = Integer.parseInt(Util.str(model.getValueAt(i, allocCol(j))).trim());
                    int mx = Integer.parseInt(Util.str(model.getValueAt(i, maxCol(j))).trim());
                    if (a >= 0 && mx >= a) need = "" + (mx - a);
                } catch (NumberFormatException ignored) { }
                model.setValueAt(need, i, needCol(j));
            }
    }

    private void showNeed(BankersAlgorithm b) {
        for (int i = 0; i < b.getProcessCount(); i++) {
            int[] n = b.getNeed(i);
            for (int j = 0; j < n.length; j++) model.setValueAt("" + n[j], i, needCol(j));
        }
    }

    private void clearWorking() {
        for (int r = model.getRowCount() - 1; r >= p; r--) model.removeRow(r);
        for (int i = 0; i < p; i++) {
            model.setValueAt("P" + i, i, 0);
            if (i > 0) for (int j = 0; j < m; j++) model.setValueAt("", i, availCol(j));
        }
    }

    private void fillWorking(BankersAlgorithm.SafetyResult s) {
        clearWorking();
        boolean[] seen = new boolean[p];
        for (BankersAlgorithm.TraceRow t : s.trace) {
            int i = t.process;
            if (!seen[i]) {
                seen[i] = true;
                for (int j = 0; j < m; j++) if (i > 0) model.setValueAt("" + t.workBefore[j], i, availCol(j));
                model.setValueAt(t.ok ? "P" + i : "P" + i + " (skipped)", i, 0);
            } else {
                Object[] row = new Object[1 + 4 * m];
                row[0] = "P" + i + (t.ok ? " (again)" : " (skipped)");
                for (int j = 0; j < m; j++) {
                    row[allocCol(j)] = model.getValueAt(i, allocCol(j));
                    row[maxCol(j)] = model.getValueAt(i, maxCol(j));
                    row[availCol(j)] = "" + t.workBefore[j];
                    row[needCol(j)] = "" + t.need[j];
                }
                model.addRow(row);
            }
        }
    }

    private String verdict(BankersAlgorithm.SafetyResult s) {
        if (s.safe) return "SAFE.   Safe sequence: " + s.sequenceText();
        StringBuilder sb = new StringBuilder("NOT SAFE.   Stuck:");
        for (int i : s.stuck) sb.append(" P").append(i);
        return sb.append("   (possible deadlock)").toString();
    }

    @Override
    protected void runSimulation() {
        BankersAlgorithm b;
        try { b = readState(); } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }
        BankersAlgorithm.SafetyResult s = b.checkSafety();
        updating = true;
        showNeed(b);
        fillWorking(s);
        updating = false;
        setVerdict(verdict(s), s.safe ? SAFE_GREEN : Theme.CRIMSON);
        tbl.repaint();
    }
}