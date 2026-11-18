package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public final class ModuleHeader {
    private ModuleHeader() {
    }


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
        banner.body.setBorder(BorderFactory.createEmptyBorder());
        banner.setPreferredSize(new Dimension(400, 92));
        banner.setMinimumSize(new Dimension(200, 92));

        JPanel runWrap = new JPanel(new GridBagLayout());
        runWrap.setOpaque(false);
        GridBagConstraints rc = new GridBagConstraints();
        rc.anchor = GridBagConstraints.NORTH;
        runWrap.add(run, rc);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(tools, BorderLayout.CENTER);
        bar.add(runWrap, BorderLayout.EAST);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{banner, bar}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            north.add(c);
        }
        return north;
    }

    public static JPanel toolRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setOpaque(false);
        return p;
    }

    public static JButton runButton(String text) {
        JButton run = new Theme.SkinButton(text, "red");
        run.setIcon(Theme.playIcon(12, Theme.CREAM));
        run.setFont(Theme.body(17f).deriveFont(Font.BOLD));
        run.setPreferredSize(new Dimension(190, 44));
        return run;
    }

    public static JButton navyButton(String text, int width) {
        JButton b = new Theme.SkinButton(text, "navy");
        b.setMargin(new Insets(4, 4, 8, 4));
        b.setPreferredSize(new Dimension(width, 40));
        return b;
    }

    public static JButton dangerButton(String text, int width) {
        JButton b = new Theme.SkinButton(text, "danger");
        b.setMargin(new Insets(4, 4, 8, 4));
        b.setPreferredSize(new Dimension(width, 40));
        return b;
    }

    public static JButton withIcon(JButton b, String kind, Color color) {
        return Icons.on(b, kind, color);
    }


    public static JPanel tableButtons(JButton add, JButton del, JButton clr, JTable table, javax.swing.table.DefaultTableModel model,
                                      Runnable doRemove, Runnable doClear, Component parent) {
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

        for (ActionListener l : del.getActionListeners()) del.removeActionListener(l);
        del.addActionListener(e -> {
            int n = table.getSelectedRowCount();
            if (n == 0) return;
            int ok = JOptionPane.showConfirmDialog(parent,
                    "Remove " + (n == 1 ? "the selected row" : "the " + n + " selected rows") + "? This cannot be undone.",
                    "Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) doRemove.run();
        });

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
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(add, c);
        c.gridx = 1;
        c.insets = new Insets(0, 18, 0, 0);
        p.add(del, c);
        c.gridx = 2;
        c.insets = new Insets(0, 6, 0, 0);
        p.add(clr, c);
        return p;
    }

    public static JPanel tableButtons(JButton add, JButton del, JButton clr, JTable table, javax.swing.table.DefaultTableModel model,
                                      Runnable doClear, Component parent) {
        JPanel p = tableButtons(add, del, clr, table, model, () -> {
        }, doClear, parent);
        for (ActionListener l : del.getActionListeners()) del.removeActionListener(l);
        return p;
    }
}