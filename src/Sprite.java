import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.HashMap;
import javax.imageio.ImageIO;

// รูปภาพหนึ่งรูปในเกม
// รูปต้นฉบับมีขนาดใหญ่มาก (6000x3375) ถ้าย่อทุกเฟรมเกมจะกระตุก
// คลาสนี้จึง (1) ตัดขอบใสทิ้ง (2) ย่อรูปครั้งเดียวตอนโหลด (3) จำรูปที่ย่อตามขนาดที่วาดจริงไว้ใช้ซ้ำ
public class Sprite {
    static final int MAX_SIDE = 1024;
    static final int MAX_CACHE = 6;

    private final BufferedImage image;
    private final int fullW;
    private final int fullH;
    private final int sx;
    private final int sy;
    private final int sw;
    private final int sh;
    private final HashMap<String, BufferedImage> cache = new HashMap<>();

    private Sprite(BufferedImage image, int fullW, int fullH, int sx, int sy, int sw, int sh) {
        this.image = image;
        this.fullW = Math.max(1, fullW);
        this.fullH = Math.max(1, fullH);
        this.sx = sx;
        this.sy = sy;
        this.sw = Math.max(1, sw);
        this.sh = Math.max(1, sh);
    }

    private static Sprite empty() {
        return new Sprite(null, 1, 1, 0, 0, 1, 1);
    }

    // หาไฟล์จากหลายตำแหน่ง เพื่อให้รันได้ทั้งจากโฟลเดอร์โปรเจกต์และโฟลเดอร์ src
    public static File findFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        File png = new File(path.replace(".jpg", ".png"));
        if (png.exists()) {
            return png;
        }
        return new File("..", path);
    }

    public static Sprite load(String path) {
        File file = findFile(path);
        if (!file.exists()) {
            System.out.println("ไม่พบไฟล์รูป: " + path);
            return empty();
        }
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) {
                System.out.println("อ่านไฟล์รูปไม่ได้: " + path);
                return empty();
            }
            int[] box = contentBox(raw);
            BufferedImage cropped = raw.getSubimage(box[0], box[1], box[2], box[3]);

            // ย่อส่วนที่ตัดแล้วให้ด้านยาวสุดไม่เกิน MAX_SIDE
            double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(box[2], box[3]));
            int w = Math.max(1, (int) Math.round(box[2] * scale));
            int h = Math.max(1, (int) Math.round(box[3] * scale));
            BufferedImage small = resize(cropped, w, h);
            return new Sprite(small, raw.getWidth(), raw.getHeight(), box[0], box[1], box[2], box[3]);
        } catch (Exception e) {
            System.out.println("โหลดรูปไม่สำเร็จ: " + path);
            return empty();
        }
    }

    // วาดเฉพาะส่วนที่มีภาพ (ตัดขอบใสแล้ว) ให้พอดีกรอบ w x h
    public void draw(Graphics g, ImageObserver obs, int x, int y, int w, int h) {
        drawContent(g, obs, x, y, w, h);
    }

    // วาดทั้งรูปเหมือนรูปต้นฉบับ (รวมขอบใส) ให้พอดีกรอบ w x h
    public void drawFull(Graphics g, ImageObserver obs, int x, int y, int w, int h) {
        double kx = (double) w / fullW;
        double ky = (double) h / fullH;
        int dx = x + (int) Math.round(sx * kx);
        int dy = y + (int) Math.round(sy * ky);
        int dw = (int) Math.round(sw * kx);
        int dh = (int) Math.round(sh * ky);
        drawContent(g, obs, dx, dy, dw, dh);
    }

    private void drawContent(Graphics g, ImageObserver obs, int x, int y, int w, int h) {
        if (image == null || w <= 0 || h <= 0) {
            return;
        }
        String key = w + "x" + h;
        BufferedImage scaled = cache.get(key);
        if (scaled == null && cache.size() < MAX_CACHE) {
            scaled = resize(image, w, h);
            cache.put(key, scaled);
        }
        if (scaled != null) {
            // ขนาดตรงกันพอดี วาดได้เร็วมากเพราะไม่ต้องย่อ
            g.drawImage(scaled, x, y, obs);
        } else {
            // ขนาดเปลี่ยนทุกเฟรม (เช่นตอนเต้นตุบ ๆ) ให้ย่อจากรูปเล็กที่เตรียมไว้แทน
            g.drawImage(image, x, y, w, h, obs);
        }
    }

    // ย่อรูปแบบค่อย ๆ ลดทีละครึ่งเพื่อให้ภาพยังคมชัด
    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage current = src;
        int cw = src.getWidth();
        int ch = src.getHeight();
        do {
            cw = Math.max(w, cw / 2);
            ch = Math.max(h, ch / 2);
            BufferedImage next = newImage(cw, ch);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, cw, ch, null);
            g.dispose();
            current = next;
        } while (cw != w || ch != h);
        return current;
    }

    // สร้างรูปในรูปแบบที่การ์ดจอวาดได้เร็วที่สุด
    private static BufferedImage newImage(int w, int h) {
        if (!GraphicsEnvironment.isHeadless()) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            return gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private static int[] contentBox(BufferedImage im) {
        int w = im.getWidth();
        int h = im.getHeight();
        int minX = w;
        int minY = h;
        int maxX = 0;
        int maxY = 0;
        int step = 4;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int a = (im.getRGB(x, y) >>> 24) & 0xFF;
                if (a > 12) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }
        if (maxX <= minX || maxY <= minY) {
            return new int[] {0, 0, w, h};
        }
        int pad = 6;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad);
        maxY = Math.min(h - 1, maxY + pad);
        return new int[] {minX, minY, maxX - minX, maxY - minY};
    }
}
