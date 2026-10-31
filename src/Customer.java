import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

public class Customer {
    static final int WALKING_IN = 0;
    static final int WAITING = 1;
    static final int HAPPY = 2;
    static final int ANGRY = 3;
    static final int WALKING_OUT = 4;

    // ผลลัพธ์ของการเสิร์ฟ
    static final int SERVE_WRONG = 0;
    static final int SERVE_NEXT = 1;
    static final int SERVE_DONE = 2;
    static final int SERVE_MORE = 3;

    static final int DOOR_X = 760;
    static final int DRAW_W = 138;
    static final int DRAW_H = 290;
    static final int WALK_IN_SPEED = 260;
    static final int WALK_OUT_SPEED = 320;

    // เวลาแสดงอารมณ์ (วินาที) ก่อนเดินออก
    static final double REACTION_TIME = 0.55;
    static final double TIMEOUT_REACTION_TIME = 0.45;
    static final double CHEER_TIME = 0.45;

    private static final Random RAND = new Random();

    private int slot;
    private double x;
    private double y;
    private int standY;
    private int targetX;
    private double walkSpeed;
    private double patienceDrain;
    private ArrayList<Integer> orders = new ArrayList<>();
    private boolean hasReordered;
    private int state;
    private int skin;
    protected double patience;
    protected double maxPatience;
    private double reactionTime;
    private double cheerTime;
    private boolean timedOut;

    protected double walkPhase;
    private double bubbleScale;
    private double shake;
    private double hop;

    private Sprite normalFace;
    private Sprite angryFace;
    private Sprite happyFace;
    private Sprite[] orderBubbles;
    private Sprite[] patienceBars;

    // walkSpeed = ความเร็วเดิน (1.0 = ปกติ), patienceDrain = หลอดความอดทนลดเร็วแค่ไหน (1.0 = ปกติ)
    public Customer(int slot, int targetX, int standY, double patienceSeconds, double walkSpeed,
            double patienceDrain, int orderCount,
            Sprite[] girl, Sprite[] guy, Sprite[] bubbles, Sprite[] bars) {
        this.slot = slot;
        this.targetX = targetX;
        this.x = DOOR_X;
        this.y = standY;
        this.standY = standY;
        this.walkSpeed = walkSpeed;
        this.patienceDrain = patienceDrain;
        this.state = WALKING_IN;
        this.reactionTime = 0;
        this.timedOut = false;
        this.hasReordered = false;
        this.orderBubbles = bubbles;
        this.patienceBars = bars;
        this.walkPhase = RAND.nextDouble() * 6;
        this.bubbleScale = 0;

        this.skin = RAND.nextInt(2);
        for (int i = 0; i < Math.max(1, orderCount); i++) {
            orders.add(RAND.nextInt(4));
        }
        this.maxPatience = skin == 0 ? Math.max(8, patienceSeconds - 2) : patienceSeconds + 3;
        this.patience = this.maxPatience;

        Sprite[] faces = skin == 0 ? girl : guy;
        normalFace = pick(faces, 0);
        angryFace = pick(faces, 1);
        happyFace = pick(faces, 2);
    }

    // ดึงรูปจาก array อย่างปลอดภัย ถ้า index เกินหรือ array เป็น null จะคืน null แทนการ crash
    private static Sprite pick(Sprite[] sprites, int index) {
        if (sprites == null || index < 0 || index >= sprites.length) {
            return null;
        }
        return sprites[index];
    }

    public int getSlot() {
        return slot;
    }

    public int getOrderCount() {
        return orders.size();
    }

    // ตัวคูณคะแนนของลูกค้าคนนี้ (ลูกค้าพิเศษ override ได้)
    public int getScoreMultiplier() {
        return 1;
    }

    // ความอดทนที่เหลือ เป็นสัดส่วน 0.0 - 1.0
    public double getPatienceRatio() {
        if (maxPatience <= 0) {
            return 0;
        }
        return patience / maxPatience;
    }

    // คืน true เพียงครั้งเดียวเมื่อลูกค้าหมดความอดทน เพื่อไม่ให้หักชีวิตซ้ำ
    public boolean consumeTimeout() {
        if (timedOut) {
            timedOut = false;
            return true;
        }
        return false;
    }

    public int drawX() {
        return (int) Math.round(x + shake);
    }

    public int drawY() {
        return (int) Math.round(y - hop);
    }

    public boolean isWaiting() {
        return state == WAITING;
    }

    public boolean isGone() {
        return state == WALKING_OUT && x >= DOOR_X;
    }

    public boolean contains(int mx, int my) {
        int dx = drawX();
        int dy = drawY();
        return mx >= dx && mx <= dx + DRAW_W && my >= dy && my <= dy + DRAW_H;
    }

    // เสิร์ฟซูชิให้ลูกค้า แล้วบอกผลลัพธ์ว่าเป็นแบบไหน
    // SERVE_WRONG = ไม่ได้สั่งจานนี้, SERVE_NEXT = ยังเหลืออีกจาน,
    // SERVE_DONE = ครบแล้วกลับบ้าน, SERVE_MORE = ครบแล้วแต่ขอสั่งเพิ่ม
    public int serve(int sushiType, double reorderChance) {
        if (state != WAITING) {
            return SERVE_WRONG;
        }
        int index = orders.indexOf(sushiType);
        if (index < 0) {
            serveWrong();
            return SERVE_WRONG;
        }
        orders.remove(index);

        if (!orders.isEmpty()) {
            patience = Math.min(maxPatience, patience + maxPatience * 0.25);
            cheerTime = CHEER_TIME;
            return SERVE_NEXT;
        }
        if (!hasReordered && RAND.nextDouble() < reorderChance) {
            hasReordered = true;
            orders.add(RAND.nextInt(4));
            patience = maxPatience;
            bubbleScale = 0;
            cheerTime = CHEER_TIME;
            return SERVE_MORE;
        }
        serveCorrect();
        return SERVE_DONE;
    }

