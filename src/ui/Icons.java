package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Small button icons drawn in code (no PNG needed), so they always render and take the color you give them.
 * Kinds: "plus", "minus", "trash", "dice", "sheet".
 *
 * Usage:  Icons.on(ModuleHeader.navyButton("Add", 110), "plus", Theme.CREAM);
 */
public final class Icons {
    private Icons() { }

    /** Puts the icon on the button (to the left of the text) and returns the same button. */
    public static JButton on(JButton b, String kind, Color color) {
        b.setIcon(glyph(kind, color));
        b.setIconTextGap(8);
        return b;
    }

    public static Icon glyph(String kind, Color c) {
        final int s = 16;
        return new Icon() {
            public void paintIcon(Component comp, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(c);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (kind) {
                    case "plus":
                        g.drawLine(8, 3, 8, 13);
                        g.drawLine(3, 8, 13, 8);
                        break;
                    case "minus":
                        g.drawLine(3, 8, 13, 8);
                        break;
                    case "trash":
                        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.drawLine(2, 4, 14, 4);                                                    // lid
                        g.drawLine(6, 4, 6, 2); g.drawLine(6, 2, 10, 2); g.drawLine(10, 2, 10, 4);  // handle
                        g.drawRoundRect(4, 4, 8, 10, 2, 2);                                         // bin
                        g.drawLine(7, 7, 7, 11); g.drawLine(9, 7, 9, 11);
                        break;
                    case "dice":
                        g.setStroke(new BasicStroke(1.8f));
                        g.drawRoundRect(1, 1, 13, 13, 4, 4);
                        g.fillOval(4, 4, 3, 3); g.fillOval(9, 4, 3, 3); g.fillOval(6, 6, 3, 3);
                        g.fillOval(4, 9, 3, 3); g.fillOval(9, 9, 3, 3);
                        break;
                    case "sheet":
                        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.drawRoundRect(3, 1, 10, 14, 3, 3);
                        g.drawLine(5, 6, 11, 6); g.drawLine(5, 9, 11, 9); g.drawLine(5, 12, 9, 12);
                        break;
                    default:
                        break;
                }
                g.dispose();
            }
            public int getIconWidth() { return s; }
            public int getIconHeight() { return s; }
        };
    }
}