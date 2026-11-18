package replacement;

import paging.PagingResult;
import ui.ModuleHeader;
import ui.ModulePanel;
import ui.Theme;
import util.Util;

import javax.swing.*;
import java.awt.*;

public class ReplacementPanel extends ModulePanel {
    private final JTextField refs = new Theme.RoundField("", 40);
    private final JTextField frames = new Theme.RoundField("3", 4);
    private final JTextField len = new Theme.RoundField("20", 4);
    private final JComboBox<PageReplacement> algo = new JComboBox<>(new PageReplacement[]{
            new FIFOReplacement(), new LRUReplacement(), new OptimalReplacement()});
    private final ReplacementGrid grid = new ReplacementGrid();
    private final JLabel summary = Theme.label("Run a simulation to see the results.", Theme.INK, 16f);

    public ReplacementPanel() {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton rnd = ModuleHeader.navyButton("Randomize", 120);
        JButton run = ModuleHeader.runButton("Run");

        refs.setToolTipText("Page numbers, separated by spaces or commas");
        len.setToolTipText("How many references Randomize generates");
        Theme.styleCombo(algo);
        algo.setPreferredSize(new Dimension(140, 36));

        // HEADER: banner + tool bar
        JPanel tools = ModuleHeader.toolRow();
        tools.add(Theme.label("Length", Theme.CREAM, 16f));
        tools.add(len);
        tools.add(Theme.label("Frames", Theme.CREAM, 16f));
        tools.add(frames);
        tools.add(Theme.label("Algorithm", Theme.CREAM, 16f));
        tools.add(algo);
        add(ModuleHeader.north("Page Replacement", "FIFO, LRU and Optimal", "heart", tools, run), BorderLayout.NORTH);

        // REFERENCE STRING card
        Theme.SkinPanel refCard = new Theme.SkinPanel("Reference String", null, false);
        JPanel refRow = new JPanel(new BorderLayout(8, 0));
        refRow.setOpaque(false);
        refRow.add(refs, BorderLayout.CENTER);
        refRow.add(rnd, BorderLayout.EAST);
        refCard.body.add(refRow, BorderLayout.CENTER);

        // TABLE card
        Theme.SkinPanel tableCard = new Theme.SkinPanel("Page Replacement Table", null, false);
        tableCard.body.add(Theme.scroll(grid, Theme.DEEP), BorderLayout.CENTER);

        // SUMMARY card
        Theme.SkinPanel sumCard = new Theme.SkinPanel("Summary", null, false);
        summary.setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 0));
        sumCard.body.add(summary, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(refCard, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        center.add(sumCard, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
        refs.addActionListener(e -> runSimulation());
        frames.addActionListener(e -> runSimulation());

        refs.setText("7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1");      // the lecture example, so the table is never empty
        runSimulation();
    }

    @Override public String getTitle() { return "Page Replacement"; }

    @Override
    protected void randomizeData() {
        try {
            int n = Util.parseInt(len.getText(), "Length", 1);
            int range = Math.max(8, 2 * (int) Math.sqrt(n));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append(Util.RNG.nextInt(range)).append(' ');
            refs.setText(sb.toString().trim());
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); }
    }

    @Override
    protected void runSimulation() {
        int[] ref;
        int f;
        try {
            String text = refs.getText().trim();
            if (text.isEmpty()) throw new IllegalArgumentException("Reference string is blank. Type pages or use Randomize.");
            String[] parts = text.split("[\\s,]+");
            ref = new int[parts.length];
            for (int i = 0; i < parts.length; i++) ref[i] = Util.parseInt(parts[i], "Page number #" + (i + 1), 0);
            f = Util.parseInt(frames.getText(), "Number of frames", 1);
        } catch (IllegalArgumentException ex) { Util.error(this, ex.getMessage()); return; }

        PageReplacement alg = (PageReplacement) algo.getSelectedItem();
        PagingResult res = alg.simulate(ref, f);           // polymorphic call

        grid.show(res, f);
        summary.setText(String.format("%s:  %d references  |  %d page faults  |  %d hits  |  hit ratio %.1f%%",
                alg.getName(), ref.length, res.getFaults(), res.getHits(), res.getHitRatio()));
    }
}