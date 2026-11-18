package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public final class HomePanel extends JPanel {

    public static final class Entry {
        public final String title, description, icon, group, shortcut;
        public final Runnable open;

        public Entry(String title, String description, String icon, String group, String shortcut, Runnable open) {
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.group = group;
            this.shortcut = shortcut;
            this.open = open;
        }
    }

    private static final int COLUMNS = 3;

    public HomePanel(BufferedImage logo, List<Entry> entries, String quickStartLabel, Runnable quickStart,
                     String footerTip) {
        super(new BorderLayout());
        setOpaque(false);

        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));

        page.add(hero(logo, quickStartLabel, quickStart));
        page.add(Box.createVerticalStrut(28));

        page.add(section(entries));
        page.add(Box.createVerticalStrut(22));

        JLabel tip = Theme.label(footerTip, new Color(Theme.CREAM.getRed(), Theme.CREAM.getGreen(), Theme.CREAM.getBlue(), 170), 13f);
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(tip);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(page, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(wrap, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.setBorder(null);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        Theme.styleScroll(sp);
        add(sp, BorderLayout.CENTER);
    }


    private JComponent hero(BufferedImage logo, String quickStartLabel, Runnable quickStart) {
        JPanel hero = new JPanel(new BorderLayout(20, 0));
        hero.setOpaque(false);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (logo != null) {
            JLabel l = new JLabel(new ImageIcon(logo.getScaledInstance(104, 104, Image.SCALE_SMOOTH)));
            l.setVerticalAlignment(SwingConstants.TOP);
            hero.add(l, BorderLayout.WEST);
        }

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("OS SIMULATOR");
        title.setFont(Theme.heading(38f));
        title.setForeground(Theme.CREAM);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = Theme.label("Pick a module to see how an operating system schedules, allocates and manages resources.",
                Theme.GOLD, 16f);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton start = new Theme.SkinButton(quickStartLabel, "red");
        start.setIcon(Theme.playIcon(12, Theme.CREAM));
        start.setFont(Theme.body(16f).deriveFont(Font.BOLD));
        start.setPreferredSize(new Dimension(270, 44));
        start.addActionListener(e -> quickStart.run());
        JPanel startRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        startRow.setOpaque(false);
        startRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        startRow.add(start);

        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(sub);
        text.add(Box.createVerticalStrut(16));
        text.add(startRow);
        hero.add(text, BorderLayout.CENTER);

        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, hero.getPreferredSize().height));
        return hero;
    }


    private JComponent section(List<Entry> items) {
        JPanel sec = new JPanel(new BorderLayout(0, 12)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        sec.setOpaque(false);
        sec.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 14, 14));
        grid.setOpaque(false);
        for (Entry e : items) grid.add(new Card(e));
        int pad = (COLUMNS - items.size() % COLUMNS) % COLUMNS;
        for (int i = 0; i < pad; i++) {
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            grid.add(filler);
        }
        sec.add(grid, BorderLayout.CENTER);
        return sec;
    }


    private static final class Card extends JPanel {
        private final Entry entry;
        private final Icon icon;
        private boolean hover;

        Card(Entry entry) {
            this.entry = entry;
            this.icon = Theme.icon(entry.icon, 30, Theme.INK);
            setOpaque(false);
            setPreferredSize(new Dimension(200, 136));
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(entry.shortcut == null ? null : "Shortcut: " + entry.shortcut);
            getAccessibleContext().setAccessibleName(entry.title);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) entry.open.run();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            Action open = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    entry.open.run();
                }
            };
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "open");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "open");
            getActionMap().put("open", open);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth() - 6, h = getHeight() - 6;      // leave room for the skin's drop shadow
            BufferedImage skin = Theme.image("panels/cream_panel_9slice.png");
            if (skin != null) Theme.nine(g, skin, 0, 0, getWidth(), getHeight(), 30);
            else {
                g.setColor(Theme.CREAM);
                g.fillRoundRect(0, 0, w, h, 16, 16);
            }

            if (hover || isFocusOwner()) {
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(3f));
                g.drawRoundRect(3, 3, w - 6, h - 6, 16, 16);
            }

            int pad = 18;
            icon.paintIcon(this, g, pad, 16);

            g.setFont(Theme.heading(15f));
            g.setColor(Theme.INK);
            g.drawString(entry.title, pad + 42, 38);

            Font bodyFont = Theme.body(13f);
            g.setFont(bodyFont);
            g.setColor(Theme.MUTED);
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = wrap(entry.description, fm, w - pad * 2);
            int y = 68;
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                g.drawString(lines.get(i), pad, y);
                y += fm.getHeight();
            }

            g.setFont(Theme.body(13f).deriveFont(Font.BOLD));
            g.setColor(hover || isFocusOwner() ? Theme.CRIMSON : new Color(0x9A6F6F));
            String cta = "Open  \u203A";
            int cw = g.getFontMetrics().stringWidth(cta);
            g.drawString(cta, w - pad - cw, h - 14);
            g.dispose();
        }

        private static List<String> wrap(String text, FontMetrics fm, int max) {
            List<String> lines = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            for (String word : text.split(" ")) {
                String trial = cur.length() == 0 ? word : cur + " " + word;
                if (fm.stringWidth(trial) > max && cur.length() > 0) {
                    lines.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(trial);
                }
            }
            if (cur.length() > 0) lines.add(cur.toString());
            return lines;
        }
    }
}