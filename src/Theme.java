import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Joker / playing-card look for the OS Simulator: colors, fonts, image skins and styled components.
 *
 * Put the asset pack so that ONE of these works (first match wins):
 *   1. classpath:  /assets/...            (e.g. src/main/resources/assets/)
 *   2. ./assets/...                        (next to where you run the program)
 *   3. ./OS_Simulator_Individual_Assets/...
 * Missing images never crash the UI; the panels just fall back to flat colors.
 */
public final class Theme {
    private Theme() {}

    public static final Color NAVY_BG = new Color(0x0F2035), NAVY_PANEL = new Color(0x193049), DEEP = new Color(0x142337),
            CREAM = new Color(0xF4E0C1), CREAM_SHADOW = new Color(0xE7CFAB), CRIMSON = new Color(0xB02736),
            INK = new Color(0x142334), MUTED = new Color(0x263F59), GOLD = new Color(0xE1B169),
            FIELD = new Color(0xFFF8EA);

    public static Font heading = new Font(Font.MONOSPACED, Font.BOLD, 14);
    public static Font body = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    static {
        heading = loadFont("fonts/NotoSansMono-Bold.ttf", heading);
        body = loadFont("fonts/Cabin-SemiBold.otf", body);
    }

    // ---------------------------------------------------------------- loading
    private static InputStream open(String rel) {
        InputStream in = Theme.class.getResourceAsStream("/assets/" + rel);
        if (in != null) return in;
        for (String base : new String[]{"assets/", "OS_Simulator_Individual_Assets/", "src/main/resources/assets/"}) {
            File f = new File(base + rel);
            if (f.isFile()) { try { return new FileInputStream(f); } catch (IOException ignored) { } }
        }
        return null;
    }

    private static Font loadFont(String rel, Font fallback) {
        try (InputStream in = open(rel)) {
            if (in == null) return fallback;
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f.deriveFont(fallback.getStyle(), 13f);
        } catch (Exception e) { return fallback; }
    }

    public static Font body(float size) { return body.deriveFont(Font.PLAIN, size); }
    public static Font heading(float size) { return heading.deriveFont(Font.BOLD, size); }

    public static BufferedImage image(String rel) {
        if (CACHE.containsKey(rel)) return CACHE.get(rel);
        BufferedImage img = null;
        try (InputStream in = open(rel)) { if (in != null) img = ImageIO.read(in); } catch (IOException ignored) { }
        CACHE.put(rel, img);
        return img;
    }

