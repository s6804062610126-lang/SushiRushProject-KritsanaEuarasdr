import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class FX {
    static class FloatText {
        String text;
        double x;
        double y;
        double life;
        Color color;

        FloatText(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.life = 0.9;
            this.color = color;
        }
    }

    static class Spark {
        double x;
        double y;
        double vx;
        double vy;
        double life;
        Color color;

        Spark(double x, double y, Color color, Random rand, double power) {
            this.x = x;
            this.y = y;
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = (40 + rand.nextDouble() * 90) * power;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed - 40;
            this.life = 0.35 + rand.nextDouble() * 0.35;
            this.color = color;
        }
    }

    private final ArrayList<FloatText> texts = new ArrayList<>();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final Random rand = new Random();

    public void burst(int x, int y, boolean good) {
        Color color = good ? new Color(120, 230, 120) : new Color(255, 90, 90);
        int n = good ? 16 : 10;
        for (int i = 0; i < n; i++) {
            sparks.add(new Spark(x, y, color, rand, 1.0));
        }
    }

    // ประกายเล็ก ๆ ตอนวางวัตถุดิบ
    public void puff(int x, int y) {
        Color color = new Color(255, 250, 230);
        for (int i = 0; i < 8; i++) {
            sparks.add(new Spark(x, y, color, rand, 0.6));
        }
    }

    // พลุหลายสี ใช้ตอนเลเวลอัปหรือเข้าโหมด Fever
    public void confetti(int x, int y) {
        Color[] colors = {
            new Color(255, 205, 70), new Color(255, 120, 120),
            new Color(120, 200, 255), new Color(140, 230, 140)
        };
        for (int i = 0; i < 40; i++) {
            sparks.add(new Spark(x, y, colors[i % colors.length], rand, 2.2));
        }
    }

    public void popup(String text, int x, int y, boolean good) {
        Color color = good ? new Color(255, 240, 140) : new Color(255, 160, 160);
        texts.add(new FloatText(text, x, y, color));
    }

    public void update(double dt) {
        Iterator<FloatText> ti = texts.iterator();
        while (ti.hasNext()) {
            FloatText t = ti.next();
            t.y -= 55 * dt;
            t.life -= dt;
            if (t.life <= 0) {
                ti.remove();
            }
        }
        Iterator<Spark> si = sparks.iterator();
        while (si.hasNext()) {
            Spark s = si.next();
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vy += 180 * dt;
            s.life -= dt;
            if (s.life <= 0) {
                si.remove();
            }
        }
    }

    // ข้อความลอยจะวางกึ่งกลางที่ตำแหน่ง x และไม่ล้นขอบจอ
    public void draw(Graphics g, Font font, int screenWidth) {
        for (Spark s : sparks) {
            int a = (int) Math.max(0, Math.min(255, s.life * 500));
            g.setColor(new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), a));
            g.fillOval((int) s.x, (int) s.y, 6, 6);
        }
        g.setFont(font);
        for (FloatText t : texts) {
            int a = (int) Math.max(0, Math.min(255, t.life * 280));
            int w = g.getFontMetrics().stringWidth(t.text);
            int x = (int) t.x - w / 2;
            x = Math.max(6, Math.min(screenWidth - w - 6, x));
            g.setColor(new Color(0, 0, 0, a * 2 / 3));
            g.drawString(t.text, x + 2, (int) t.y + 2);
            g.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), a));
            g.drawString(t.text, x, (int) t.y);
        }
    }

    public void clear() {
        texts.clear();
        sparks.clear();
    }
}
