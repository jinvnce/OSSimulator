package ui;

import javax.swing.*;
import java.awt.*;


public final class Icons {
    private Icons() {
    }

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
                        g.drawLine(2, 4, 14, 4);
                        g.drawLine(6, 4, 6, 2);
                        g.drawLine(6, 2, 10, 2);
                        g.drawLine(10, 2, 10, 4);
                        g.drawRoundRect(4, 4, 8, 10, 2, 2);
                        g.drawLine(7, 7, 7, 11);
                        g.drawLine(9, 7, 9, 11);
                        break;
                    case "dice":
                        g.setStroke(new BasicStroke(1.8f));
                        g.drawRoundRect(1, 1, 13, 13, 4, 4);
                        g.fillOval(4, 4, 3, 3);
                        g.fillOval(9, 4, 3, 3);
                        g.fillOval(6, 6, 3, 3);
                        g.fillOval(4, 9, 3, 3);
                        g.fillOval(9, 9, 3, 3);
                        break;
                    case "sheet":
                        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.drawRoundRect(3, 1, 10, 14, 3, 3);
                        g.drawLine(5, 6, 11, 6);
                        g.drawLine(5, 9, 11, 9);
                        g.drawLine(5, 12, 9, 12);
                        break;
                    case "sliders":
                        g.drawLine(2, 3, 14, 3);
                        g.drawLine(2, 8, 14, 8);
                        g.drawLine(2, 13, 14, 13);
                        g.fillOval(10, 1, 4, 4);
                        g.fillOval(3, 6, 4, 4);
                        g.fillOval(9, 11, 4, 4);
                        break;
                    case "table":
                        g.setStroke(new BasicStroke(1.8f));
                        g.drawRoundRect(1, 2, 13, 12, 2, 2);
                        g.drawLine(1, 6, 14, 6);
                        g.drawLine(7, 6, 7, 14);
                        break;
                    case "chip":
                        g.setStroke(new BasicStroke(1.8f));
                        g.drawRect(4, 4, 8, 8);
                        g.drawLine(6, 1, 6, 4);
                        g.drawLine(10, 1, 10, 4);
                        g.drawLine(6, 12, 6, 15);
                        g.drawLine(10, 12, 10, 15);
                        g.drawLine(1, 6, 4, 6);
                        g.drawLine(1, 10, 4, 10);
                        g.drawLine(12, 6, 15, 6);
                        g.drawLine(12, 10, 15, 10);
                        break;
                    case "list":
                        g.fillOval(2, 3, 2, 2);
                        g.fillOval(2, 7, 2, 2);
                        g.fillOval(2, 11, 2, 2);
                        g.drawLine(6, 4, 14, 4);
                        g.drawLine(6, 8, 14, 8);
                        g.drawLine(6, 12, 14, 12);
                        break;
                    case "compare":   // bars
                        g.drawLine(1, 14, 15, 14);
                        g.drawLine(4, 14, 4, 8);
                        g.drawLine(8, 14, 8, 3);
                        g.drawLine(12, 14, 12, 10);
                        break;
                    case "path":      // zigzag route with start dot
                        g.drawPolyline(new int[]{3, 12, 4, 13}, new int[]{3, 6, 10, 14}, 4);
                        g.fillOval(1, 1, 4, 4);
                        break;
                    case "stats":     // trend line over an axis
                        g.drawLine(2, 14, 14, 14);
                        g.drawPolyline(new int[]{2, 6, 9, 14}, new int[]{11, 7, 9, 3}, 4);
                        break;
                    case "home":
                        g.drawPolyline(new int[]{1, 8, 15}, new int[]{8, 1, 8}, 3);   // roof
                        g.drawPolyline(new int[]{3, 3, 13, 13}, new int[]{7, 14, 14, 7}, 4);   // walls
                        break;
                    default:
                        break;
                }
                g.dispose();
            }

            public int getIconWidth() {
                return s;
            }

            public int getIconHeight() {
                return s;
            }
        };
    }
}