    private void serveCorrect() {
        state = HAPPY;
        reactionTime = REACTION_TIME;
        hop = 18;
    }

    private void serveWrong() {
        state = ANGRY;
        reactionTime = REACTION_TIME;
        patience = 0;
        shake = 8;
    }

    public void update(double dt) {
        walkPhase += dt * 10 * walkSpeed;
        if (state == WALKING_IN) {
            x -= WALK_IN_SPEED * walkSpeed * dt;
            y = standY + Math.sin(walkPhase) * 5;
            bubbleScale = Math.min(1, bubbleScale + dt * 2.5);
            if (x <= targetX) {
                x = targetX;
                y = standY;
                state = WAITING;
                bubbleScale = 1;
            }
        } else if (state == WAITING) {
            y = standY + Math.sin(walkPhase * 0.45) * 2.2;
            bubbleScale = Math.min(1, bubbleScale + dt * 4);
            if (cheerTime > 0) {
                // กระโดดดีใจตอนได้ของ แต่ยังรออยู่
                cheerTime -= dt;
                hop = Math.abs(Math.sin(cheerTime * 14)) * 14;
            } else {
                hop *= Math.max(0, 1 - dt * 10);
            }
            if (getPatienceRatio() < 0.28) {
                shake = Math.sin(walkPhase * 3) * 3;
            } else {
                shake *= Math.max(0, 1 - dt * 8);
            }
            patience -= dt * patienceDrain;
            if (patience <= 0) {
                patience = 0;
                timedOut = true;
                state = ANGRY;
                reactionTime = TIMEOUT_REACTION_TIME;
                shake = 10;
            }
        } else if (state == HAPPY || state == ANGRY) {
            reactionTime -= dt;
            if (state == HAPPY) {
                hop = Math.abs(Math.sin(walkPhase * 1.4)) * 16;
            } else {
                shake = Math.sin(walkPhase * 8) * 7;
            }
            if (reactionTime <= 0) {
                state = WALKING_OUT;
                hop = 0;
            }
        } else if (state == WALKING_OUT) {
            x += WALK_OUT_SPEED * walkSpeed * dt;
            y = standY + Math.sin(walkPhase) * 5;
            shake *= Math.max(0, 1 - dt * 6);
            hop *= Math.max(0, 1 - dt * 8);
        }
    }

    public void draw(Graphics g, Component c, boolean highlight) {
        int dx = drawX();
        int dy = drawY();

        if (highlight && isWaiting()) {
            g.setColor(new Color(255, 230, 120, 80));
            g.fillRoundRect(dx - 8, dy - 8, DRAW_W + 16, DRAW_H + 16, 18, 18);
        }

        drawAura(g, dx, dy);

        Sprite face = normalFace;
        if (state == HAPPY) {
            face = happyFace;
        } else if (state == ANGRY) {
            face = angryFace;
        }
        if (face != null) {
            face.draw(g, c, dx, dy, DRAW_W, DRAW_H);
        }

        drawBadge(g, dx, dy);

        boolean showOrder = state == WAITING || state == WALKING_IN;
        if (showOrder && bubbleScale > 0.05) {
            double scale = bubbleScale;
            // ใบสั่งอาหารเต้นตุบ ๆ เมื่อใกล้หมดความอดทน
            if (state == WAITING && getPatienceRatio() < 0.28) {
                scale = scale * (1 + Math.abs(Math.sin(walkPhase * 1.2)) * 0.1);
            }
            // ถ้าสั่งหลายจาน วางใบสั่งซ้อนกันลงมา
            for (int i = 0; i < orders.size(); i++) {
                Sprite bubble = pick(orderBubbles, orders.get(i));
                if (bubble != null) {
                    int bw = (int) (108 * scale);
                    int bh = (int) (70 * scale);
                    bubble.draw(g, c, dx + DRAW_W - 22, dy + 8 + i * 64, bw, bh);
                }
            }
        }
        if (state == WAITING && patienceBars != null && patienceBars.length > 0) {
            int maxIndex = patienceBars.length - 1;
            int bars = (int) Math.round(getPatienceRatio() * maxIndex);
            if (bars < 0) {
                bars = 0;
            }
            if (bars > maxIndex) {
                bars = maxIndex;
            }
            Sprite bar = patienceBars[maxIndex - bars];
            if (bar != null) {
                bar.draw(g, c, dx + 6, dy - 20, DRAW_W - 12, 16);
            }
        }
    }

    // วาดสิ่งที่อยู่ข้างหลังตัวลูกค้า (คลาสลูกเลือก override ได้)
    protected void drawAura(Graphics g, int dx, int dy) {
    }

    // วาดสิ่งที่อยู่บนตัวลูกค้า เช่น มงกุฎ (คลาสลูกเลือก override ได้)
    protected void drawBadge(Graphics g, int dx, int dy) {
    }
}
