package disk;

import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DiskChart extends JPanel {
    private static final Color DARK_ROW = new Color(0x16293F);

    private List<Integer> path = new ArrayList<>();
    private int maxCyl = 199;

    public DiskChart() {
        setOpaque(true);
        setBackground(DARK_ROW);
    }

    public void show(List<Integer> path, int maxCyl) {
        this.path = path; this.maxCyl = maxCyl;
        setPreferredSize(new Dimension(700, 60 + Math.max(1, path.size()) * 28));
        revalidate(); repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (path.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Theme.body(13f));

        int left = 30, right = Math.max(200, getWidth() - 40), top = 40, rowH = 28;

        // axis
        g2.setColor(Theme.CREAM);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(left, 22, right, 22);
        g2.drawString("0", left - 3, 14);
        String m = String.valueOf(maxCyl);
        g2.drawString(m, right - g2.getFontMetrics().stringWidth(m) + 3, 14);

        // faint row guides
        g2.setColor(new Color(0x2B4562));
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < path.size(); i++) {
            int y = top + i * rowH;
            g2.drawLine(left, y, right, y);
        }

        // path
        int px = 0, py = 0;
        for (int i = 0; i < path.size(); i++) {
            int x = left + (int) ((long) path.get(i) * (right - left) / Math.max(1, maxCyl));
            int y = top + i * rowH;
            if (i > 0) {
                g2.setColor(Theme.GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(px, py, x, y);
            }
            g2.setColor(i == 0 ? Theme.GOLD : Theme.CRIMSON);
            g2.fillOval(x - 5, y - 5, 10, 10);
            g2.setColor(Theme.CREAM);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x - 5, y - 5, 10, 10);
            g2.drawString(String.valueOf(path.get(i)), x + 8, y - 5);
            px = x; py = y;
        }
        g2.dispose();
    }
}