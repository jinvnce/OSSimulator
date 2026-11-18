package replacement;

import paging.PagingResult;
import ui.Theme;

import javax.swing.*;
import java.awt.*;


public class ReplacementGrid extends JPanel implements Scrollable {
    private static final Color GOLD = new Color(0xFFC247), FAULT = new Color(0xE0534F),
            GRID = new Color(0x2B4562), HIT_FILL = new Color(255, 255, 255, 18);

    private static final int MIN_CELL_W = 36, MIN_ROW_H = 28, MAX_ROW_H = 72;
    private static final int LEFT = 16, LABEL_W = 84, PAD = 14, GAP = 10, RIGHT = 20, LEGEND_H = 44;

    private static final int MIN_BOX_W = 120, MAX_BOX_W = 180, STAR_W = 30, HEAD_H = 28, CELL_H = 28,
            BOX_GAP_X = 16, BOX_GAP_Y = 18;

    private PagingResult result;
    private PageReplacement alg;
    private int numFrames;
    private int cw = MIN_CELL_W, rh = MIN_ROW_H;

    public ReplacementGrid() {
        setBackground(Theme.DEEP);
    }

    public void show(PagingResult result, int numFrames, PageReplacement alg) {
        this.result = result;
        this.numFrames = numFrames;
        this.alg = alg;
        if (!useMode()) {
            int cols = result == null ? 0 : result.getSteps();
            int rows = numFrames + 2;
            setPreferredSize(new Dimension(LEFT + LABEL_W + cols * MIN_CELL_W + RIGHT,
                    PAD + rows * MIN_ROW_H + 2 * GAP + LEGEND_H + 12));
        }
        revalidate();
        repaint();
    }

    private boolean useMode() {
        return alg != null && !alg.useLog.isEmpty() && alg.useLog.get(0) != null;
    }

    private int boxHeight() {
        return HEAD_H + numFrames * CELL_H + 6;
    }

    private int viewWidth() {
        return getParent() instanceof JViewport ? Math.max(getParent().getWidth(), 320) : 800;
    }

    private int perRow(int w, int total) {
        int usable = w - 2 * LEFT;
        return Math.max(1, Math.min(total, (usable + BOX_GAP_X) / (MIN_BOX_W + BOX_GAP_X)));
    }

    private int boxW(int w, int per) {
        int usable = w - 2 * LEFT;
        return Math.max(MIN_BOX_W, Math.min(MAX_BOX_W, (usable + BOX_GAP_X) / per - BOX_GAP_X));
    }

