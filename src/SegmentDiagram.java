import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Physical memory drawn as one wide bar (address 0 on the left). Every segment is a coloured block placed at its base
 * and as long as its limit. Valid translations show a gold arrow above the bar at the physical address they reach.
 * Segments that overlap each other get a red outline. Hover a block to see its numbers.
 */
public class SegmentDiagram extends JComponent implements Scrollable {
    static final Color[] COLORS = {new Color(0x2F6FBF), new Color(0xE58A2B), new Color(0x4DBB5F), new Color(0x8E4FC2),
            new Color(0xD94F70), new Color(0x20A39E), new Color(0xD9B23A), new Color(0x5C7CE0)};
    private static final int LEFT = 28, BAR_Y = 52, BAR_H = 36;

    private SegmentTable table;
    private long memSize;
    private List<SegmentTable.Result> results = new ArrayList<>();

    public SegmentDiagram() {
        setOpaque(true);
        setBackground(Theme.DEEP);
        setToolTipText("");                                   // switches tooltips on (the text comes from getToolTipText)
    }

    public void show(SegmentTable t, long memSize, List<SegmentTable.Result> results) {
        this.table = t;
        this.memSize = memSize;
        this.results = results == null ? new ArrayList<>() : results;
        repaint();
    }

    public void clear() { table = null; results = new ArrayList<>(); repaint(); }

    @Override public Dimension getPreferredSize() { return new Dimension(440, 140); }

    // fills the card when there is room, scrolls when the card is smaller than the drawing
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
    @Override public boolean getScrollableTracksViewportWidth() { return getParent() instanceof JViewport && getParent().getWidth() >= 380; }
    @Override public boolean getScrollableTracksViewportHeight() { return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height; }

    private int barLeft() { return LEFT; }
    private int barRight() { return Math.max(LEFT + 200, getWidth() - LEFT); }

    private int x(long address) {
        if (memSize <= 0) return barLeft();
        long w = barRight() - barLeft();
        return barLeft() + (int) Math.min(w, address * w / memSize);
    }

    @Override public String getToolTipText(MouseEvent e) {
        if (table == null || e.getY() < BAR_Y || e.getY() > BAR_Y + BAR_H) return null;
        for (int i = table.size() - 1; i >= 0; i--) {
            SegmentTable.Entry en = table.get(i);
            int x0 = x(en.base), x1 = Math.max(x0 + 3, x(en.base + en.limit));
            if (e.getX() >= x0 && e.getX() <= x1)
                return "Segment " + i + ": base " + en.base + ", limit " + en.limit + " (addresses " + en.base + " to " + en.end() + ")";
        }
        return null;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (table == null) {
            g.setFont(Theme.body(15f));
            g.setColor(Theme.CREAM_SHADOW);
            g.drawString("Load a sample or randomize segments to see physical memory.", 16, 30);
            g.dispose();
            return;
        }
        int l = barLeft(), r = barRight();
        g.setFont(Theme.heading(12f));
        g.setColor(Theme.CREAM);
        g.drawString("Physical memory", l, 14);

        g.setColor(new Color(0x1C344F));
        g.fill(new RoundRectangle2D.Double(l, BAR_Y, r - l, BAR_H, 10, 10));
        Shape oldClip = g.getClip();
        g.clip(new RoundRectangle2D.Double(l, BAR_Y, r - l, BAR_H, 10, 10));

        Set<Integer> overlapping = new HashSet<>();
        for (int[] p : table.overlaps()) { overlapping.add(p[0]); overlapping.add(p[1]); }

        // segments: coloured blocks with the segment number inside
        List<long[]> marks = new ArrayList<>();                   // {address, kind}  kind 0 = segment start
        for (int i = 0; i < table.size(); i++) {
            SegmentTable.Entry e = table.get(i);
            int x0 = x(e.base), w = Math.max(4, x(e.base + e.limit) - x0);
            Color c = COLORS[i % COLORS.length];
            boolean over = overlapping.contains(i);
            g.setColor(over ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 190) : c);
            g.fillRect(x0, BAR_Y, w, BAR_H);
            if (over) { g.setColor(new Color(0xFF6B78)); g.setStroke(new BasicStroke(2.5f)); g.drawRect(x0 + 1, BAR_Y + 1, w - 2, BAR_H - 2); }
            g.setFont(Theme.heading(12f));
            String label = "S" + i;
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(label) + 4 <= w) g.drawString(label, x0 + (w - fm.stringWidth(label)) / 2, BAR_Y + BAR_H / 2 + 5);
            marks.add(new long[]{e.base, 0});
        }
        g.setClip(oldClip);
        g.setColor(new Color(0x5E7CA0));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(l, BAR_Y, r - l, BAR_H, 10, 10));

        // addresses under the bar (two rows so neighbours do not collide): the start of every segment, plus 0 and the memory size
        marks.add(new long[]{0, 1});
        marks.add(new long[]{memSize, 1});
        marks.sort((a, b) -> Long.compare(a[0], b[0]));
        g.setFont(Theme.body(12f));
        FontMetrics fm = g.getFontMetrics();
        int[] rowRight = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (long[] m : marks) {
            String s = String.valueOf(m[0]);
            int cx = x(m[0]), tw = fm.stringWidth(s), tx = Math.max(2, Math.min(getWidth() - tw - 2, cx - tw / 2));
            int row = tx > rowRight[0] + 6 ? 0 : tx > rowRight[1] + 6 ? 1 : -1;
            if (row < 0) continue;                                // no room: skip this label (the block's tooltip still has it)
            rowRight[row] = tx + tw;
            g.setColor(new Color(0x5E7CA0));
            g.drawLine(cx, BAR_Y + BAR_H, cx, BAR_Y + BAR_H + 4 + row * 15);
            g.setColor(m[1] == 1 ? Theme.CREAM_SHADOW : Theme.CREAM);
            g.drawString(s, tx, BAR_Y + BAR_H + 16 + row * 15);
        }

        // gold arrows above the bar for valid translations
        List<SegmentTable.Result> valid = new ArrayList<>();
        for (SegmentTable.Result res : results) if (res.valid) valid.add(res);
        valid.sort((a, b) -> Long.compare(a.physical, b.physical));
        int[] arrowRight = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        g.setFont(Theme.heading(11f));
        FontMetrics af = g.getFontMetrics();
        for (SegmentTable.Result res : valid) {
            int cx = x(res.physical);
            g.setColor(Theme.GOLD);
            Path2D.Double tri = new Path2D.Double();
            tri.moveTo(cx, BAR_Y - 1); tri.lineTo(cx - 5, BAR_Y - 9); tri.lineTo(cx + 5, BAR_Y - 9); tri.closePath();
            g.fill(tri);
            String s = String.valueOf(res.physical);
            int tw = af.stringWidth(s), tx = Math.max(2, Math.min(getWidth() - tw - 2, cx - tw / 2));
            int row = tx > arrowRight[0] + 6 ? 0 : tx > arrowRight[1] + 6 ? 1 : -1;
            if (row < 0) continue;
            arrowRight[row] = tx + tw;
            g.drawString(s, tx, BAR_Y - 14 - row * 13);
        }
        g.dispose();
    }
}
