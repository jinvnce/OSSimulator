package replacement;

import paging.PagingResult;
import ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Page replacement table. One column per reference, one row per frame, and a Hit / Fault row at the bottom.
 * Gold cell = page just loaded, red F = page fault, cream H = hit.
 * The cells stretch to fill the whole card; if there are too many columns/rows to fit, it scrolls.
 */
public class ReplacementGrid extends JPanel implements Scrollable {
    private static final Color GOLD = new Color(0xFFC247), FAULT = new Color(0xE0534F),
            GRID = new Color(0x2B4562), HIT_FILL = new Color(255, 255, 255, 18);
    // minimum sizes (below these the panel scrolls) and maximum sizes (so a short table doesn't look silly)
    private static final int MIN_CELL_W = 44, MAX_CELL_W = 110, MIN_ROW_H = 34, MAX_ROW_H = 84;
    private static final int LEFT = 16, LABEL_W = 70, PAD = 14, GAP = 10, RIGHT = 20, LEGEND_H = 44;

    private PagingResult result;
    private int numFrames;
    private int cw = MIN_CELL_W, rh = MIN_ROW_H;      // current cell size, recalculated on every paint

    public ReplacementGrid() { setBackground(Theme.DEEP); }

    public void show(PagingResult result, int numFrames) {
        this.result = result;
        this.numFrames = numFrames;
        int cols = result == null ? 0 : result.getSteps();
        int rows = numFrames + 2;                                   // reference row + frames + result row
        setPreferredSize(new Dimension(LEFT + LABEL_W + cols * MIN_CELL_W + RIGHT,
                PAD + rows * MIN_ROW_H + 2 * GAP + LEGEND_H + 12));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (result == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int n = Math.max(1, result.getSteps());
        int rows = numFrames + 2;
        int x0 = LEFT + LABEL_W;

        // stretch the cells to the space we were given
        int availW = getWidth() - x0 - RIGHT;
        int availH = getHeight() - PAD - 2 * GAP - LEGEND_H - 12;
        cw = Math.max(MIN_CELL_W, Math.min(MAX_CELL_W, availW / n));
        rh = Math.max(MIN_ROW_H, Math.min(MAX_ROW_H, availH / rows));

        float size = Math.max(15f, Math.min(30f, Math.min(rh * 0.48f, cw * 0.42f)));
        Font plain = Theme.body(size), bold = plain.deriveFont(Font.BOLD);
        Font label = Theme.heading(Math.max(13f, size * 0.75f));
        int y = PAD;

        // reference string row
        rowLabel(g2, "Ref", label, y);
        for (int i = 0; i < n; i++)
            cell(g2, x0 + i * cw, y, null, String.valueOf(result.getPage(i)), Theme.CREAM, bold);
        y += rh + GAP;

        // frame rows
        for (int k = 0; k < numFrames; k++) {
            int cy = y + k * rh;
            rowLabel(g2, "F" + (k + 1), label, cy);
            for (int i = 0; i < n; i++) {
                int v = result.getFrames(i)[k];
                if (v == -1) { cell(g2, x0 + i * cw, cy, null, null, null, plain); continue; }
                boolean loaded = !result.isHit(i) && changed(i, k);
                if (loaded) cell(g2, x0 + i * cw, cy, GOLD, String.valueOf(v), Theme.INK, bold);
                else cell(g2, x0 + i * cw, cy, null, String.valueOf(v), Theme.CREAM, plain);
            }
        }
        y += numFrames * rh + GAP;

        // hit / fault row
        rowLabel(g2, "Result", label, y);
        for (int i = 0; i < n; i++) {
            boolean hit = result.isHit(i);
            if (hit) cell(g2, x0 + i * cw, y, HIT_FILL, "H", Theme.CREAM, bold);
            else cell(g2, x0 + i * cw, y, FAULT, "F", Theme.CREAM, bold);
        }
        y += rh + 22;

        legend(g2, y);
    }

    /** True when frame k received a new page at step i (the page that was just loaded). */
    private boolean changed(int i, int k) {
        if (i == 0) return true;
        return result.getFrames(i)[k] != result.getFrames(i - 1)[k];
    }

    private void rowLabel(Graphics2D g2, String s, Font f, int y) {
        g2.setFont(f);
        g2.setColor(Theme.CREAM);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, LEFT, y + (rh + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void cell(Graphics2D g2, int x, int y, Color fill, String s, Color fg, Font f) {
        if (fill != null) { g2.setColor(fill); g2.fillRect(x + 1, y + 1, cw - 1, rh - 1); }
        g2.setColor(GRID);
        g2.drawRect(x, y, cw, rh);
        if (s == null) return;
        g2.setFont(f);
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, x + (cw - fm.stringWidth(s)) / 2, y + (rh + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void legend(Graphics2D g2, int y) {
        int x = LEFT;
        x = legendItem(g2, x, y, GOLD, "Page loaded");
        x = legendItem(g2, x, y, FAULT, "F = page fault");
        legendItem(g2, x, y, HIT_FILL, "H = hit");
    }

    private int legendItem(Graphics2D g2, int x, int y, Color c, String text) {
        g2.setColor(c);
        g2.fillRect(x, y, 14, 14);
        g2.setColor(Theme.CREAM);
        g2.drawRect(x, y, 14, 14);
        g2.setFont(Theme.body(13f));
        g2.drawString(text, x + 22, y + 12);
        return x + 22 + g2.getFontMetrics().stringWidth(text) + 24;
    }

    // ------------------------------------------------------------ Scrollable: fill the viewport when there is room
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return o == SwingConstants.HORIZONTAL ? r.width : r.height; }
    @Override public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() >= getPreferredSize().width;
    }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height;
    }
}