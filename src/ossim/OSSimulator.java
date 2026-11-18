package ossim;

import banker.BankerPanel;
import cpu.CpuPanel;
import disk.DiskPanel;
import fragmentation.FragmentationPanel;
import ui.ModulePanel;
import multiprogramming.MultiprogrammingPanel;
import paging.PagingPanel;
import replacement.ReplacementPanel;
import segmentation.SegmentationPanel;
import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class OSSimulator extends JFrame {
    private static final String[] ICONS = {"spade", "diamond", "club", "heart", "cards", "hourglass", "memory", "gear"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OSSimulator().setVisible(true));
    }

    public OSSimulator() {
        super("OS Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ModulePanel[] modules = {new CpuPanel(), new BankerPanel(), new PagingPanel(), new ReplacementPanel(),
                new SegmentationPanel(), new FragmentationPanel(), new MultiprogrammingPanel(), new DiskPanel()};

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Theme.paintBackground(g, this);
            }
        };

        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setOpaque(false);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 8));
        side.setPreferredSize(new Dimension(250, 100));
        side.add(logo());
        side.add(Box.createVerticalStrut(14));

        List<Theme.SkinButton> buttons = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            ModulePanel m = modules[i];
            content.add(m, m.getTitle());
            Theme.SkinButton b = new Theme.SkinButton(m.getTitle(), "navy");
            b.setIcon(Theme.icon(ICONS[i % ICONS.length], 22, Theme.CREAM));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMargin(new Insets(4, 16, 8, 10));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setPreferredSize(new Dimension(226, 50));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            b.addActionListener(e -> {
                for (Theme.SkinButton o : buttons) o.setKind(o == b ? "red" : "navy");
                cards.show(content, m.getTitle());
            });
            buttons.add(b);
            side.add(b);
            side.add(Box.createVerticalStrut(6));
        }
        side.add(Box.createVerticalGlue());
        buttons.get(0).setKind("red");

        JPanel divider = new JPanel();
        divider.setBackground(new Color(0x050D18));
        divider.setPreferredSize(new Dimension(2, 10));

        JPanel west = new JPanel(new BorderLayout());
        west.setOpaque(false);
        west.add(side, BorderLayout.CENTER);
        west.add(divider, BorderLayout.EAST);

        root.add(west, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
    }

    private JComponent logo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        BufferedImage img = Theme.image("logos/os_simulator_jester_card_logo.png");
        if (img != null) p.add(new JLabel(new ImageIcon(img.getScaledInstance(56, 56, Image.SCALE_SMOOTH))));
        JLabel name = new JLabel("<html>OS &nbsp;&#8226;<br>SIMULATOR</html>");
        name.setFont(Theme.heading(18f));
        name.setForeground(Theme.CREAM);
        p.add(name);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        return p;
    }
}
