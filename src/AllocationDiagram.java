import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * One row per memory block (the bar length is the block size). Inside a block you see the process, the wasted space
 * inside the block (internal fragmentation, hatched red) and the free hole that is left (dashed outline).
 * Processes that could not be placed are listed underneath.
 */
public class AllocationDiagram extends JComponent {
    private MemoryAllocator.Result result;
    private int[] procs = new int[0];

    public AllocationDiagram() {
        setOpaque(true);
        setBackground(Theme.DEEP);
    }

    public void show(MemoryAllocator.Result r, int[] procs) {
        this.result = r;
        this.procs = procs.clone();
        revalidate();
        repaint();
    }

    public void clear() { result = null; revalidate(); repaint(); }

    @Override public Dimension getPreferredSize() {
        int rows = result == null ? 0 : result.blocks.length;
        int waiting = result == null ? 0 : result.waiting;
        return new Dimension(300, 54 + rows * 36 + (waiting > 0 ? 32 : 8));
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (result == null) {
            g.setFont(Theme.body(15f));
            g.setColor(Theme.CREAM_SHADOW);
            g.drawString("Enter blocks and processes, then click Run Simulation.", 16, 30);
            g.dispose();
            return;
        }
        legend(g);
        int maxBlock = 1;
        for (int b : result.blocks) maxBlock = Math.max(maxBlock, b);
        int labelW = 118, x0 = 14 + labelW, avail = Math.max(120, getWidth() - x0 - 18);
        int y = 42;
        for (int b = 0; b < result.blocks.length; b++, y += 36) {
            g.setFont(Theme.body(14f));
            g.setColor(Theme.CREAM);
            g.drawString("Block " + (b + 1) + " (" + result.blocks[b] + ")", 14, y + 18);
            int bw = Math.max(8, (int) ((long) result.blocks[b] * avail / maxBlock));
            Shape frame = new RoundRectangle2D.Double(x0, y, bw, 26, 8, 8);
            g.setColor(new Color(0x1C344F));
            g.fill(frame);
            int x = x0;
            List<MemoryAllocator.Cell> cells = result.cells.get(b);
            Shape oldClip = g.getClip();
            g.clip(frame);
            for (int k = 0; k < cells.size(); k++) {
                MemoryAllocator.Cell c = cells.get(k);
                int w = k == cells.size() - 1 ? x0 + bw - x : Math.max(2, (int) ((long) c.size * bw / result.blocks[b]));
                paintCell(g, c, x, y, w);
                x += w;
            }
            g.setClip(oldClip);
            g.setColor(new Color(0x5E7CA0));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(frame);
        }
        if (result.waiting > 0) {
            List<String> names = new ArrayList<>();
            for (MemoryAllocator.Placement p : result.placements)
                if (!p.allocated()) names.add("P" + (p.process + 1) + " (" + p.size + ")");
            g.setFont(Theme.body(14f));
            g.setColor(new Color(0xFF8A95));
            g.drawString("Waiting, no place found: " + String.join(", ", names), 14, y + 18);
        }
        g.dispose();
    }

    private void paintCell(Graphics2D g, MemoryAllocator.Cell c, int x, int y, int w) {
        String label;
        switch (c.kind) {
            case PROCESS:
                g.setColor(SegmentDiagram.COLORS[c.process % SegmentDiagram.COLORS.length]);
                g.fillRect(x, y, w, 26);
                label = "P" + (c.process + 1) + "  " + c.size;
                g.setColor(Color.WHITE);
                break;
            case INTERNAL:
                g.setColor(new Color(0x7A1F2B));
                g.fillRect(x, y, w, 26);
                g.setColor(new Color(0xFF6B78));
                g.setStroke(new BasicStroke(1.5f));
                Shape clip = g.getClip();
                g.clipRect(x, y, w, 26);
                for (int i = -30; i < w + 30; i += 8) g.drawLine(x + i, y + 26, x + i + 26, y);
                g.setClip(clip);
                label = "waste " + c.size;
                g.setColor(Color.WHITE);
                break;
            default:
                g.setColor(new Color(0xE1B169));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
                g.drawRect(x + 2, y + 2, Math.max(1, w - 5), 21);
                label = "free " + c.size;
                g.setColor(Theme.GOLD);
        }
        g.setFont(Theme.heading(11f));
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(label) + 6 <= w) {
            Rectangle r = new Rectangle(x, y, w, 26);
            g.drawString(label, r.x + (w - fm.stringWidth(label)) / 2, y + 17);
        }
        // thin line between cells
        g.setColor(new Color(0, 0, 0, 90));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(x, y, x, y + 26);
    }

    private void legend(Graphics2D g) {
        int x = 14;
        g.setFont(Theme.body(13f));
        x = legendItem(g, x, "Process", SegmentDiagram.COLORS[0], 0);
        x = legendItem(g, x, "Internal fragmentation", new Color(0xFF6B78), 1);
        legendItem(g, x, "Free", Theme.GOLD, 2);
    }

    private int legendItem(Graphics2D g, int x, String text, Color c, int style) {
        g.setColor(style == 0 ? c : style == 1 ? new Color(0x7A1F2B) : new Color(0x1C344F));
        g.fillRect(x, 12, 16, 14);
        g.setColor(c);
        g.setStroke(style == 2 ? new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f) : new BasicStroke(1.5f));
        g.drawRect(x, 12, 16, 14);
        g.setColor(Theme.CREAM);
        g.drawString(text, x + 22, 24);
        return x + 22 + g.getFontMetrics().stringWidth(text) + 18;
    }
}
