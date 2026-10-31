import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

// รวมเมธอดวาด UI ที่ใช้ซ้ำบ่อย ๆ (กล่อง, ตัวหนังสือมีเงา, หัวใจ, หลอด)
public final class UI {
    static final Color PANEL = new Color(34, 20, 16, 215);
    static final Color BORDER = new Color(255, 214, 150);
    static final Color CREAM = new Color(255, 236, 190);
    static final Color GOLD = new Color(255, 205, 70);
    static final Color ORANGE = new Color(255, 140, 40);
    static final Color RED = new Color(230, 70, 80);
    static final Color GREEN = new Color(120, 225, 120);
    static final Color SHADOW = new Color(0, 0, 0, 170);

    private UI() {
    }

    static void panel(Graphics g, int x, int y, int w, int h) {
        panel(g, x, y, w, h, PANEL);
    }

    static void panel(Graphics g, int x, int y, int w, int h, Color fill) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x + 4, y + 5, w, h, 18, 18);
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 18, 18);

        Graphics2D g2 = (Graphics2D) g;
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(2f));
        g.setColor(BORDER);
        g.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 16, 16);
        g2.setStroke(old);
    }

    static void text(Graphics g, String s, int x, int y, Color color) {
        g.setColor(SHADOW);
        g.drawString(s, x + 2, y + 2);
        g.setColor(color);
        g.drawString(s, x, y);
    }

    static void textCentered(Graphics g, String s, int centerX, int y, Color color) {
        int w = g.getFontMetrics().stringWidth(s);
        text(g, s, centerX - w / 2, y, color);
    }

    static void textRight(Graphics g, String s, int rightX, int y, Color color) {
        int w = g.getFontMetrics().stringWidth(s);
        text(g, s, rightX - w, y, color);
    }

    // ตัวหนังสือหัวเรื่อง มีขอบเข้มและเงาสีแดงแบบเกมย้อนยุค
    static void title(Graphics g, String s, int centerX, int y) {
        int x = centerX - g.getFontMetrics().stringWidth(s) / 2;
        g.setColor(new Color(190, 45, 45));
        g.drawString(s, x + 4, y + 5);
        g.setColor(new Color(45, 18, 12));
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dy = -3; dy <= 3; dy += 3) {
                g.drawString(s, x + dx, y + dy);
            }
        }
        g.setColor(new Color(255, 228, 150));
        g.drawString(s, x, y);
    }

    static void heart(Graphics g, int x, int y, int size, Color color) {
        int r = size / 2;
        g.setColor(color);
        g.fillOval(x, y, r + 1, r + 1);
        g.fillOval(x + r - 1, y, r + 1, r + 1);
        int[] px = {x, x + size, x + size / 2};
        int[] py = {y + r / 2 + 2, y + r / 2 + 2, y + size};
        g.fillPolygon(px, py, 3);
        g.setColor(new Color(255, 255, 255, 110));
        g.fillOval(x + r / 3, y + r / 4, r / 3 + 1, r / 3 + 1);
    }

    // หลอดแสดงค่า ratio ตั้งแต่ 0.0 ถึง 1.0
    static void bar(Graphics g, int x, int y, int w, int h, double ratio, Color fill) {
        if (ratio < 0) {
            ratio = 0;
        }
        if (ratio > 1) {
            ratio = 1;
        }
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(fill);
        g.fillRoundRect(x, y, (int) (w * ratio), h, h, h);
    }
}
