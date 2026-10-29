import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.util.Random;

/** Shared helpers: input validation and small Swing utilities. Not instantiable. */
public final class Util {
    public static final Random RNG = new Random();

    private Util() { }

    /** Parses a whole number with clear messages for blank, non-numeric or too-small values. */
    public static int parseInt(String s, String name, int min) {
        s = s == null ? "" : s.trim();
        if (s.isEmpty()) throw new IllegalArgumentException(name + " is blank.");
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number (got \"" + s + "\").");
        }
        if (v < min) throw new IllegalArgumentException(name + " must be at least " + min + " (got " + v + ").");
        return v;
    }

    /** Resource names follow the slides: A, B, C, D ... (R26, R27 ... after Z). */
    public static String resourceName(int j) { return j < 26 ? String.valueOf((char) ('A' + j)) : "R" + j; }

    public static String str(Object o) { return o == null ? "" : o.toString(); }

    public static void error(Component c, String msg) {
        JOptionPane.showMessageDialog(c, msg, "Input error", JOptionPane.ERROR_MESSAGE);
    }

    public static void stopEditing(JTable t) {
        if (t.isEditing()) t.getCellEditor().stopCellEditing();
    }

    public static JScrollPane scroll(Component c, String title) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    public static DefaultTableModel readOnlyModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
