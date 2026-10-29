import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Gantt chart panel that draws itself progressively (animation) using a Swing timer. */
public class GanttChart extends JPanel {
    private List<Segment> segments = new ArrayList<>();
    private double shown = 0;
    private int unit = 30;
    private javax.swing.Timer timer;

    public void play(List<Segment> segs) {
        if (timer != null) timer.stop();
        segments = segs;
        shown = 0;
        final int total = segs.isEmpty() ? 0 : segs.get(segs.size() - 1).getEnd();
        unit = Math.max(1, Math.min(30, 30000 / Math.max(1, total)));
        setPreferredSize(new Dimension(total * unit + 50, 110));
        revalidate();
        final double step = Math.max(0.15, total / 150.0); // animation lasts about 3 seconds at most
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
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics();
        int y = 20, h = 50;
        for (Segment s : segments) {
            if (s.getStart() >= shown) break;
            double e = Math.min(s.getEnd(), shown);
            int x = 20 + s.getStart() * unit, w = (int) ((e - s.getStart()) * unit);
            g2.setColor(colorOf(s));
            g2.fillRect(x, y, w, h);
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, w, h);
            String label = s.getLabel();
            if (e == s.getEnd() && (s.isIdle() || w >= fm.stringWidth(label) + 6))   // the word Idle is always shown
                g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h / 2 + 5);
            if (w >= 14) g2.drawString("" + s.getStart(), x - 3, y + h + 16);
            if (e == s.getEnd() && w >= 14) g2.drawString("" + s.getEnd(), x + w - 3, y + h + 16);
        }
    }
}
