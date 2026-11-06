import javax.swing.*;
import java.awt.*;

public class ReplacementPanel extends ModulePanel {
    private final JTextField refs = new JTextField(32), frames = new JTextField("3", 4), len = new JTextField("20", 4);
    private final JComboBox<PageReplacement> algo = new JComboBox<>(new PageReplacement[]{
            new FIFOReplacement(), new LRUReplacement(), new OptimalReplacement()});
    private final ReplacementGrid grid = new ReplacementGrid();
    private final JLabel summary = new JLabel("Run a simulation to see the results.");

    public ReplacementPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new GridLayout(2, 1));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        r1.add(new JLabel("Page reference string (spaces or commas):")); r1.add(refs);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton rnd = new JButton("Randomize"), run = new JButton("Run");
        r2.add(new JLabel("Length:")); r2.add(len); r2.add(rnd);
        r2.add(new JLabel("Frames:")); r2.add(frames);
        r2.add(new JLabel("Algorithm:")); r2.add(algo); r2.add(run);
        top.add(r1); top.add(r2);
        add(top, BorderLayout.NORTH);
        add(Util.scroll(grid, "Page replacement table"), BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
        rnd.addActionListener(e -> randomizeData());
        run.addActionListener(e -> runSimulation());
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
        summary.setText(String.format("%s: %d references, %d page faults, %d hits, hit ratio %.1f%%",
                alg.getName(), ref.length, res.getFaults(), res.getHits(), res.getHitRatio()));
    }
}
