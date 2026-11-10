import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GanttChart extends JPanel {
    private static final Color[] PALETTE = {
            new Color(0x3F79C8), new Color(0xE58B2B), new Color(0x45A84C),
            new Color(0x8B5CC6), new Color(0xD6477A), new Color(0x2FA7A0)};

    private static final int LEFT = 40;

    private List<Segment> segments = new ArrayList<>();
    private TimeFmt fmt;
    private double shown = 0;
    private double pxPerUnit = 1;
    private javax.swing.Timer timer;

    public GanttChart() {
        setOpaque(true);
        setBackground(Theme.DEEP);
    }

    public void play(List<Segment> segs, TimeFmt fmt, int scale) {
        if (timer != null) timer.stop();
        segments = segs;
        this.fmt = fmt;
        shown = 0;
        final double total = segs.isEmpty() ? 0 : segs.get(segs.size() - 1).getEnd();
        double inputUnits = total / Math.pow(10, scale);
        double width = Math.max(600, Math.min(30000, inputUnits * 30));
        pxPerUnit = total == 0 ? 1 : width / total;
        setPreferredSize(new Dimension((int) (total * pxPerUnit) + LEFT + 40, 125));
        revalidate();
        repaint();
        final double step = Math.max(1e-9, total / 150.0);   // animation lasts about 3 seconds
        timer = new javax.swing.Timer(20, e -> {
            shown += step;
            if (shown >= total) { shown = total; timer.stop(); }
            repaint();
        });
        timer.start();
    }

    private Color colorOf(Segment s) {
        if (s.isIdle()) return new Color(0x3A5573);
        return PALETTE[s.getProcess().getId() % PALETTE.length];
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fmt == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.heading(15f));
        FontMetrics fm = g2.getFontMetrics();
        int y = 16, h = 52;
        int lastTickRight = Integer.MIN_VALUE;
        int axisY = y + h + 8;
        int axisEnd = LEFT + (int) (shown * pxPerUnit);

        g2.setColor(new Color(Theme.CREAM.getRed(), Theme.CREAM.getGreen(), Theme.CREAM.getBlue(), 110));
        g2.drawLine(LEFT, axisY, axisEnd, axisY);

        for (Segment s : segments) {
            if (s.getStart() >= shown) break;
            double e = Math.min(s.getEnd(), shown);
            int x = LEFT + (int) (s.getStart() * pxPerUnit), w = (int) ((e - s.getStart()) * pxPerUnit);
            if (w > 0) {
                g2.setColor(colorOf(s));
                g2.fillRoundRect(x, y, w, h, 10, 10);
                g2.setColor(Theme.INK);
                g2.drawRoundRect(x, y, w, h, 10, 10);
                String label = s.getLabel();
                if (e == s.getEnd() && (s.isIdle() || w >= fm.stringWidth(label) + 6)) {   // the word Idle is always shown
                    g2.setColor(Theme.CREAM);
                    g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h / 2 + 5);
                }
            }
            lastTickRight = tick(g2, fm, fmt.format(s.getStart()), x, axisY, lastTickRight);
            if (e == s.getEnd()) lastTickRight = tick(g2, fm, fmt.format(s.getEnd()), x + w, axisY, lastTickRight);
        }
    }

    private int tick(Graphics2D g2, FontMetrics fm, String text, int cx, int axisY, int lastRight) {
        int left = cx - fm.stringWidth(text) / 2;
        if (left <= lastRight + 4) return lastRight;
        g2.setColor(Theme.CREAM);
        g2.drawLine(cx, axisY - 3, cx, axisY + 3);
        g2.drawString(text, left, axisY + 20);
        return left + fm.stringWidth(text);
    }
}