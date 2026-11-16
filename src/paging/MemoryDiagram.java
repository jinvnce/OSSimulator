package paging;

import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * One row per address. One line per page (page block -> its frame block).
 * Gold = what you translated, red = page fault, cream = loaded, dark = free frame.
 */
public class MemoryDiagram extends JPanel {
    private static final int H = 22, TOP = 40, LX = 20, W = 230, PX = 380;
    private static final Color LOADED = Theme.CREAM, FREE = new Color(0x2B4562),
            GOLD = new Color(0xFFC247), FAULT = new Color(0xE0534F),
            FAINT = new Color(255, 255, 255, 80), INK = Theme.INK;

    private PageTable table;
    private Set<Integer> hotPages = new HashSet<>();
    private Set<Integer> hotLogical = new HashSet<>();
    private Set<Long> hotPhysical = new HashSet<>();

    public MemoryDiagram() { setBackground(Theme.DEEP); }

    public void show(PageTable t, Set<Integer> highlightedPages) {
        show(t, highlightedPages, new HashSet<>(), new HashSet<>());
    }

    public void show(PageTable t, Set<Integer> highlightedPages,
                     Set<Integer> logicalAddrs, Set<Long> physicalAddrs) {
        table = t;
        hotPages = highlightedPages;
        hotLogical = logicalAddrs;
        hotPhysical = physicalAddrs;
        long rows = Math.max(t.getLogicalSize(), t.getPhysicalSize());
        int height = (int) Math.min(TOP + rows * H + 20, Integer.MAX_VALUE - 1);
        setPreferredSize(new Dimension(PX + W + 30, height));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (table == null) return;

        Rectangle clip = g2.getClipBounds();
        int size = table.getPageSize(), pages = table.getPageCount();
        long logical = table.getLogicalSize(), physical = table.getPhysicalSize();
        long total = Math.max(logical, physical);

        g2.setColor(Theme.CREAM);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString("Logical memory", LX, 28);
        g2.drawString("Physical memory", PX, 28);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // one line per page: faint first, gold (translated) on top
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

        // rows (text is kept short: address on every row, page/frame label only at the start of a block)
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
                g2.drawString(label, LX + 6, y + 15);
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
                g2.drawString(label, PX + 6, y + 15);
                if (off == 0) { g2.setColor(owner == -1 ? Theme.CREAM_SHADOW : INK); boundary(g2, PX, y); }
            }
        }
    }

    private void boundary(Graphics2D g2, int x, int y) {
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x, y, x + W, y);
        g2.setStroke(new BasicStroke(1f));
    }

    // ------------------------------------------------------------ legend
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
                g.setColor(c); g.fillRect(x, y, 14, 14);
                g.setColor(INK); g.drawRect(x, y, 14, 14);
            }
            public int getIconWidth() { return 15; }
            public int getIconHeight() { return 15; }
        };
    }

    private static Icon line(Color c, float w) {
        return new Icon() {
            public void paintIcon(Component comp, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c); g.setStroke(new BasicStroke(w));
                g.drawLine(x, y + 12, x + 24, y + 2);
                g.dispose();
            }
            public int getIconWidth() { return 25; }
            public int getIconHeight() { return 15; }
        };
    }
}