    @Override
    public Dimension getPreferredSize() {
        if (result == null || !useMode()) return super.getPreferredSize();
        int w = viewWidth();
        int total = result.getSteps() + 1;
        int rows = (total + perRow(w, total) - 1) / perRow(w, total);
        return new Dimension(w, PAD + rows * (boxHeight() + BOX_GAP_Y) + LEGEND_H + 20);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (result == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (useMode()) {
            paintBoxes(g2);
            return;
        }

        int n = Math.max(1, result.getSteps());
        int rows = numFrames + 2;
        int x0 = LEFT + LABEL_W;

        int availW = getWidth() - x0 - RIGHT;
        int availH = getHeight() - PAD - 2 * GAP - LEGEND_H - 12;
        cw = Math.max(MIN_CELL_W, availW / n);
        rh = Math.max(MIN_ROW_H, Math.min(MAX_ROW_H, availH / rows));

        float size = Math.max(13f, Math.min(26f, Math.min(rh * 0.45f, cw * 0.42f)));
        Font plain = Theme.body(size), bold = plain.deriveFont(Font.BOLD);
        Font label = Theme.heading(Math.max(13f, Math.min(20f, size * 0.75f)));
        int y = PAD;

        rowLabel(g2, "Ref", label, y);
        for (int i = 0; i < n; i++)
            cell(g2, x0 + i * cw, y, null, String.valueOf(result.getPage(i)), Theme.CREAM, bold);
        y += rh + GAP;

        for (int k = 0; k < numFrames; k++) {
            int cy = y + k * rh;
            rowLabel(g2, "F" + (k + 1), label, cy);
            for (int i = 0; i < n; i++) {
                int v = result.getFrames(i)[k];
                if (v == -1) {
                    cell(g2, x0 + i * cw, cy, null, null, null, plain);
                    continue;
                }
                boolean loaded = !result.isHit(i) && changed(i, k);
                if (loaded) cell(g2, x0 + i * cw, cy, GOLD, String.valueOf(v), Theme.INK, bold);
                else cell(g2, x0 + i * cw, cy, null, String.valueOf(v), Theme.CREAM, plain);
            }
        }
        y += numFrames * rh + GAP;

        rowLabel(g2, "Result", label, y);
        for (int i = 0; i < n; i++) {
            if (result.isHit(i)) cell(g2, x0 + i * cw, y, HIT_FILL, "H", Theme.CREAM, bold);
            else cell(g2, x0 + i * cw, y, FAULT, "F", Theme.CREAM, bold);
        }
        y += rh + 22;

        legend(g2, y);
    }

    private void paintBoxes(Graphics2D g2) {
        int n = result.getSteps();
        if (n == 0) return;
        int total = n + 1;
        int w = getWidth();
        int per = perRow(w, total);
        int bw = boxW(w, per);
        int boxH = boxHeight();
        Font plain = Theme.body(16f), bold = plain.deriveFont(Font.BOLD);

        int gridW = per * (bw + BOX_GAP_X) - BOX_GAP_X;
        int left = Math.max(LEFT, (w - gridW) / 2);

        for (int b = 0; b < total; b++) {
            int x = left + (b % per) * (bw + BOX_GAP_X);
            int y = PAD + (b / per) * (boxH + BOX_GAP_Y);
            int[] f, u;
            int p;
            String head;
            Color headFill = null;
            if (b < n) {
                f = alg.framesBefore.get(b);
                u = alg.useBefore.get(b);
                p = alg.ptrBefore.get(b);
                head = String.valueOf(result.getPage(b));
                headFill = result.isHit(b) ? HIT_FILL : FAULT;
            } else {
                f = result.getFrames(n - 1);
                u = alg.useLog.get(n - 1);
                p = alg.ptrLog.get(n - 1);
                head = "end";
            }
            drawBox(g2, x, y, bw, head, headFill, f, u, p, plain, bold);
        }

        int rows = (total + per - 1) / per;
        int ly = PAD + rows * (boxH + BOX_GAP_Y) + 4;
        int lx = left;
        lx = legendItem(g2, lx, ly, FAULT, "header = incoming page (fault)");
        legendItem(g2, lx, ly, HIT_FILL, "(hit)");
        g2.setFont(Theme.body(13f));
        g2.setColor(Theme.CREAM);
        g2.drawString("P = page, U = use flag, * = pointer (next frame to scan). Each box is the state before the "
                + "header page is processed; the last box is the final state.", left, ly + 34);
    }

    private void drawBox(Graphics2D g2, int x, int y, int bw, String head, Color headFill,
                         int[] f, int[] u, int p, Font plain, Font bold) {
        int colW = (bw - STAR_W) / 2;
        if (headFill != null) {
            g2.setColor(headFill);
            g2.fillRect(x + 1, y + 1, bw - 1, HEAD_H - 1);
        }
        g2.setColor(GRID);
        g2.drawRect(x, y, bw, HEAD_H);
        centered(g2, "P", x, y, colW, HEAD_H, Theme.CREAM, plain);
        centered(g2, "U", x + colW, y, colW, HEAD_H, Theme.CREAM, plain);
        centered(g2, head, x + 2 * colW, y, bw - 2 * colW, HEAD_H, Theme.CREAM, bold);

        for (int k = 0; k < numFrames; k++) {
            int cy = y + HEAD_H + k * CELL_H;
            g2.setColor(GRID);
            g2.drawRect(x, cy, colW, CELL_H);
            g2.drawRect(x + colW, cy, colW, CELL_H);
            if (f[k] >= 0) centered(g2, String.valueOf(f[k]), x, cy, colW, CELL_H, Theme.CREAM, plain);
            centered(g2, String.valueOf(u[k]), x + colW, cy, colW, CELL_H, Theme.CREAM, plain);
            if (k == p) centered(g2, "*", x + 2 * colW, cy, bw - 2 * colW, CELL_H, GOLD, bold);
        }
    }

    private void centered(Graphics2D g2, String s, int x, int y, int w, int h, Color fg, Font f) {
        g2.setFont(f);
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

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
        if (fill != null) {
            g2.setColor(fill);
            g2.fillRect(x + 1, y + 1, cw - 1, rh - 1);
        }
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

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return o == SwingConstants.HORIZONTAL ? r.width : r.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() >= getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height;
    }
}