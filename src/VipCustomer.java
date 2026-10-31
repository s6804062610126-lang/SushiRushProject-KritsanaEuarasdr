import java.awt.Color;
import java.awt.Graphics;

// ลูกค้า VIP: ใจร้อนกว่าปกติ แต่ให้คะแนน x2 (ตัวอย่างการสืบทอดคลาสและ override เมธอด)
public class VipCustomer extends Customer {
    static final double PATIENCE_FACTOR = 0.75;

    public VipCustomer(int slot, int targetX, int standY, double patienceSeconds, double walkSpeed,
            double patienceDrain, int orderCount,
            Sprite[] girl, Sprite[] guy, Sprite[] bubbles, Sprite[] bars) {
        super(slot, targetX, standY, patienceSeconds, walkSpeed, patienceDrain, orderCount,
                girl, guy, bubbles, bars);
        maxPatience = maxPatience * PATIENCE_FACTOR;
        patience = maxPatience;
    }

    @Override
    public int getScoreMultiplier() {
        return 2;
    }

    // แสงสีทองด้านหลังตัวลูกค้า
    @Override
    protected void drawAura(Graphics g, int dx, int dy) {
        int alpha = (int) (60 + 35 * Math.sin(walkPhase * 0.6));
        g.setColor(new Color(255, 210, 80, alpha));
        g.fillOval(dx - 12, dy + 10, DRAW_W + 24, DRAW_H - 30);
    }

    // มงกุฎบนหัว
    @Override
    protected void drawBadge(Graphics g, int dx, int dy) {
        int w = 46;
        int h = 24;
        int x0 = dx + DRAW_W / 2 - w / 2;
        int y0 = dy - 2;
        int[] px = {x0, x0, x0 + w / 4, x0 + w / 2, x0 + 3 * w / 4, x0 + w, x0 + w};
        int[] py = {y0 + h, y0 + 4, y0 + 12, y0, y0 + 12, y0 + 4, y0 + h};
        g.setColor(new Color(255, 200, 50));
        g.fillPolygon(px, py, px.length);
        g.setColor(new Color(120, 70, 10));
        g.drawPolygon(px, py, px.length);
        g.setColor(new Color(220, 40, 60));
        g.fillOval(x0 + w / 2 - 4, y0 + h - 12, 8, 8);
        g.setColor(new Color(60, 160, 230));
        g.fillOval(x0 + 6, y0 + h - 10, 6, 6);
        g.fillOval(x0 + w - 12, y0 + h - 10, 6, 6);
    }
}
