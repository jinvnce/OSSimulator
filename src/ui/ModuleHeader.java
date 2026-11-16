package ui;

import javax.swing.*;
import java.awt.*;

/** The title banner + tool bar that sits at the top of a module (same look as the CPU Scheduling screen). */
final class ModuleHeader {
    private ModuleHeader() { }

    /**
     * @param tools the left part of the tool bar (labels, boxes, drop-downs)
     * @param run   the red main button on the right
     */
    static JPanel north(String title, String subtitle, String iconName, JComponent tools, JButton run) {
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
    static JPanel toolRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setOpaque(false);
        return p;
    }

    /** The red main button. */
    static JButton runButton(String text) {
        JButton run = new Theme.SkinButton(text, "red");
        run.setIcon(Theme.playIcon(12, Theme.CREAM));
        run.setFont(Theme.body(17f).deriveFont(Font.BOLD));
        run.setPreferredSize(new Dimension(190, 44));
        return run;
    }

    /** A small navy button used in cards. */
    static JButton navyButton(String text, int width) {
        JButton b = new Theme.SkinButton(text, "navy");
        b.setMargin(new Insets(4, 4, 8, 4));
        b.setPreferredSize(new Dimension(width, 40));
        return b;
    }
}
