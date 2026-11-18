package ossim;

import banker.BankerPanel;
import cpu.CpuPanel;
import disk.DiskPanel;
import fragmentation.FragmentationPanel;
import multiprogramming.MultiprogrammingPanel;
import paging.PagingPanel;
import replacement.ReplacementPanel;
import segmentation.SegmentationPanel;
import ui.HomePanel;
import ui.Icons;
import ui.ModulePanel;
import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OSSimulator extends JFrame {
    private static final String[] ICONS = {"spade", "diamond", "club", "heart", "cards", "hourglass", "memory", "gear"};

    private static final String[] GROUPS = {
            "Processes & CPU", "Processes & CPU", "Memory management", "Memory management",
            "Memory management", "Memory management", "Processes & CPU", "Storage"};
    private static final String[] DESCRIPTIONS = {
            "Compare scheduling algorithms and watch the timeline unfold.",
            "Check whether a state is safe and find a safe sequence.",
            "Translate logical addresses into physical frames.",
            "Run page replacement policies and count the page faults.",
            "Map segment and offset addresses, and catch limit errors.",
            "See where memory gets wasted with different fit strategies.",
            "Explore CPU utilization as the degree of multiprogramming grows.",
            "Compare disk head scheduling and total head movement."};

    private static final String HOME = "__home__";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Theme.SkinButton> nav = new LinkedHashMap<>();
    private JPanel west;

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
        content.setOpaque(false);

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        String mod = InputEvent.getModifiersExText(mask);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 8));
        side.setPreferredSize(new Dimension(250, 100));
        side.add(logo());
        side.add(Box.createVerticalStrut(14));

        Theme.SkinButton homeBtn = new Theme.SkinButton("Home", "navy");
        homeBtn.setIcon(Icons.glyph("home", Theme.CREAM));
        homeBtn.setIconTextGap(11);
        homeBtn.setHorizontalAlignment(SwingConstants.LEFT);
        homeBtn.setMargin(new Insets(4, 19, 8, 10));
        homeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        homeBtn.setPreferredSize(new Dimension(226, 50));
        homeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        homeBtn.setToolTipText("Main menu  (" + mod + "+0)");
        homeBtn.addActionListener(e -> show(HOME));
        nav.put(HOME, homeBtn);
        side.add(homeBtn);
        side.add(Box.createVerticalStrut(14));

        List<HomePanel.Entry> entries = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            final int index = i;
            ModulePanel m = modules[i];
            String key = m.getTitle();
            content.add(m, key);

            Theme.SkinButton b = new Theme.SkinButton(m.getTitle(), "navy");
            b.setIcon(Theme.icon(ICONS[i % ICONS.length], 22, Theme.CREAM));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMargin(new Insets(4, 16, 8, 10));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setPreferredSize(new Dimension(226, 50));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            b.setToolTipText(mod + "+" + (index + 1));
            b.addActionListener(e -> show(key));
            nav.put(key, b);
            side.add(b);
            side.add(Box.createVerticalStrut(6));

            entries.add(new HomePanel.Entry(m.getTitle(), DESCRIPTIONS[i], ICONS[i % ICONS.length], GROUPS[i],
                    mod + "+" + (index + 1), () -> show(key)));

            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, mask);
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "go" + i);
            root.getActionMap().put("go" + i, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    show(key);
                }
            });
        }
        side.add(Box.createVerticalGlue());

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_0, mask), "home");
        root.getActionMap().put("home", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(HOME);
            }
        });

        String first = modules[0].getTitle();
        HomePanel home = new HomePanel(Theme.image("logos/os_simulator_jester_card_logo.png"), entries,
                "Quick start: " + first, () -> show(first),
                "Tip: " + mod + "+1 to " + mod + "+" + modules.length + " jump between modules, " + mod + "+0 returns here.");
        content.add(home, HOME);

        JPanel divider = new JPanel();
        divider.setBackground(new Color(0x050D18));
        divider.setPreferredSize(new Dimension(2, 10));

        west = new JPanel(new BorderLayout());
        west.setOpaque(false);
        west.add(side, BorderLayout.CENTER);
        west.add(divider, BorderLayout.EAST);

        root.add(west, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        show(HOME);
    }

    private void show(String key) {
        cards.show(content, key);
        west.setVisible(!HOME.equals(key));
        for (Map.Entry<String, Theme.SkinButton> e : nav.entrySet())
            e.getValue().setKind(e.getKey().equals(key) ? "red" : "navy");
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

        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText("Main menu");
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                show(HOME);
            }
        });
        return p;
    }
}