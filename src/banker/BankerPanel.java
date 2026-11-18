package banker;

import ui.ModulePanel;
import ui.Theme;
import util.Util;
import ui.Icons;

import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;


public class BankerPanel extends ModulePanel {
    private static final Color HEADER = Theme.CREAM_SHADOW,
            ROW_A = new Color(0xFBEBD2), ROW_B = Theme.CREAM, SAFE_GREEN = new Color(0x1E6B34),
            GRID = new Color(0xD8BE96),
            GROUP_LINE = new Color(0xB99A68),
            EDGE = new Color(0xCDB28A);
    private static final int FIRST_COL_W = 110, FIRST_COL_MIN = 110, COL_MIN = 48, COL_PREF = 56;

    private final JTextField np = new Theme.RoundField("5", 3), nr = new Theme.RoundField("4", 3);
    private final JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
    {
        totalsPanel.setOpaque(false);
    }    private JTextField[] totals = new JTextField[0];
    private final JTable tbl = new JTable() {
        @Override
        protected void configureEnclosingScrollPane() {
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport
                    && getParent().getWidth() >= getPreferredSize().width;
        }
    };
    private DefaultTableModel model;
    private int p = 0, m = 0;
    private boolean updating = false;
    private final JLabel verdict = new JLabel("Set n and m, click Randomize, then click Run Simulation.", SwingConstants.CENTER);

    private int allocCol(int j) {
        return 1 + j;
    }

    private int maxCol(int j) {
        return 1 + m + j;
    }

    private int availCol(int j) {
        return 1 + 2 * m + j;
    }

    private int needCol(int j) {
        return 1 + 3 * m + j;
    }

