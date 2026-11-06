import javax.swing.*;
import java.awt.*;


public class ReplacementGrid extends JPanel {
    private static final Color RED = new Color(220, 0, 0);
    private PagingResult result;
    private int numFrames;

    public ReplacementGrid() { setBackground(Color.WHITE); }

    public void show(PagingResult result, int numFrames) {
        this.result = result; this.numFrames = numFrames;
        int cols = result == null ? 0 : result.getSteps();
        int rows = numFrames;
        setPreferredSize(new Dimension(80 + cols * CELL_W + 40, 60 + 34 + (rows + 1) * ROW_H + 100));
        revalidate(); repaint();
    }

    private static final int CELL_W = 44, ROW_H = 32, LEFT = 24, LABEL_W = 56;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (result == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font plain = new Font(Font.SERIF, Font.PLAIN, 17), bold = plain.deriveFont(Font.BOLD);
        int n = result.getSteps();
        int x0 = LEFT + LABEL_W, y = 26;

        g2.setFont(bold); g2.setColor(Color.BLACK);
        g2.drawString("Reference string", LEFT, y);
        y += 34;

        // reference string row
        g2.setFont(plain);
        for (int i = 0; i < n; i++) center(g2, String.valueOf(result.getPage(i)), x0 + i * CELL_W, y, CELL_W);
        y += 12;
        g2.setColor(Color.BLACK);
        g2.drawLine(LEFT, y, x0 + n * CELL_W, y);
        y += 6;

        // frame rows
        for (int k = 0; k < numFrames; k++) {
            int base = y + k * ROW_H + ROW_H - 10;
            g2.setFont(plain); g2.setColor(Color.BLACK);
            g2.drawString("F" + (k + 1), LEFT, base);
            for (int i = 0; i < n; i++) {
                int v = result.getFrames(i)[k];
                if (v == -1) continue;
                boolean loaded = !result.isHit(i) && changed(i, k);
                g2.setFont(loaded ? bold : plain);
                g2.setColor(loaded ? RED : Color.BLACK);
                center(g2, String.valueOf(v), x0 + i * CELL_W, base, CELL_W);
            }
        }
        y += numFrames * ROW_H + 4;

        // fault / hit row
        int base = y + ROW_H - 10;
        for (int i = 0; i < n; i++) {
            boolean hit = result.isHit(i);
            g2.setFont(plain);
            g2.setColor(hit ? Color.BLACK : RED);
            center(g2, hit ? "H" : "F", x0 + i * CELL_W, base, CELL_W);
        }
        y = base + 44;

        g2.setFont(bold); g2.setColor(Color.BLACK);
        g2.drawString("Total Page faults: " + result.getFaults(), LEFT, y);
        g2.drawString("Total Hits: " + result.getHits(), LEFT, y + 30);
    }

    /** True when frame k received a new page at step i (the page that was just loaded). */
    private boolean changed(int i, int k) {
        if (i == 0) return true;
        return result.getFrames(i)[k] != result.getFrames(i - 1)[k];
    }

    private static void center(Graphics2D g2, String s, int x, int baseline, int w) {
        g2.drawString(s, x + (w - g2.getFontMetrics().stringWidth(s)) / 2, baseline);
    }
}
