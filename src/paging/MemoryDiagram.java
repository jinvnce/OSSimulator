package paging;

import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.*;


public class MemoryDiagram extends JPanel implements Scrollable {
    private static final int TOP = 40, LX = 20, MIN_H = 16, MAX_H = 26, MIN_COL = 150, MAX_COL = 320, MIN_GAP = 120;
    private static final Color LOADED = Theme.CREAM, FREE = new Color(0x2B4562),
            GOLD = new Color(0xFFC247), FAULT = new Color(0xE0534F),
            FAINT = new Color(255, 255, 255, 80), INK = Theme.INK;

    private PageTable table;
    private Set<Integer> hotPages = new HashSet<>();
    private Set<Integer> hotLogical = new HashSet<>();
    private Set<Long> hotPhysical = new HashSet<>();

    private int H = 22, W = 230, PX = 380;

    public MemoryDiagram() {
        setBackground(Theme.DEEP);
    }

    public void show(PageTable t, Set<Integer> highlightedPages) {
        show(t, highlightedPages, new HashSet<>(), new HashSet<>());
    }

    public void show(PageTable t, Set<Integer> highlightedPages,
                     Set<Integer> logicalAddrs, Set<Long> physicalAddrs) {
        table = t;
        hotPages = highlightedPages;
        hotLogical = logicalAddrs;
        hotPhysical = physicalAddrs;
        revalidate();
        repaint();
    }

    private long totalRows() {
        return table == null ? 0 : Math.max(table.getLogicalSize(), table.getPhysicalSize());
    }

    private int minHeight() {
        return (int) Math.min(TOP + totalRows() * MIN_H + 20, Integer.MAX_VALUE - 1);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(2 * MIN_COL + MIN_GAP + 2 * LX, minHeight());
    }

    private void layoutNow() {
        W = Math.max(MIN_COL, Math.min(MAX_COL, (getWidth() - 2 * LX - MIN_GAP) / 2));
        PX = Math.max(LX + W + 40, getWidth() - LX - W);
        long total = Math.max(1, totalRows());
        H = (int) Math.max(MIN_H, Math.min(MAX_H, (getHeight() - TOP - 20) / total));
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
        return H;
    }

    public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return Math.max(H, r.height - H);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= minHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (table == null) return;
        layoutNow();

        Rectangle clip = g2.getClipBounds();
        int size = table.getPageSize(), pages = table.getPageCount();
        long logical = table.getLogicalSize(), physical = table.getPhysicalSize();
        long total = Math.max(logical, physical);

        g2.setColor(Theme.CREAM);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString("Logical memory", LX, 28);
        g2.drawString("Physical memory", PX, 28);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, H >= 20 ? 12 : 10));
        int textY = H / 2 + 4;

        for (int pass = 0; pass < 2; pass++) {
            boolean goldPass = pass == 1;
            g2.setColor(goldPass ? GOLD : FAINT);
            g2.setStroke(new BasicStroke(goldPass ? 2.5f : 1f));
            for (int p = 0; p < pages; p++) {
                int f = table.getFrameOf(p);
                if (f == -1 || hotPages.contains(p) != goldPass) continue;
                long y1 = TOP + (long) p * size * H + (long) size * H / 2;
                long y2 = TOP + (long) f * size * H + (long) size * H / 2;
                if (Math.max(y1, y2) < clip.y || Math.min(y1, y2) > clip.y + clip.height) continue;
                g2.drawLine(LX + W, (int) y1, PX, (int) y2);
            }
        }
        g2.setStroke(new BasicStroke(1f));

        long from = Math.max(0, (clip.y - TOP) / H);
        long to = Math.min(total - 1, (clip.y + clip.height - TOP) / H);
        for (long i = from; i <= to; i++) {
            int y = (int) (TOP + i * H);

            if (i < logical) {
                int page = (int) (i / size), off = (int) (i % size);
                boolean hot = hotLogical.contains((int) i);
                boolean fault = hot && table.getFrameOf(page) == -1;
                g2.setColor(fault ? FAULT : hot ? GOLD : LOADED);
                g2.fillRect(LX, y, W, H);
                g2.setColor(INK);
                g2.drawRect(LX, y, W, H);
                String label = i + "   P" + page + " +" + off;
                if (fault) label += "   FAULT";
                g2.drawString(label, LX + 6, y + textY);
                if (off == 0) boundary(g2, LX, y);
            }

            if (i < physical) {
                int frame = (int) (i / size), off = (int) (i % size);
                int owner = table.getPageInFrame(frame);
                boolean hot = hotPhysical.contains(i);
                g2.setColor(hot ? GOLD : owner == -1 ? FREE : LOADED);
                g2.fillRect(PX, y, W, H);
                g2.setColor(owner == -1 ? Theme.CREAM_SHADOW : INK);
                g2.drawRect(PX, y, W, H);
                g2.setColor(owner == -1 ? new Color(255, 255, 255, 110) : INK);
                String label = i + "   F" + frame + " +" + off + (owner == -1 ? "   free" : "   (P" + owner + ")");
                g2.drawString(label, PX + 6, y + textY);
                if (off == 0) {
                    g2.setColor(owner == -1 ? Theme.CREAM_SHADOW : INK);
                    boundary(g2, PX, y);
                }
            }
        }
    }

    private void boundary(Graphics2D g2, int x, int y) {
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x, y, x + W, y);
        g2.setStroke(new BasicStroke(1f));
    }

    public static JComponent legend() {
        JPanel boxes = row();
        boxes.add(item(box(GOLD), "Translated address"));
        boxes.add(item(box(FAULT), "Page fault"));
        boxes.add(item(box(LOADED), "Loaded"));
        boxes.add(item(box(FREE), "Free frame"));

        JPanel lines = row();
        lines.add(item(line(GOLD, 3f), "Page you translated \u2192 its frame"));
        lines.add(item(line(new Color(140, 150, 165), 1.2f), "Other pages \u2192 their frames"));

        JPanel keys = row();
        keys.add(Theme.label("P = page    F = frame    +n = offset    (Pn) = page stored here", INK, 13f));

        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        p.add(boxes);
        p.add(lines);
        p.add(keys);
        return p;
    }

    private static JPanel row() {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 1));
        r.setOpaque(false);
        return r;
    }

    private static JLabel item(Icon icon, String text) {
        JLabel l = Theme.label(text, INK, 13f);
        l.setIcon(icon);
        l.setIconTextGap(6);
        return l;
    }

    private static Icon box(Color c) {
        return new Icon() {
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                g.setColor(c);
                g.fillRect(x, y, 14, 14);
                g.setColor(INK);
                g.drawRect(x, y, 14, 14);
            }

            public int getIconWidth() {
                return 15;
            }

            public int getIconHeight() {
                return 15;
            }
        };
    }

    private static Icon line(Color c, float w) {
        return new Icon() {
            public void paintIcon(Component comp, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c);
                g.setStroke(new BasicStroke(w));
                g.drawLine(x, y + 12, x + 24, y + 2);
                g.dispose();
            }

            public int getIconWidth() {
                return 25;
            }

            public int getIconHeight() {
                return 15;
            }
        };
    }
}