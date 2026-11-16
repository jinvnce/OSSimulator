package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/** The title banner + tool bar that sits at the top of a module (same look as the CPU Scheduling screen). */
public final class ModuleHeader {
    private ModuleHeader() { }

    /**
     * @param tools the left part of the tool bar (labels, boxes, drop-downs)
     * @param run   the red main button on the right
     */
    public static JPanel north(String title, String subtitle, String iconName, JComponent tools, JButton run) {
        Theme.SkinPanel banner = new Theme.SkinPanel(null, null, false);
        banner.body.setLayout(new BorderLayout(12, 0));
        banner.body.add(new JLabel(Theme.icon(iconName, 30, Theme.INK)), BorderLayout.WEST);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.heading(28f));
        t.setForeground(Theme.INK);
        titles.add(t);
        titles.add(Theme.label(subtitle, Theme.MUTED, 14f));
        banner.body.add(titles, BorderLayout.CENTER);
        banner.setBorder(BorderFactory.createEmptyBorder(8, 14, 16, 18));
        banner.setPreferredSize(new Dimension(400, 86));
        banner.setMinimumSize(new Dimension(200, 86));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(tools, BorderLayout.CENTER);
        bar.add(run, BorderLayout.EAST);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{banner, bar}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            north.add(c);
        }
        return north;
    }

    /** A row of tool bar items on the dark background. */
    public static JPanel toolRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setOpaque(false);
        return p;
    }

    /** The red main button. */
    public static JButton runButton(String text) {
        JButton run = new Theme.SkinButton(text, "red");
        run.setIcon(Theme.playIcon(12, Theme.CREAM));
        run.setFont(Theme.body(17f).deriveFont(Font.BOLD));
        run.setPreferredSize(new Dimension(190, 44));
        return run;
    }

    /** A small navy button used in cards. */
    public static JButton navyButton(String text, int width) {
        JButton b = new Theme.SkinButton(text, "navy");
        b.setMargin(new Insets(4, 4, 8, 4));
        b.setPreferredSize(new Dimension(width, 40));
        return b;
    }

    /** A destructive action (Remove, Clear all): cream button with red text, so it never looks like a safe action. */
    public static JButton dangerButton(String text, int width) {
        JButton b = new Theme.SkinButton(text, "danger");
        b.setMargin(new Insets(4, 4, 8, 4));
        b.setPreferredSize(new Dimension(width, 40));
        return b;
    }

    /**
     * The Add / Remove / Clear row used under every editable table: Add (safe, navy) on the left,
     * then a gap, then the two destructive buttons. Remove is only enabled when a row is selected,
     * Clear all only when the table has rows, and Clear all asks before it deletes.
     */
    public static JPanel tableButtons(JButton add, JButton del, JButton clr, JTable table, javax.swing.table.DefaultTableModel model,
                                      Runnable doClear, Component parent) {
        add.setToolTipText("Add a new row");
        del.setToolTipText("Remove the selected row(s)");
        clr.setToolTipText("Delete every row");
        Runnable refresh = () -> {
            del.setEnabled(table.getSelectedRowCount() > 0);
            clr.setEnabled(model.getRowCount() > 0);
        };
        table.getSelectionModel().addListSelectionListener(e -> refresh.run());
        model.addTableModelListener(e -> refresh.run());
        refresh.run();

        for (ActionListener l : clr.getActionListeners()) clr.removeActionListener(l);
        clr.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(parent, "Delete all " + model.getRowCount() + " rows? This cannot be undone.",
                    "Clear all", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) doClear.run();
        });

        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.insets = new Insets(0, 0, 0, 0); p.add(add, c);
        c.gridx = 1; c.insets = new Insets(0, 18, 0, 0); p.add(del, c);     // wider gap = separates safe from destructive
        c.gridx = 2; c.insets = new Insets(0, 6, 0, 0); p.add(clr, c);
        return p;
    }
}
