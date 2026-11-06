import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DiskChart extends JPanel {
    private List<Integer> path = new ArrayList<>();
    private int maxCyl = 199;

    public void show(List<Integer> path, int maxCyl) {
        this.path = path; this.maxCyl = maxCyl;
        setPreferredSize(new Dimension(700, 60 + Math.max(1, path.size()) * 28));
        revalidate(); repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (path.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 30, right = Math.max(200, getWidth() - 40), top = 40, rowH = 28;
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(left, 22, right, 22);
        g2.drawString("0", left - 3, 14);
        String m = String.valueOf(maxCyl);
        g2.drawString(m, right - g2.getFontMetrics().stringWidth(m) + 3, 14);
        int px = 0, py = 0;
        for (int i = 0; i < path.size(); i++) {
            int x = left + (int) ((long) path.get(i) * (right - left) / Math.max(1, maxCyl));
            int y = top + i * rowH;
            if (i > 0) { g2.setColor(new Color(40, 90, 200)); g2.setStroke(new BasicStroke(2f)); g2.drawLine(px, py, x, y); }
            g2.setColor(i == 0 ? new Color(30, 150, 60) : new Color(200, 50, 50));
            g2.fillOval(x - 4, y - 4, 8, 8);
            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(path.get(i)), x + 7, y - 4);
            px = x; py = y;
        }
    }
}
