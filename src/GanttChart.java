import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Gantt chart panel that draws itself progressively (animation) using a Swing timer. */
public class GanttChart extends JPanel {
    private List<Segment> segments = new ArrayList<>();
    private TimeFmt fmt;
    private double shown = 0;            // how much of the chart is revealed, in internal time units
    private double pxPerUnit = 1;        // pixels per internal time unit
    private javax.swing.Timer timer;

    /**
     * @param scale decimal places of the internal numbers (used to keep the chart a sensible width:
     *              about 30 px per input-unit of time, but never narrower than 600 px or wider than 30,000 px)
     */
    public void play(List<Segment> segs, TimeFmt fmt, int scale) {
        if (timer != null) timer.stop();
        segments = segs;
        this.fmt = fmt;
        shown = 0;
        final double total = segs.isEmpty() ? 0 : segs.get(segs.size() - 1).getEnd();
        double inputUnits = total / Math.pow(10, scale);
        double width = Math.max(600, Math.min(30000, inputUnits * 30));
        pxPerUnit = total == 0 ? 1 : width / total;
        setPreferredSize(new Dimension((int) (total * pxPerUnit) + 50, 110));
        revalidate();
        final double step = Math.max(1e-9, total / 150.0);   // animation lasts about 3 seconds
        timer = new javax.swing.Timer(20, e -> {
            shown += step;
            if (shown >= total) { shown = total; timer.stop(); }
            repaint();
        });
        timer.start();
    }

    private Color colorOf(Segment s) {
        if (s.isIdle()) return Color.LIGHT_GRAY;
        return Color.getHSBColor((s.getProcess().getId() * 0.618f) % 1f, 0.35f, 0.95f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fmt == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics();
        int y = 20, h = 50;
        int lastTickRight = Integer.MIN_VALUE;   // stops time labels from overlapping each other
        for (Segment s : segments) {
            if (s.getStart() >= shown) break;
            double e = Math.min(s.getEnd(), shown);
            int x = 20 + (int) (s.getStart() * pxPerUnit), w = (int) ((e - s.getStart()) * pxPerUnit);
            if (w > 0) {
                g2.setColor(colorOf(s));
                g2.fillRect(x, y, w, h);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, w, h);
                String label = s.getLabel();
                if (e == s.getEnd() && (s.isIdle() || w >= fm.stringWidth(label) + 6))   // the word Idle is always shown
                    g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h / 2 + 5);
            }
            lastTickRight = tick(g2, fm, fmt.format(s.getStart()), x, y + h + 16, lastTickRight);
            if (e == s.getEnd()) lastTickRight = tick(g2, fm, fmt.format(s.getEnd()), x + w, y + h + 16, lastTickRight);
        }
    }

    private int tick(Graphics2D g2, FontMetrics fm, String text, int cx, int baseline, int lastRight) {
        int left = cx - fm.stringWidth(text) / 2;
        if (left <= lastRight + 4) return lastRight;
        g2.drawString(text, left, baseline);
        return left + fm.stringWidth(text);
    }
}
