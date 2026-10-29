import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Draws logical memory (pages) on the left, physical memory (frames) on the right, with page-table arrows. */
public class MemoryDiagram extends JPanel {
    private static final int H = 26, TOP = 36, LX = 20, W = 200, PX = 360;
    private PageTable table;
    private Set<Integer> highlight = new HashSet<>();

    public void show(PageTable t, Set<Integer> highlightedPages) {
        table = t;
        highlight = highlightedPages;
        int rows = Math.max(t.getPageCount(), t.getFrameCount());
        setPreferredSize(new Dimension(PX + W + 30, TOP + rows * H + 20));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (table == null) { g2.drawString("Click Generate Table, Randomize or Load Sample.", 20, 30); return; }
        Rectangle clip = g2.getClipBounds();
        int size = table.getPageSize();
        Color pageColor = new Color(190, 215, 245), frameColor = new Color(200, 235, 200),
                hot = new Color(255, 190, 90), free = new Color(235, 235, 235);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        g2.drawString("Logical memory", LX, 22);
        g2.drawString("Physical memory", PX, 22);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN));

        int pages = table.getPageCount(), frames = table.getFrameCount();
        // arrows first, so the blocks are drawn on top
        for (int p = 0; p < pages; p++) {
            int f = table.getFrameOf(p);
            if (f == -1) continue;
            int y1 = TOP + p * H + H / 2, y2 = TOP + f * H + H / 2;
            if (Math.max(y1, y2) < clip.y || Math.min(y1, y2) > clip.y + clip.height) continue;
            boolean hl = highlight.contains(p);
            g2.setColor(hl ? new Color(230, 120, 0) : Color.GRAY);
            g2.setStroke(new BasicStroke(hl ? 2.5f : 1f));
            g2.drawLine(LX + W, y1, PX, y2);
        }
        g2.setStroke(new BasicStroke(1f));
        int from = Math.max(0, (clip.y - TOP) / H), to = Math.min(Math.max(pages, frames) - 1, (clip.y + clip.height - TOP) / H);
        for (int i = from; i <= to; i++) {
            int y = TOP + i * H;
            if (i < pages) {
                g2.setColor(highlight.contains(i) ? hot : pageColor);
                g2.fillRect(LX, y, W, H);
                g2.setColor(Color.BLACK);
                g2.drawRect(LX, y, W, H);
                g2.drawString("Page " + i + "  [" + (long) i * size + " - " + ((long) (i + 1) * size - 1) + "]", LX + 6, y + 17);
            }
            if (i < frames) {
                int o = table.getPageInFrame(i);
                g2.setColor(o == -1 ? free : highlight.contains(o) ? hot : frameColor);
                g2.fillRect(PX, y, W, H);
                g2.setColor(Color.BLACK);
                g2.drawRect(PX, y, W, H);
                g2.drawString("Frame " + i + "  [" + (long) i * size + " - " + ((long) (i + 1) * size - 1) + "]  " + (o == -1 ? "free" : "Page " + o), PX + 6, y + 17);
            }
        }
    }
}