    /** An icon from the pack, recolored to {@code tint}. The hourglass PNG in the pack is a blank box, so it is drawn in code. */
    public static Icon icon(String name, int size, Color tint) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (name.equals("hourglass")) {
            g.setColor(tint);
            g.setStroke(new BasicStroke(size / 12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float l = size * 0.22f, r = size * 0.78f, t = size * 0.12f, b = size * 0.88f, m = size / 2f;
            g.drawLine((int) l, (int) t, (int) r, (int) t);
            g.drawLine((int) l, (int) b, (int) r, (int) b);
            Path2D p = new Path2D.Float();
            p.moveTo(l + size * 0.04, t); p.lineTo(m, m); p.lineTo(r - size * 0.04, t);
            p.moveTo(l + size * 0.04, b); p.lineTo(m, m); p.lineTo(r - size * 0.04, b);
            g.draw(p);
            Path2D sand = new Path2D.Float();
            sand.moveTo(m - size * 0.16, b - size * 0.02); sand.lineTo(m, m + size * 0.12); sand.lineTo(m + size * 0.16, b - size * 0.02); sand.closePath();
            g.fill(sand);
        } else {
            BufferedImage src = image("icons/" + name + ".png");
            if (src != null) {
                g.drawImage(src, 0, 0, size, size, null);
                g.setComposite(AlphaComposite.SrcIn);
                g.setColor(tint);
                g.fillRect(0, 0, size, size);
            }
        }
        g.dispose();
        return new ImageIcon(out);
    }

    /** Small triangle used on the Run button. */
    public static Icon playIcon(int size, Color c) {
        return new Icon() {
            public void paintIcon(Component comp, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c);
                g.fillPolygon(new int[]{x + 2, x + size - 1, x + 2}, new int[]{y, y + size / 2, y + size}, 3);
                g.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ---------------------------------------------------------------- painting helpers
    /** Draws a 9-slice image so corners keep their shape while the middle stretches. */
    public static void nine(Graphics2D g, BufferedImage im, int x, int y, int w, int h, int slice) {
        if (im == null || w <= 0 || h <= 0) return;
        int iw = im.getWidth(), ih = im.getHeight();
        int s = Math.max(1, Math.min(slice, Math.min(Math.min(iw, ih) / 2, Math.min(w, h) / 2)));
        int[] sx = {0, s, iw - s, iw}, sy = {0, s, ih - s, ih};
        int[] dx = {x, x + s, x + w - s, x + w}, dy = {y, y + s, y + h - s, y + h};
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                g.drawImage(im, dx[c], dy[r], dx[c + 1], dy[r + 1], sx[c], sy[r], sx[c + 1], sy[r + 1], null);
    }

    /** Button-style skin: only the left/right caps are kept, scaled to the button height, so text is never crossed by an edge. */
    public static void cap3(Graphics2D g, BufferedImage im, int w, int h, int cap) {
        if (im == null || w <= 0 || h <= 0) return;
        int iw = im.getWidth(), ih = im.getHeight();
        double k = h / (double) ih;
        int dc = (int) Math.round(cap * k);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (w < dc * 2 + 2) { g.drawImage(im, 0, 0, w, h, null); return; }
        g.drawImage(im, 0, 0, dc, h, 0, 0, cap, ih, null);
        g.drawImage(im, dc, 0, w - dc, h, cap, 0, iw - cap, ih, null);
        g.drawImage(im, w - dc, 0, w, h, iw - cap, 0, iw, ih, null);
    }

    // ---------------------------------------------------------------- components
    /** Window background (navy casino wallpaper). */
    public static void paintBackground(Graphics g, JComponent c) {
        BufferedImage bg = image("backgrounds/casino_navy_background.png");
        if (bg != null) g.drawImage(bg, 0, 0, c.getWidth(), c.getHeight(), null);
        else { g.setColor(NAVY_BG); g.fillRect(0, 0, c.getWidth(), c.getHeight()); }
    }

    /** A cream (or navy) card panel with an optional underlined title. Add your content to {@link #body}. */
    public static class SkinPanel extends JPanel {
        public final JPanel body = new JPanel(new BorderLayout());
        private final boolean navy;
        private final JLabel titleLabel;

        public SkinPanel(String title, String iconName, boolean navy) {
            super(new BorderLayout(0, 6));
            this.navy = navy;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 20, 20));
            body.setOpaque(false);
            if (title != null) {
                JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                head.setOpaque(false);
                if (iconName != null) head.add(new JLabel(icon(iconName, 18, navy ? CREAM : INK)));
                titleLabel = new JLabel(title) {
                    @Override protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(CRIMSON);
                        g.fillRect(0, getHeight() - 3, getWidth(), 2);
                    }
                };
                titleLabel.setFont(heading(16f));
                titleLabel.setForeground(navy ? CREAM : INK);
                titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
                head.add(titleLabel);
                add(head, BorderLayout.NORTH);
            } else titleLabel = null;
            add(body, BorderLayout.CENTER);
        }

        public void setTitle(String t) { if (titleLabel != null) { titleLabel.setText(t); revalidate(); repaint(); } }

        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            BufferedImage skin = image(navy ? "panels/navy_panel_9slice.png" : "panels/cream_panel_9slice.png");
            if (skin != null) nine(g, skin, 0, 0, getWidth(), getHeight(), 30);
            else {
                g.setColor(navy ? NAVY_PANEL : CREAM);
                g.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 16, 16);
            }
            g.dispose();
        }
    }

    /** Image button: kind is "red", "navy" or "cream". Hover and pressed images swap automatically. */
    public static class SkinButton extends JButton {
        private String kind;

        public SkinButton(String text, String kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setForeground(kind.equals("cream") ? INK : CREAM);
            setFont(body(15f).deriveFont(Font.BOLD));
            setMargin(new Insets(4, 14, 8, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setIconTextGap(8);
        }

        /** Switch the skin: "red", "navy" or "cream". */
        public void setKind(String k) {
            kind = k;
            setForeground(k.equals("cream") ? INK : CREAM);
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            String state = getModel().isPressed() ? "pressed" : getModel().isRollover() ? "hover" : "normal";
            BufferedImage im = image("buttons/" + kind + "_button_" + state + ".png");
            if (im != null) cap3(g, im, getWidth(), getHeight(), 34);
            else {
                g.setColor(kind.equals("red") ? CRIMSON : kind.equals("cream") ? CREAM : MUTED);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Red rounded label, used for the Mode (preemptive / non-preemptive) display. */
    public static class Pill extends JLabel {
        public Pill() {
            setForeground(CREAM);
            setFont(body(15f).deriveFont(Font.BOLD));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(5, 16, 5, 16));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CRIMSON);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.setColor(INK);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.dispose();
            super.paintComponent(g0);
        }
    }

    public static JLabel label(String text, Color color, float size) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(body(size));
        return l;
    }

    /** Rounded cream text box with a dark outline (same look as the combo boxes). */
    public static class RoundField extends JTextField {
        public RoundField(String text, int columns) {
            super(text, columns);
            setOpaque(false);
            setForeground(INK);
            setCaretColor(INK);
            setSelectionColor(GOLD);
            setSelectedTextColor(INK);
            setFont(body(15f));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(FIELD);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Rounded check box: crimson with a cream tick when selected. */
    public static void styleCheck(JCheckBox c) {
        c.setOpaque(false);
        c.setForeground(INK);
        c.setFont(body(15f));
        c.setFocusPainted(false);
        c.setIconTextGap(10);
        c.setIcon(checkIcon(false));
        c.setSelectedIcon(checkIcon(true));
    }

    private static Icon checkIcon(boolean on) {
        return new Icon() {
            public void paintIcon(Component comp, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(on ? CRIMSON : FIELD);
                g.fillRoundRect(x, y, 22, 22, 8, 8);
                g.setColor(INK);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(x + 1, y + 1, 20, 20, 8, 8);
                if (on) {
                    g.setColor(CREAM);
                    g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawPolyline(new int[]{x + 5, x + 10, x + 17}, new int[]{y + 11, y + 16, y + 6}, 3);
                }
                g.dispose();
            }
            public int getIconWidth() { return 24; }
            public int getIconHeight() { return 22; }
        };
    }

    /** Red rounded drop-down with a cream arrow. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void styleCombo(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override public void paint(Graphics g0) {
                        Graphics2D g = (Graphics2D) g0.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(CREAM);
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        g.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
                        g.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.setPreferredSize(new Dimension(24, 24));
                return b;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }
            @Override public void paint(Graphics g0, JComponent c) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(CRIMSON);
                g.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
                g.setColor(INK);
                g.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
                g.dispose();
                super.paint(g0, c);
            }
        });
        combo.setOpaque(false);
        combo.setForeground(CREAM);
        combo.setFont(body(15f).deriveFont(Font.BOLD));
        combo.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
        ((JComboBox) combo).setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object v, int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, v, i, sel, foc);
                setFont(body(15f).deriveFont(Font.BOLD));
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                if (i < 0) { setOpaque(false); setForeground(CREAM); }
                else { setOpaque(true); setBackground(sel ? GOLD : CRIMSON); setForeground(sel ? INK : CREAM); }
                return this;
            }
        });
    }

    /** Slim scroll bars with a crimson thumb. */
    public static void styleScroll(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new SlimBar());
        sp.getHorizontalScrollBar().setUI(new SlimBar());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(12, 12));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(12, 12));
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setOpaque(false);
    }

    public static JScrollPane scroll(Component c, Color viewportBg) {
        JScrollPane sp = new JScrollPane(c);
        if (c instanceof JTable) sp.setColumnHeaderView(((JTable) c).getTableHeader());
        sp.setBorder(new LineBorder(INK, 1));
        sp.getViewport().setBackground(viewportBg);
        styleScroll(sp);
        return sp;
    }

    private static class SlimBar extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { }
        private JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
            return b;
        }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected void paintTrack(Graphics g0, JComponent c, Rectangle r) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 60));
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g.dispose();
        }
        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CRIMSON);
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g.dispose();
        }
    }

    /** Table look. dark=false: cream rows (inputs). dark=true: navy rows (results). */
    public static void styleTable(JTable t, boolean dark) {
        final Color rowA = dark ? new Color(0x16293F) : new Color(0xFBEBD2), rowB = dark ? new Color(0x1C344F) : CREAM;
        final Color fg = dark ? CREAM : INK, grid = dark ? new Color(0x2B4562) : new Color(0xD8BE96);
        t.setFont(body(15f));
        t.setForeground(fg);
        t.setBackground(rowA);
        t.setRowHeight(30);
        t.setGridColor(grid);
        t.setShowGrid(true);
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(GOLD);
        t.setSelectionForeground(INK);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!sel) { setBackground(r % 2 == 0 ? rowA : rowB); setForeground(fg); }
                return this;
            }
        });
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 34));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(dark ? NAVY_BG : CREAM_SHADOW);
                setForeground(dark ? CREAM : INK);
                setFont(heading(11f));
                setBorder(new MatteBorder(0, 0, 1, 1, grid));
                return this;
            }
        });
    }

    /** Vertical split pane whose divider shows three dots. */
    public static JSplitPane split(Component top, Component bottom) {
        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        sp.setUI(new BasicSplitPaneUI() {
            @Override public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider d = new BasicSplitPaneDivider(this) {
                    @Override public void paint(Graphics g0) {
                        Graphics2D g = (Graphics2D) g0.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(new Color(CREAM.getRed(), CREAM.getGreen(), CREAM.getBlue(), 160));
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        for (int i = -1; i <= 1; i++) g.fillOval(cx + i * 10 - 2, cy - 2, 4, 4);
                        g.dispose();
                    }
                };
                d.setBorder(null);
                return d;
            }
        });
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.setDividerSize(14);
        sp.setContinuousLayout(true);
        return sp;
    }
}