    public BankerPanel() {
        setLayout(new BorderLayout(10, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

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
        banner.body.setBorder(BorderFactory.createEmptyBorder());
        banner.setPreferredSize(new Dimension(400, 92));
        banner.setMinimumSize(new Dimension(200, 92));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));

        JButton rnd = new Theme.SkinButton("Randomize", "navy"),
                chk = new Theme.SkinButton("Run Simulation", "red");
        rnd.setPreferredSize(new Dimension(130, 40));
        rnd.setToolTipText("Build the table with n processes and m resource types, filled with random values");
        JButton delRow = Icons.on(new Theme.SkinButton("Delete process", "navy"), "trash", Theme.CREAM);
        JButton delCol = Icons.on(new Theme.SkinButton("Delete resource", "navy"), "trash", Theme.CREAM);
        delRow.setPreferredSize(new Dimension(170, 40));
        delCol.setPreferredSize(new Dimension(180, 40));
        delRow.setToolTipText("Delete the selected process (or the last one if none is selected)");
        delCol.setToolTipText("Delete the selected resource type (or the last one if none is selected)");

        chk.setIcon(Theme.playIcon(12, Theme.CREAM));
        chk.setFont(Theme.body(17f).deriveFont(Font.BOLD));
        chk.setPreferredSize(new Dimension(200, 44));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        r1.setOpaque(false);
        r1.add(Theme.label("Processes (n):", Theme.CREAM, 15f));
        r1.add(np);
        r1.add(Theme.label("Resource types (m):", Theme.CREAM, 15f));
        r1.add(nr);
        r1.add(rnd);
        r1.add(delRow);
        r1.add(delCol);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(r1, BorderLayout.CENTER);
        bar.add(chk, BorderLayout.EAST);
        JPanel runWrap = new JPanel(new GridBagLayout());
        runWrap.setOpaque(false);
        GridBagConstraints rc = new GridBagConstraints();
        rc.anchor = GridBagConstraints.NORTH;
        runWrap.add(chk, rc);
        bar.add(runWrap, BorderLayout.EAST);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JScrollPane totalsScroll = new JScrollPane(totalsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        totalsScroll.setOpaque(false);
        totalsScroll.getViewport().setOpaque(false);
        totalsScroll.getHorizontalScrollBar().setUnitIncrement(24);
        totalsScroll.setPreferredSize(new Dimension(400, 56));
        totalsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        Theme.styleScroll(totalsScroll);
        totalsScroll.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
        for (JComponent c : new JComponent[]{banner, bar, totalsScroll}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            north.add(c);
        }
        add(north, BorderLayout.NORTH);

        setupTable();
        JScrollPane tableScroll = Theme.scroll(tbl, ROW_A);
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(tbl.getColumnModel().getTotalColumnWidth(), d.height);
            }
        };
        header.add(new GroupBar(), BorderLayout.NORTH);
        header.add(tbl.getTableHeader(), BorderLayout.CENTER);
        tableScroll.setColumnHeaderView(header);
        tableScroll.getColumnHeader().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 8));
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setOpaque(false);
        tableScroll.setViewportBorder(null);
        Theme.SkinPanel tablePanel = new Theme.SkinPanel("Banker's Table", "diamond", false);
        tablePanel.body.add(tableScroll, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);

        verdict.setFont(Theme.heading(20f));
        verdict.setForeground(Theme.INK);
        verdict.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        Theme.SkinPanel resultPanel = new Theme.SkinPanel("Result", "cpu", false);
        resultPanel.body.add(verdict, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.SOUTH);

        rnd.addActionListener(e -> randomizeData());
        chk.addActionListener(e -> runSimulation());
        delRow.addActionListener(e -> deleteProcess());
        delCol.addActionListener(e -> deleteResource());
        generate();
    }

    private void deleteProcess() {
        Util.stopEditing(tbl);
        java.util.TreeSet<Integer> rows = new java.util.TreeSet<>();
        for (int r : tbl.getSelectedRows()) if (r < p) rows.add(r);
        if (rows.isEmpty()) rows.add(p - 1);

        if (rows.size() >= p) {
            JOptionPane.showMessageDialog(this, "You need at least one process.",
                    "Cannot delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder names = new StringBuilder();
        for (int r : rows) names.append(names.length() == 0 ? "" : ", ").append("P").append(r + 1);
        int ans = JOptionPane.showConfirmDialog(this,
                "Delete " + (rows.size() == 1 ? "process " : "processes ") + names + "?\nThis cannot be undone.",
                "Delete process", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        rebuildWithout(rows, new java.util.TreeSet<>());
    }

    private void deleteResource() {
        Util.stopEditing(tbl);
        java.util.TreeSet<Integer> cols = new java.util.TreeSet<>();
        for (int c : tbl.getSelectedColumns()) if (c >= 1) cols.add((c - 1) % m);
        if (cols.isEmpty()) cols.add(m - 1);

        if (cols.size() >= m) {
            JOptionPane.showMessageDialog(this, "You need at least one resource type.",
                    "Cannot delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder names = new StringBuilder();
        for (int c : cols) names.append(names.length() == 0 ? "" : ", ").append(Util.resourceName(c));
        int ans = JOptionPane.showConfirmDialog(this,
                "Delete " + (cols.size() == 1 ? "resource " : "resources ") + names
                        + "?\nTheir Allocation, Max, Available and Need columns will be removed.",
                "Delete resource", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        rebuildWithout(new java.util.TreeSet<>(), cols);
    }

    private void rebuildWithout(java.util.Set<Integer> dropRows, java.util.Set<Integer> dropCols) {
        String[][] alloc = new String[p][m], mx = new String[p][m];
        String[] tot = new String[m];
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                alloc[i][j] = Util.str(model.getValueAt(i, allocCol(j)));
                mx[i][j] = Util.str(model.getValueAt(i, maxCol(j)));
            }
        for (int j = 0; j < m; j++) tot[j] = totals[j].getText();

        java.util.List<Integer> keepRows = new java.util.ArrayList<>(), keepCols = new java.util.ArrayList<>();
        for (int i = 0; i < p; i++) if (!dropRows.contains(i)) keepRows.add(i);
        for (int j = 0; j < m; j++) if (!dropCols.contains(j)) keepCols.add(j);

        np.setText("" + keepRows.size());
        nr.setText("" + keepCols.size());
        generate();

        updating = true;
        for (int i = 0; i < p; i++)
            for (int j = 0; j < m; j++) {
                model.setValueAt(alloc[keepRows.get(i)][keepCols.get(j)], i, allocCol(j));
                model.setValueAt(mx[keepRows.get(i)][keepCols.get(j)], i, maxCol(j));
            }
        for (int j = 0; j < m; j++) totals[j].setText(tot[keepCols.get(j)]);
        computeLive();
        updating = false;
        tbl.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.paintBackground(g, this);
        super.paintComponent(g);
    }

    @Override
    public String getTitle() {
        return "Banker's Algorithm";
    }


    private static float fitSize(int colWidth, float factor, float max, float min) {
        return Math.max(min, Math.min(max, colWidth * factor));
    }

    private void setupTable() {
        tbl.setRowHeight(36);
        tbl.setFont(Theme.body(15f));
        tbl.setForeground(Theme.INK);
        tbl.setBackground(ROW_A);
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tbl.setShowHorizontalLines(false);
        tbl.setShowVerticalLines(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setGridColor(GRID);
        tbl.setCellSelectionEnabled(true);
        tbl.setSelectionBackground(Theme.GOLD);
        tbl.setSelectionForeground(Theme.INK);
        tbl.setFillsViewportHeight(false);
        tbl.getTableHeader().setReorderingAllowed(false);
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 34));
        tbl.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(HEADER);
                setForeground(Theme.INK);
                setFont(Theme.heading(fitSize(t.getColumnModel().getColumn(c).getWidth(), 0.45f, 13f, 7f)));
                setBorder(groupBorder(c, 0, true, false, t.getColumnCount()));
                return this;
            }
        });
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(Theme.body(fitSize(t.getColumnModel().getColumn(c).getWidth(), 0.5f, 16f, 8f)).deriveFont(Font.BOLD));
                if (!sel) {
                    setBackground(r % 2 == 0 ? ROW_A : ROW_B);
                    setForeground(Theme.INK);
                }
                setBorder(groupBorder(c, r == p && t.getRowCount() > p ? 1 : 0, false,
                        r == t.getRowCount() - 1, t.getColumnCount()));
                return this;
            }
        });
        java.awt.event.ComponentAdapter relayout = new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                repaintHeader();
            }
        };
        tbl.addComponentListener(relayout);
        tbl.getTableHeader().addComponentListener(relayout);
        tbl.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) {
                repaintHeader();
            }

            public void columnRemoved(TableColumnModelEvent e) {
                repaintHeader();
            }

            public void columnMoved(TableColumnModelEvent e) {
            }

            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                repaintHeader();
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });
    }

    private void repaintHeader() {
        Container parent = tbl.getTableHeader().getParent();
        if (parent != null) parent.repaint();
    }

    private javax.swing.border.Border groupBorder(int c, int topLine, boolean header, boolean lastRow, int cols) {
        boolean groupStart = m > 0 && (c == allocCol(0) || c == maxCol(0) || c == availCol(0) || c == needCol(0));
        boolean lastCol = c == cols - 1;
        int left = (c == 0 || !groupStart) ? 1 : 2;
        javax.swing.border.Border b = BorderFactory.createMatteBorder(0, left, 0, 0,
                c == 0 ? EDGE : groupStart ? GROUP_LINE : GRID);
        if (lastCol)
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(0, 0, 0, 1, EDGE));
        if (header || lastRow)
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(0, 0, 1, 0, GROUP_LINE));
        if (topLine > 0)
            b = BorderFactory.createCompoundBorder(b, BorderFactory.createMatteBorder(topLine, 0, 0, 0, GRID));
        return b;
    }

    private class GroupBar extends JComponent {
        GroupBar() {
            setPreferredSize(new Dimension(100, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            JTableHeader th = tbl.getTableHeader();
            int cols = tbl.getColumnModel().getColumnCount();
            String[] names = {"", "Allocation", "Max", "Available", "Need"};
            int[] starts = {0, 1, 1 + m, 1 + 2 * m, 1 + 3 * m, 1 + 4 * m};
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.heading(14f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(HEADER);
            g2.fillRect(0, 0, getWidth(), getHeight());
            for (int k = 0; k < 5; k++) {
                if (starts[k] >= cols) break;
                int last = Math.min(starts[k + 1], cols) - 1;
                int x = th.getHeaderRect(starts[k]).x;
                int w = th.getHeaderRect(last).x + th.getHeaderRect(last).width - x;
                if (k > 0) {
                    g2.setColor(GROUP_LINE);
                    g2.fillRect(x, 0, 2, getHeight());
                }
                g2.setColor(Theme.INK);
                Rectangle vis = getVisibleRect();
                int vx = Math.max(x, vis.x);
                int vw = Math.min(x + w, vis.x + vis.width) - vx;
                if (vw > 0 && !names[k].isEmpty())
                    g2.drawString(names[k], vx + (vw - fm.stringWidth(names[k])) / 2, getHeight() / 2 + fm.getAscent() / 2 - 1);
            }
            int endX = th.getHeaderRect(cols - 1).x + th.getHeaderRect(cols - 1).width;
            g2.setColor(EDGE);
            g2.fillRect(endX - 1, 0, 1, getHeight());
            g2.setColor(GRID);
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }

    private void generate() {
        int np2 = Util.parseInt(np.getText(), "Number of processes", 1);
        int nr2 = Util.parseInt(nr.getText(), "Number of resource types", 1);
        p = np2;
        m = nr2;
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
            row[0] = "P" + (i + 1);
            for (int c = 1; c < cols.length; c++) {
                if (c < maxCol(0)) row[c] = "0";
                else if (c < availCol(0)) row[c] = "1";
                else if (c < needCol(0)) row[c] = (i == 0 ? "1" : "");
                else row[c] = "0";
            }
            model.addRow(row);
        }
        model.addTableModelListener(e -> userEdited());
        tbl.setModel(model);
        TableColumnModel cm = tbl.getColumnModel();
        cm.getColumn(0).setMinWidth(FIRST_COL_MIN);
        cm.getColumn(0).setPreferredWidth(FIRST_COL_W);
        for (int c = 1; c < model.getColumnCount(); c++) {
            cm.getColumn(c).setMinWidth(COL_MIN);
            cm.getColumn(c).setPreferredWidth(COL_PREF);
        }
        tbl.revalidate();
        buildTotals();
        setVerdict("set n and m, click Randomize, then click Run Simulation.", Theme.INK);
        Container hp = tbl.getTableHeader().getParent();
        if (hp != null) {
            hp.revalidate();
            hp.repaint();
        }
    }

    private void setVerdict(String text, Color color) {
        verdict.setText(text);
        verdict.setForeground(color);
    }

    private boolean generateSafely() {
        try {
            generate();
            return true;
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
            return false;
        }
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
        for (int j = 0; j < m; j++) {
            int used = 0;
            for (int i = 0; i < p; i++) used += Integer.parseInt((String) model.getValueAt(i, allocCol(j)));
            totals[j].setText("" + Math.max(1, used + Util.RNG.nextInt(6)));
        }
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
                String where = "P" + (i + 1) + " " + Util.resourceName(j);
                a[i][j] = Util.parseInt(Util.str(model.getValueAt(i, allocCol(j))), "Allocation of " + where, 0);
                mx[i][j] = Util.parseInt(Util.str(model.getValueAt(i, maxCol(j))), "Max of " + where, 1);
            }
        for (int j = 0; j < m; j++) {
            int total = Util.parseInt(totals[j].getText(), "Total " + Util.resourceName(j), 1), used = 0;
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

    private void computeLive() {
        computeLiveNeed();
        computeLiveAvailable();
    }

    private void computeLiveAvailable() {
        for (int j = 0; j < m; j++) {
            String av = "";
            try {
                int total = Integer.parseInt(totals[j].getText().trim()), used = 0;
                for (int i = 0; i < p; i++) used += Integer.parseInt(Util.str(model.getValueAt(i, allocCol(j))).trim());
                if (total >= used) av = "" + (total - used);
            } catch (NumberFormatException ignored) {
            }
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
            totals[j] = new Theme.RoundField("1", 3);
            totals[j].setHorizontalAlignment(SwingConstants.CENTER);
            totals[j].getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    userEdited();
                }

                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    userEdited();
                }

                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    userEdited();
                }
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
                } catch (NumberFormatException ignored) {
                }
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
            model.setValueAt("P" + (i + 1), i, 0);
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
                model.setValueAt(t.ok ? "P" + (i + 1) : "P" + (i + 1) + " (skipped)", i, 0);
            } else {
                Object[] row = new Object[1 + 4 * m];
                row[0] = "P" + (i + 1) + (t.ok ? " (again)" : " (skipped)");
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
        for (int i : s.stuck) sb.append(" P").append(i + 1);
        return sb.append("   (possible deadlock)").toString();
    }

    @Override
    protected void runSimulation() {
        BankersAlgorithm b;
        try {
            b = readState();
        } catch (IllegalArgumentException ex) {
            Util.error(this, ex.getMessage());
            return;
        }
        BankersAlgorithm.SafetyResult s = b.checkSafety();
        updating = true;
        showNeed(b);
        fillWorking(s);
        updating = false;
        setVerdict(verdict(s), s.safe ? SAFE_GREEN : Theme.CRIMSON);
        tbl.repaint();
    }
}