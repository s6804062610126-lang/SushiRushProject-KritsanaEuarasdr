import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GamePanel extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

    static final int TARGET_FPS = 60;

    static final int MENU = 0;
    static final int PLAYING = 1;
    static final int GAME_OVER = 2;
    static final int PAUSED = 3;
    static final int MODE_SELECT = 4;

    static final int MAX_CUSTOMERS = 3;
    static final int STAND_Y = 150;
    static final int[] SLOT_X = {100, 300, 500};

    static final int PLATE_X = 592;
    static final int PLATE_Y = 508;
    static final int PLATE_W = 68;
    static final int PLATE_H = 46;

    static final double FEEDBACK_TIME = 0.65;
    static final int START_LIVES = 3;
    static final int SERVES_PER_LEVEL = 5;
    static final int FEVER_COMBO = 5;
    static final double FEVER_DURATION = 8.0;
    static final int LIFE_BONUS_COMBO = 10;
    static final double LEVEL_BANNER_TIME = 1.8;

    // พื้นที่คลิกของแต่ละจุดบนโต๊ะ (ตรงกับตำแหน่งรูป)
    static final Rectangle RICE_BOX = new Rectangle(24, 402, 154, 178); // เฉพาะตัวหม้อ ไม่รวมฝา
    static final Rectangle SALMON_BOX = new Rectangle(182, 452, 94, 42);
    static final Rectangle TUNA_BOX = new Rectangle(182, 494, 94, 40);
    static final Rectangle TAMAGO_BOX = new Rectangle(182, 534, 94, 46);
    static final Rectangle SEAWEED_BOX = new Rectangle(280, 462, 90, 126);
    static final Rectangle TRASH_BOX = new Rectangle(700, 510, 100, 90);
    static final Rectangle PLATE_BOX = new Rectangle(580, 480, 95, 92);

    static final Rectangle START_BTN = new Rectangle(275, 158, 250, 72);
    static final Rectangle AGAIN_BTN = new Rectangle(265, 322, 270, 80);
    static final Rectangle CHANGE_MODE_BTN = new Rectangle(290, 420, 220, 42);
    static final Rectangle BACK_BTN = new Rectangle(330, 500, 140, 40);
    static final Rectangle PAUSE_BTN = new Rectangle(740, 14, 50, 50);
    static final Rectangle RESTART_BTN = new Rectangle(270, 262, 260, 52);
    static final Rectangle HOME_BTN = new Rectangle(270, 328, 260, 52);

    final int screenWidth = 800;
    final int screenHeight = 600;

    Sprite bgImage;
    Sprite tableImage;
    Sprite matImage;
    Sprite plateImage;
    Sprite riceImage;
    Sprite fishTrayImage;
    Sprite seaweedImage;
    Sprite trashImage;
    Sprite carpetImage;
    Sprite knifeImage;
    Sprite wasabiImage;
    Sprite shoyuImage;
    Sprite gingerImage;
    Sprite sushiSalmonImage;
    Sprite sushiTunaImage;
    Sprite sushiTamagoImage;
    Sprite sushiMakiImage;
    Sprite riceBallImage;
    Sprite matSeaweedImage;
    Sprite matSeaweedRiceImage;
    Sprite startButton;
    Sprite playAgainButton;
    Sprite checkIcon;
    Sprite warnIcon;
    Sprite[] girlSprites;
    Sprite[] guySprites;
    Sprite[] orderBubbles;
    Sprite[] patienceBars;

    Font pixelBig;
    Font pixelMid;
    Font pixelFont;
    Font pixelSmall;

    Customer[] customers = new Customer[MAX_CUSTOMERS];
    GameMode mode = GameMode.INFINITE;
    double timeLeft = 0;
    boolean timeUp = false;
    Random rand = new Random();

    boolean hasRice = false;
    boolean hasSalmon = false;
    boolean hasTuna = false;
    boolean hasTamago = false;
    boolean hasSeaweed = false;

    int gameState = MENU;
    int score = 0;
    double displayScore = 0;
    int lives = START_LIVES;
    int level = 1;
    int served = 0;
    boolean newBest = false;
    double feedbackTime = 0;
    boolean lastServeCorrect = false;
    double spawnCooldown = 0;

    int combo = 0;
    int bestCombo = 0;
    double feverTime = 0;
    double levelBannerTime = 0;
    double animTime = 0;
    double sushiPulse = 0;
    double flashWrong = 0;
    FX fx = new FX();
    boolean dragging = false;
    int dragType = Sushi.NONE;
    int dragX;
    int dragY;
    int mouseX = -1;
    int mouseY = -1;

    Thread gameLoop;
    long lastNanos;

    public GamePanel() {
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        loadAssets();
    }

    // เริ่ม game loop: อัปเดตและวาดใหม่ 60 ครั้งต่อวินาทีอย่างสม่ำเสมอ
    public void start() {
        if (gameLoop != null) {
            return;
        }
        lastNanos = System.nanoTime();
        gameLoop = new Thread(this::runLoop, "game-loop");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    private void runLoop() {
        long frameNanos = 1_000_000_000L / TARGET_FPS;
        long nextFrame = System.nanoTime();
        while (true) {
            try {
                // อัปเดตและวาดบน Swing thread เพื่อไม่ให้ข้อมูลเกมชนกันระหว่าง thread
                SwingUtilities.invokeAndWait(this::tick);
            } catch (InterruptedException e) {
                return;
            } catch (InvocationTargetException e) {
                e.getCause().printStackTrace();
            }

            nextFrame += frameNanos;
            long now = System.nanoTime();
            if (now - nextFrame > frameNanos * 5) {
                // ช้าไปมาก (เช่นลากหน้าต่าง) ไม่ต้องพยายามไล่ตามเฟรมที่หายไป
                nextFrame = now;
            }
            // นอนรอจนใกล้ถึงเฟรมถัดไป แล้วรอส่วนที่เหลือแบบละเอียด
            while (nextFrame - System.nanoTime() > 2_000_000) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
            while (System.nanoTime() < nextFrame) {
                Thread.onSpinWait();
            }
        }
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (dt > 0.05) {
            dt = 0.05;
        }
        updateGame(dt);
        paintImmediately(0, 0, getWidth(), getHeight());
        Toolkit.getDefaultToolkit().sync();
    }

    // ยิ่งเลเวลสูง ลูกค้ายิ่งใจร้อน (แล้วปรับตามโหมดที่เลือก)
    private double currentPatience() {
        double patience = 16.5 - (level - 1) * 1.3;
        if (patience < 6.5) {
            patience = 6.5;
        }
        return patience;
    }

    // โอกาสที่ลูกค้าจะสั่ง 2 จาน เพิ่มขึ้นตามเลเวล
    private double doubleOrderChance() {
        return Math.min(0.5, mode.getDoubleOrderChance() + (level - 1) * 0.03);
    }

    // ตำแหน่งการ์ดเลือกโหมดแต่ละใบ
    private Rectangle modeCard(int index) {
        return new Rectangle(160, 122 + index * 92, 480, 80);
    }

    // โอกาสที่ลูกค้า VIP จะมา เริ่มตั้งแต่เลเวล 2
    private double vipChance() {
        if (level < 2) {
            return 0;
        }
        return Math.min(0.3, 0.08 + level * 0.04);
    }

    private boolean isFever() {
        return feverTime > 0;
    }

    private void loadAssets() {
        bgImage = Sprite.load("assets/images/background.png");
        tableImage = Sprite.load("assets/images/table.png");
        matImage = Sprite.load("assets/images/18.png");
        plateImage = Sprite.load("assets/images/6.png");
        riceImage = Sprite.load("assets/images/2.png");
        fishTrayImage = Sprite.load("assets/images/3.png");
        seaweedImage = Sprite.load("assets/images/11.png");
        trashImage = Sprite.load("assets/images/Trash.png");
        carpetImage = Sprite.load("assets/images/carpet.png");
        knifeImage = Sprite.load("assets/images/knife.png");
        wasabiImage = Sprite.load("assets/images/wasabi.png");
        shoyuImage = Sprite.load("assets/images/shoyu.png");
        gingerImage = Sprite.load("assets/images/ginger.png");
        sushiSalmonImage = Sprite.load("assets/images/salmonsushi.png");
        sushiTunaImage = Sprite.load("assets/images/13.png");
        sushiTamagoImage = Sprite.load("assets/images/14.png");
        sushiMakiImage = Sprite.load("assets/images/15.png");
        riceBallImage = Sprite.load("assets/images/16.png");
        matSeaweedImage = Sprite.load("assets/images/17.png");
        matSeaweedRiceImage = Sprite.load("assets/images/19.png");
        startButton = Sprite.load("assets/images/27.png");
        playAgainButton = Sprite.load("assets/images/28.png");
        checkIcon = Sprite.load("assets/images/29.png");
        warnIcon = Sprite.load("assets/images/30.png");

        girlSprites = new Sprite[] {
            Sprite.load("assets/images/20.png"),
            Sprite.load("assets/images/21.png"),
            Sprite.load("assets/images/22.png")
        };
        guySprites = new Sprite[] {
            Sprite.load("assets/images/23.png"),
            Sprite.load("assets/images/24.png"),
            Sprite.load("assets/images/25.png")
        };
        orderBubbles = new Sprite[] {
            Sprite.load("assets/images/46.png"),
            Sprite.load("assets/images/47.png"),
            Sprite.load("assets/images/48.png"),
            Sprite.load("assets/images/49.png")
        };
        patienceBars = new Sprite[11];
        for (int i = 0; i < 11; i++) {
            patienceBars[i] = Sprite.load("assets/images/" + (31 + i) + ".png");
        }

        try (InputStream fontIn = Sprite.open("assets/fonds/PressStart2P-Regular.ttf")) {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT, fontIn).deriveFont(16f);
            pixelBig = pixelFont.deriveFont(30f);
            pixelMid = pixelFont.deriveFont(22f);
            pixelSmall = pixelFont.deriveFont(11f);
        } catch (Exception e) {
            System.out.println("โหลดฟอนต์ไม่สำเร็จ ใช้ฟอนต์สำรองแทน");
            pixelFont = new Font("Monospaced", Font.BOLD, 18);
            pixelBig = new Font("Monospaced", Font.BOLD, 36);
            pixelMid = new Font("Monospaced", Font.BOLD, 26);
            pixelSmall = new Font("Monospaced", Font.BOLD, 13);
        }
    }

    // ---------------------------------------------------------------- game flow

    private void startGame(GameMode selected) {
        mode = selected;
        startGame();
    }

    private void startGame() {
        gameState = PLAYING;
        timeLeft = mode.getTimeLimit();
        timeUp = false;
        score = 0;
        displayScore = 0;
        lives = START_LIVES;
        level = 1;
        served = 0;
        newBest = false;
        combo = 0;
        bestCombo = 0;
        feverTime = 0;
        levelBannerTime = 0;
        feedbackTime = 0;
        flashWrong = 0;
        fx.clear();
        dragging = false;
        spawnCooldown = 0.25;
        clearStation();
        for (int i = 0; i < MAX_CUSTOMERS; i++) {
            customers[i] = null;
        }
        spawnCustomer();
    }

    // กลับหน้าแรก ล้างสถานะเกมที่ค้างอยู่
    private void goHome() {
        gameState = MENU;
        clearStation();
        fx.clear();
        feverTime = 0;
        levelBannerTime = 0;
        flashWrong = 0;
        feedbackTime = 0;
        for (int i = 0; i < MAX_CUSTOMERS; i++) {
            customers[i] = null;
        }
    }

    private void togglePause() {
        if (gameState == PLAYING) {
            gameState = PAUSED;
            dragging = false;
        } else if (gameState == PAUSED) {
            gameState = PLAYING;
        }
    }

    private int occupiedCount() {
        int n = 0;
        for (Customer c : customers) {
            if (c != null) {
                n++;
            }
        }
        return n;
    }

    private void spawnCustomer() {
        if (occupiedCount() >= MAX_CUSTOMERS) {
            return;
        }
        for (int i = 0; i < MAX_CUSTOMERS; i++) {
            if (customers[i] == null) {
                spawnInSlot(i);
                return;
            }
        }
    }

    private void spawnInSlot(int slot) {
        int orderCount = rand.nextDouble() < doubleOrderChance() ? 2 : 1;
        double patience = currentPatience();
        if (orderCount == 2) {
            patience = patience * 1.6;
        }
        double speed = mode.getWalkSpeed(level);
        double drain = mode.getPatienceDrain(level);
        if (rand.nextDouble() < vipChance()) {
            customers[slot] = new VipCustomer(slot, SLOT_X[slot], STAND_Y, patience, speed, drain,
                    orderCount, girlSprites, guySprites, orderBubbles, patienceBars);
        } else {
            customers[slot] = new Customer(slot, SLOT_X[slot], STAND_Y, patience, speed, drain,
                    orderCount, girlSprites, guySprites, orderBubbles, patienceBars);
        }
    }

    // หักชีวิต 1 ดวง และจบเกมเมื่อชีวิตหมด (ใช้ร่วมกันทั้งตอนหมดเวลาและเสิร์ฟผิด)
    private void loseLife() {
        combo = 0;
        feverTime = 0;
        lives--;
        if (lives <= 0) {
            lives = 0;
            endGame();
        }
    }

    private void endGame() {
        gameState = GAME_OVER;
        dragging = false;
        dragType = Sushi.NONE;
        feverTime = 0;
        newBest = mode.getHighScore().submit(score);
    }

    private void clearStation() {
        hasRice = false;
        hasSalmon = false;
        hasTuna = false;
        hasTamago = false;
        hasSeaweed = false;
        dragging = false;
        dragType = Sushi.NONE;
    }

    private boolean stationEmpty() {
        return !hasRice && !hasSeaweed && !hasSalmon && !hasTuna && !hasTamago;
    }

    private Sushi madeSushi() {
        return Sushi.fromIngredients(hasRice, hasSeaweed, hasSalmon, hasTuna, hasTamago);
    }

    private int madeType() {
        Sushi sushi = madeSushi();
        return sushi == null ? Sushi.NONE : sushi.type;
    }

    private Sprite sushiSprite(int type) {
        if (type == Sushi.MAKI) {
            return sushiMakiImage;
        }
        if (type == Sushi.SALMON) {
            return sushiSalmonImage;
        }
        if (type == Sushi.TUNA) {
            return sushiTunaImage;
        }
        if (type == Sushi.TAMAGO) {
            return sushiTamagoImage;
        }
        return null;
    }

    // ---------------------------------------------------------------- ingredients

    private void hint(String text, int x, int y) {
        fx.popup(text, x, y - 10, false);
    }

    private void addRice(int x, int y) {
        if (madeSushi() != null) {
            hint("SERVE IT FIRST", x, y);
            return;
        }
        if (hasRice) {
            hint("RICE IS READY", x, y);
            return;
        }
        hasRice = true;
        fx.puff(x, y);
    }

    private void addSeaweed(int x, int y) {
        if (madeSushi() != null) {
            hint("SERVE IT FIRST", x, y);
            return;
        }
        if (hasSeaweed) {
            hint("SEAWEED IS READY", x, y);
            return;
        }
        if (hasRice) {
            hint("SEAWEED GOES FIRST", x, y);
            return;
        }
        hasSeaweed = true;
        fx.puff(x, y);
    }

    private void addTopping(int topping, int x, int y) {
        if (madeSushi() != null) {
            hint("SERVE IT FIRST", x, y);
            return;
        }
        if (!hasRice) {
            hint("RICE FIRST!", x, y);
            return;
        }
        if (topping == Sushi.SALMON) {
            hasSalmon = true;
        } else if (topping == Sushi.TUNA) {
            hasTuna = true;
        } else {
            hasTamago = true;
        }
        fx.puff(x, y);
    }

    // ข้อความบอกว่ากำลังทำอะไรอยู่บนเสื่อ
    private String recipeText() {
        Sushi made = madeSushi();
        if (made != null) {
            return made.getName() + "  > DRAG IT!";
        }
        if (stationEmpty()) {
            if (served == 0 && level == 1) {
                return "CLICK THE RICE TO START";
            }
            return "";
        }
        String text = "";
        if (hasSeaweed) {
            text = "SEAWEED";
        }
        if (hasRice) {
            text = text.isEmpty() ? "RICE" : text + " + RICE";
            text = text + " + ?";
        } else {
            text = text + " + RICE?";
        }
        return text;
    }

    // ---------------------------------------------------------------- update

    private void updateGame(double dt) {
        animTime += dt;
        fx.update(dt);
        if (flashWrong > 0) {
            flashWrong -= dt;
        }
        sushiPulse += dt;
        if (feedbackTime > 0) {
            feedbackTime -= dt;
        }
        // ตัวเลขคะแนนค่อย ๆ วิ่งขึ้นไปหาคะแนนจริง
        displayScore += (score - displayScore) * Math.min(1, dt * 10);
        if (Math.abs(score - displayScore) < 1) {
            displayScore = score;
        }
        if (gameState != PLAYING) {
            return;
        }
        if (feverTime > 0) {
            feverTime -= dt;
        }
        if (levelBannerTime > 0) {
            levelBannerTime -= dt;
        }
        if (mode.isTimed()) {
            timeLeft -= dt;
            if (timeLeft <= 0) {
                timeLeft = 0;
                timeUp = true;
                endGame();
                return;
            }
        }

        spawnCooldown -= dt;
        if (spawnCooldown <= 0 && occupiedCount() < MAX_CUSTOMERS) {
            spawnCustomer();
            spawnCooldown = 0.9;
        }

        for (int i = 0; i < MAX_CUSTOMERS; i++) {
            Customer c = customers[i];
            if (c == null) {
                continue;
            }
            c.update(dt);
            if (c.consumeTimeout()) {
                lastServeCorrect = false;
                feedbackTime = FEEDBACK_TIME;
                flashWrong = 0.25;
                fx.burst(c.drawX() + 70, c.drawY() + 80, false);
                fx.popup("LEFT!", c.drawX() + Customer.DRAW_W / 2, c.drawY() + 40, false);
                loseLife();
                if (gameState != PLAYING) {
                    return;
                }
            }
            if (c.isGone()) {
                spawnInSlot(i);
            }
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawScene(g);

        boolean inGame = gameState == PLAYING || gameState == PAUSED;
        if (inGame) {
            drawStationHover(g);
            drawRecipeChip(g);
            drawFeedbackIcon(g);
            fx.draw(g, pixelSmall, screenWidth);
        }

        if (flashWrong > 0) {
            g.setColor(new Color(180, 20, 20, (int) (90 * flashWrong / 0.25)));
            g.fillRect(0, 0, screenWidth, screenHeight);
        }
        if (isFever() && inGame) {
            drawFeverBorder(g);
        }
        if (inGame) {
            drawHud(g);
        }
        if (levelBannerTime > 0 && gameState == PLAYING) {
            drawLevelBanner(g);
        }

        if (gameState == MENU) {
            drawMenu(g);
        } else if (gameState == MODE_SELECT) {
            drawModeSelect(g);
        } else if (gameState == GAME_OVER) {
            drawGameOver(g);
        } else if (gameState == PAUSED) {
            drawPause(g);
        }
        if (inGame) {
            drawPauseButton(g);
        }
    }

    private void drawScene(Graphics g) {
        if (bgImage != null) {
            bgImage.draw(g, this, 0, 0, screenWidth, 430);
        }

        if (gameState != MENU) {
            Customer hover = dragging ? customerAt(dragX, dragY) : null;
            for (Customer c : customers) {
                if (c != null) {
                    c.draw(g, this, c == hover);
                }
            }
        }

        if (tableImage != null) {
            tableImage.drawFull(g, this, -70, 100, screenWidth + 200, 830);
        }
        if (carpetImage != null) {
            carpetImage.drawFull(g, this, 290, 450, 520, 160);
        }

        int made = madeType();
        boolean sushiDone = made != Sushi.NONE;

        Sprite mat = matImage;
        if (hasSeaweed && hasRice && !sushiDone) {
            mat = matSeaweedRiceImage;
        } else if (hasSeaweed && !hasRice && !sushiDone) {
            mat = matSeaweedImage;
        }
        if (mat != null) {
            mat.drawFull(g, this, 340, 440, 300, 180);
        }

        if (hasRice && !hasSeaweed && !sushiDone && riceBallImage != null) {
            riceBallImage.drawFull(g, this, 420, 480, 120, 90);
        }
        if (plateImage != null) {
            plateImage.drawFull(g, this, 470, 440, 320, 180);
        }
        if (knifeImage != null) {
            knifeImage.drawFull(g, this, 420, 440, 320, 180);
        }
        if (riceImage != null) {
            riceImage.drawFull(g, this, -170, 290, 530, 300);
        }
        if (wasabiImage != null) {
            wasabiImage.drawFull(g, this, 235, 400, 320, 180);
        }
        if (shoyuImage != null) {
            shoyuImage.drawFull(g, this, 235, 450, 320, 180);
        }
        if (gingerImage != null) {
            gingerImage.drawFull(g, this, 580, 425, 220, 130);
        }
        if (fishTrayImage != null) {
            fishTrayImage.drawFull(g, this, 80, 440, 300, 160);
        }
        if (seaweedImage != null) {
            seaweedImage.drawFull(g, this, 175, 440, 300, 160);
        }
        if (trashImage != null) {
            trashImage.drawFull(g, this, 570, 500, 400, 200);
        }

        if (sushiDone && !dragging) {
            Sprite spr = sushiSprite(made);
            if (spr != null) {
                // แสงวงกลมใต้ซูชิที่ทำเสร็จ บอกว่าลากไปเสิร์ฟได้แล้ว
                int glow = (int) (60 + 40 * Math.sin(sushiPulse * 6));
                g.setColor(new Color(255, 240, 150, glow));
                g.fillOval(PLATE_X - 14, PLATE_Y - 10, PLATE_W + 28, PLATE_H + 20);
                double pulse = 1 + Math.sin(sushiPulse * 6) * 0.08;
                int w = (int) (PLATE_W * pulse);
                int h = (int) (PLATE_H * pulse);
                spr.draw(g, this, PLATE_X - (w - PLATE_W) / 2, PLATE_Y - (h - PLATE_H) / 2, w, h);
            }
        }

        if (dragging) {
            Sprite spr = sushiSprite(dragType);
            if (spr != null) {
                g.setColor(new Color(0, 0, 0, 70));
                g.fillOval(dragX - 30, dragY + 18, 60, 14);
                spr.draw(g, this, dragX - 38, dragY - 30, 76, 52);
            }
        }
    }

    // จุดบนโต๊ะที่เมาส์ชี้อยู่และกดได้
    private Rectangle hoveredStation() {
        if (madeSushi() != null && PLATE_BOX.contains(mouseX, mouseY)) {
            return PLATE_BOX;
        }
        Rectangle[] boxes = {TRASH_BOX, RICE_BOX, SALMON_BOX, TUNA_BOX, TAMAGO_BOX, SEAWEED_BOX};
        for (Rectangle box : boxes) {
            if (box.contains(mouseX, mouseY)) {
                return box;
            }
        }
        return null;
    }

    private void drawStationHover(Graphics g) {
        if (gameState != PLAYING || dragging) {
            return;
        }
        Rectangle r = hoveredStation();
        if (r == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        Stroke old = g2.getStroke();
        g.setColor(new Color(255, 255, 255, 35));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g2.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 245, 200, 170));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g2.setStroke(old);
    }

    private void drawRecipeChip(Graphics g) {
        String text = recipeText();
        if (text.isEmpty()) {
            return;
        }
        g.setFont(pixelSmall);
        int w = g.getFontMetrics().stringWidth(text) + 28;
        int h = 26;
        int x = 495 - w / 2;
        int y = 410;
        Color fill = madeSushi() != null ? new Color(40, 110, 50, 225) : UI.PANEL;
        UI.panel(g, x, y, w, h, fill);
        UI.textCentered(g, text, 495, y + 18, UI.CREAM);
    }

    private void drawFeedbackIcon(Graphics g) {
        if (feedbackTime <= 0) {
            return;
        }
        Sprite icon = lastServeCorrect ? checkIcon : warnIcon;
        if (icon != null) {
            double pop = 1 + Math.sin(feedbackTime * 10) * 0.12;
            int s = (int) (64 * pop);
            icon.draw(g, this, 400 - s / 2, 220 - s / 2, s, s);
        }
    }

    private void drawFeverBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Stroke old = g2.getStroke();
        double pulse = 0.5 + 0.5 * Math.sin(animTime * 8);
        g2.setStroke(new BasicStroke(10f));
        g.setColor(new Color(255, 140, 40, (int) (90 + 90 * pulse)));
        g.drawRect(5, 5, screenWidth - 10, screenHeight - 10);
        g2.setStroke(old);
    }

    private void drawHud(Graphics g) {
        // คะแนน + เลเวล
        UI.panel(g, 10, 10, 236, 62);
        g.setFont(pixelSmall);
        UI.text(g, "SCORE", 24, 32, UI.CREAM);
        g.setFont(pixelFont);
        UI.text(g, String.valueOf((int) Math.round(displayScore)), 24, 60, UI.GOLD);

        g.setColor(new Color(200, 60, 60));
        g.fillRoundRect(168, 18, 66, 24, 10, 10);
        g.setFont(pixelSmall);
        UI.textCentered(g, "LV " + level, 201, 36, Color.WHITE);
        double levelProgress = (served % SERVES_PER_LEVEL) / (double) SERVES_PER_LEVEL;
        UI.bar(g, 168, 50, 66, 8, levelProgress, UI.GREEN);

        // คอมโบ + Fever
        if (combo >= 2 || isFever()) {
            int h = isFever() ? 48 : 32;
            UI.panel(g, 10, 80, 236, h);
            g.setFont(pixelSmall);
            if (isFever()) {
                UI.text(g, "FEVER x2  COMBO " + combo, 24, 101, UI.ORANGE);
                UI.bar(g, 24, 110, 208, 8, feverTime / FEVER_DURATION, UI.ORANGE);
            } else {
                UI.text(g, "COMBO x" + combo, 24, 101, UI.GOLD);
            }
        }

        // ชีวิต + คะแนนสูงสุด
        UI.panel(g, 530, 10, 200, 62);
        for (int i = 0; i < START_LIVES; i++) {
            double beat = 1 + (i < lives ? Math.sin(animTime * 4 + i) * 0.08 : 0);
            int s = (int) (24 * beat);
            Color color = i < lives ? UI.RED : new Color(80, 66, 66);
            UI.heart(g, 556 + i * 40 - (s - 24) / 2, 18 - (s - 24) / 2, s, color);
        }
        g.setFont(pixelSmall);
        UI.text(g, "BEST " + Math.max(mode.getHighScore().getBest(), score), 546, 62, UI.CREAM);

        // นาฬิกาจับเวลา (เฉพาะโหมดจับเวลา)
        if (mode.isTimed()) {
            boolean hurry = timeLeft <= 10;
            UI.panel(g, 330, 10, 140, 54, hurry ? new Color(110, 20, 20, 225) : UI.PANEL);
            g.setFont(pixelSmall);
            UI.textCentered(g, mode.getName(), 400, 28, mode.getColor());
            g.setFont(pixelMid);
            int bump = hurry ? (int) (Math.abs(Math.sin(animTime * 6)) * 3) : 0;
            UI.textCentered(g, GameMode.formatTime(timeLeft), 400, 56 + bump,
                    hurry ? Color.WHITE : UI.GOLD);
        }
    }

    private void drawLevelBanner(Graphics g) {
        double t = levelBannerTime / LEVEL_BANNER_TIME;
        double appear = Math.min(1, (1 - t) * 6);
        double fade = Math.min(1, t * 4);
        int alpha = (int) (220 * Math.min(appear, fade));
        int y = 250;
        int h = 74;
        g.setColor(new Color(30, 15, 10, alpha));
        g.fillRect(0, y, screenWidth, h);
        g.setColor(new Color(255, 205, 70, alpha));
        g.fillRect(0, y, screenWidth, 3);
        g.fillRect(0, y + h - 3, screenWidth, 3);
        if (alpha < 60) {
            return;
        }
        int offset = (int) ((1 - appear) * 300);
        g.setFont(pixelMid);
        UI.textCentered(g, "LEVEL " + level, 400 - offset, y + 38, UI.GOLD);
        g.setFont(pixelSmall);
        String sub = mode.speedsUpWithLevel()
                ? "Customers get faster & less patient!"
                : "Customers get less patient!";
        UI.textCentered(g, sub, 400 + offset, y + 60, UI.CREAM);
    }

    private void drawButton(Graphics g, Sprite sprite, Rectangle r, int bob) {
        if (sprite == null) {
            return;
        }
        boolean hover = r.contains(mouseX, mouseY);
        double scale = hover ? 1.07 : 1.0;
        int w = (int) (r.width * scale);
        int h = (int) (r.height * scale);
        int x = r.x - (w - r.width) / 2;
        int y = r.y - (h - r.height) / 2 + bob;
        sprite.draw(g, this, x, y, w, h);
    }

    private void dimScreen(Graphics g, int topAlpha, int bottomAlpha) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(new GradientPaint(0, 0, new Color(10, 5, 5, topAlpha),
                0, screenHeight, new Color(40, 15, 10, bottomAlpha)));
        g2.fillRect(0, 0, screenWidth, screenHeight);
    }

    private void drawRecipe(Graphics g, Sprite icon, String text, int x, int y) {
        if (icon != null) {
            icon.draw(g, this, x, y, 46, 32);
        }
        UI.text(g, text, x + 54, y + 22, Color.WHITE);
    }

    private void drawMenu(Graphics g) {
        dimScreen(g, 200, 150);
        UI.panel(g, 130, 30, 540, 540);

        g.setFont(pixelBig);
        int titleBob = (int) (Math.sin(animTime * 2.4) * 5);
        UI.title(g, "SUSHI RUSH", 400, 100 + titleBob);
        g.setFont(pixelSmall);
        UI.textCentered(g, "Serve fast. Don't let them wait!", 400, 136, UI.CREAM);

        int btnBob = (int) (Math.sin(animTime * 3) * 4);
        drawButton(g, startButton, START_BTN, btnBob);

        g.setFont(pixelFont);
        UI.textCentered(g, "HOW TO PLAY", 400, 268, UI.GOLD);
        g.setColor(new Color(255, 214, 150, 120));
        g.fillRect(170, 278, 460, 2);

        g.setFont(pixelSmall);
        drawRecipe(g, sushiSalmonImage, "RICE + SALMON", 160, 292);
        drawRecipe(g, sushiTunaImage, "RICE + TUNA", 420, 292);
        drawRecipe(g, sushiTamagoImage, "RICE + EGG", 160, 336);
        drawRecipe(g, sushiMakiImage, "SEAWEED+RICE+ANY", 420, 336);

        g.setColor(new Color(255, 214, 150, 120));
        g.fillRect(170, 384, 460, 2);
        UI.textCentered(g, "Drag sushi from plate to customer", 400, 412, Color.WHITE);
        UI.textCentered(g, "Some customers want 2 dishes!", 400, 438, UI.CREAM);
        UI.textCentered(g, "Crown = VIP x2   5 combo = FEVER", 400, 464, UI.GOLD);
        UI.textCentered(g, "10 combo = +1 life   P = pause", 400, 490, Color.WHITE);

        g.setFont(pixelSmall);
        UI.textCentered(g, "ENTER = choose mode", 400, 548, new Color(190, 180, 160));
    }

    // ปุ่มแบบกล่องข้อความ (ใช้กับปุ่มที่ไม่มีรูป)
    private void drawTextButton(Graphics g, Rectangle r, String text) {
        boolean hover = r.contains(mouseX, mouseY);
        Color fill = hover ? new Color(90, 50, 30, 235) : UI.PANEL;
        UI.panel(g, r.x, r.y, r.width, r.height, fill);
        g.setFont(pixelSmall);
        UI.textCentered(g, text, r.x + r.width / 2, r.y + r.height / 2 + 6, hover ? UI.GOLD : UI.CREAM);
    }

    private void drawModeSelect(Graphics g) {
        dimScreen(g, 200, 150);
        UI.panel(g, 130, 30, 540, 540);

        g.setFont(pixelMid);
        UI.textCentered(g, "SELECT MODE", 400, 80, UI.GOLD);
        g.setFont(pixelSmall);
        UI.textCentered(g, "Click a mode or press 1-4", 400, 106, UI.CREAM);

        for (int i = 0; i < GameMode.ALL.length; i++) {
            GameMode m = GameMode.ALL[i];
            Rectangle r = modeCard(i);
            boolean hover = r.contains(mouseX, mouseY);
            int lift = hover ? -3 : 0;
            Color fill = hover ? new Color(80, 45, 28, 240) : new Color(52, 32, 24, 230);
            UI.panel(g, r.x, r.y + lift, r.width, r.height, fill);
            // แถบสีประจำโหมดด้านซ้าย
            g.setColor(m.getColor());
            g.fillRoundRect(r.x + 8, r.y + lift + 10, 8, r.height - 20, 6, 6);

            g.setFont(pixelFont);
            UI.text(g, (i + 1) + " " + m.getName(), r.x + 30, r.y + lift + 34, m.getColor());
            UI.textRight(g, m.getTimeLabel(), r.x + r.width - 18, r.y + lift + 34, Color.WHITE);
            g.setFont(pixelSmall);
            UI.text(g, m.getDescription(), r.x + 30, r.y + lift + 62, UI.CREAM);
            int best = m.getHighScore().getBest();
            if (best > 0) {
                UI.textRight(g, "BEST " + best, r.x + r.width - 18, r.y + lift + 62, UI.GOLD);
            }
        }

        drawTextButton(g, BACK_BTN, "BACK");
    }

    private void drawGameOver(Graphics g) {
        dimScreen(g, 200, 170);
        UI.panel(g, 190, 60, 420, 470);

        g.setFont(pixelBig);
        UI.title(g, timeUp ? "TIME UP!" : "GAME OVER", 400, 116);
        g.setFont(pixelSmall);
        UI.textCentered(g, "MODE: " + mode.getName(), 400, 146, mode.getColor());

        UI.textCentered(g, "FINAL SCORE", 400, 178, UI.CREAM);
        g.setFont(pixelMid);
        UI.textCentered(g, String.valueOf((int) Math.round(displayScore)), 400, 212, UI.GOLD);

        g.setFont(pixelSmall);
        if (newBest) {
            if ((int) (animTime * 4) % 2 == 0) {
                UI.textCentered(g, "NEW BEST SCORE!", 400, 242, UI.ORANGE);
            }
        } else {
            UI.textCentered(g, "BEST " + mode.getHighScore().getBest(), 400, 242, UI.CREAM);
        }
        UI.textCentered(g, "LEVEL " + level + "   SERVED " + served, 400, 274, Color.WHITE);
        UI.textCentered(g, "BEST COMBO x" + bestCombo, 400, 298, Color.WHITE);

        drawButton(g, playAgainButton, AGAIN_BTN, 0);
        drawTextButton(g, CHANGE_MODE_BTN, "CHANGE MODE");
        UI.textCentered(g, "ENTER = again   ESC = modes", 400, 500, new Color(190, 180, 160));
    }

    private void drawPause(Graphics g) {
        dimScreen(g, 150, 150);
        UI.panel(g, 230, 180, 340, 248);
        g.setFont(pixelMid);
        UI.textCentered(g, "PAUSED", 400, 234, UI.GOLD);
        drawTextButton(g, RESTART_BTN, "RESTART");
        drawTextButton(g, HOME_BTN, "HOME");
        g.setFont(pixelSmall);
        UI.textCentered(g, "P / ESC = resume", 400, 408, new Color(190, 180, 160));
    }

    // ปุ่มหยุดเกมมุมขวาบน (ตอนหยุดอยู่จะเปลี่ยนเป็นรูปปุ่มเล่น)
    private void drawPauseButton(Graphics g) {
        Rectangle r = PAUSE_BTN;
        boolean hover = r.contains(mouseX, mouseY);
        UI.panel(g, r.x, r.y, r.width, r.height, hover ? new Color(90, 50, 30, 235) : UI.PANEL);
        g.setColor(hover ? UI.GOLD : UI.CREAM);
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        if (gameState == PAUSED) {
            int[] px = {cx - 7, cx - 7, cx + 10};
            int[] py = {cy - 11, cy + 11, cy};
            g.fillPolygon(px, py, 3);
        } else {
            g.fillRoundRect(cx - 10, cy - 11, 7, 22, 3, 3);
            g.fillRoundRect(cx + 3, cy - 11, 7, 22, 3, 3);
        }
    }

    // ---------------------------------------------------------------- input

    private Customer customerAt(int mx, int my) {
        for (Customer c : customers) {
            if (c != null && c.isWaiting() && c.contains(mx, my)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        int mx = e.getX();
        int my = e.getY();

        if (gameState == MENU) {
            if (START_BTN.contains(mx, my)) {
                gameState = MODE_SELECT;
            }
            return;
        }
        if (gameState == MODE_SELECT) {
            for (int i = 0; i < GameMode.ALL.length; i++) {
                if (modeCard(i).contains(mx, my)) {
                    startGame(GameMode.ALL[i]);
                    return;
                }
            }
            if (BACK_BTN.contains(mx, my)) {
                gameState = MENU;
            }
            return;
        }
        if (gameState == GAME_OVER) {
            if (AGAIN_BTN.contains(mx, my)) {
                startGame();
            } else if (CHANGE_MODE_BTN.contains(mx, my)) {
                gameState = MODE_SELECT;
            }
            return;
        }
        if (gameState == PAUSED) {
            if (PAUSE_BTN.contains(mx, my)) {
                togglePause();
            } else if (RESTART_BTN.contains(mx, my)) {
                startGame();
            } else if (HOME_BTN.contains(mx, my)) {
                goHome();
            }
            return;
        }
        if (gameState != PLAYING) {
            return;
        }
        if (PAUSE_BTN.contains(mx, my)) {
            togglePause();
            return;
        }

        int made = madeType();
        if (made != Sushi.NONE && PLATE_BOX.contains(mx, my)) {
            dragging = true;
            dragType = made;
            dragX = mx;
            dragY = my;
            return;
        }
        if (TRASH_BOX.contains(mx, my)) {
            if (!stationEmpty()) {
                clearStation();
                hint("TOSSED", mx, my);
            }
            return;
        }
        if (RICE_BOX.contains(mx, my)) {
            addRice(mx, my);
        } else if (SALMON_BOX.contains(mx, my)) {
            addTopping(Sushi.SALMON, mx, my);
        } else if (TUNA_BOX.contains(mx, my)) {
            addTopping(Sushi.TUNA, mx, my);
        } else if (TAMAGO_BOX.contains(mx, my)) {
            addTopping(Sushi.TAMAGO, mx, my);
        } else if (SEAWEED_BOX.contains(mx, my)) {
            addSeaweed(mx, my);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (dragging) {
            dragX = mouseX;
            dragY = mouseY;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        boolean clickable = false;
        if (gameState == MENU) {
            clickable = START_BTN.contains(mouseX, mouseY);
        } else if (gameState == MODE_SELECT) {
            clickable = BACK_BTN.contains(mouseX, mouseY);
            for (int i = 0; i < GameMode.ALL.length; i++) {
                if (modeCard(i).contains(mouseX, mouseY)) {
                    clickable = true;
                }
            }
        } else if (gameState == GAME_OVER) {
            clickable = AGAIN_BTN.contains(mouseX, mouseY) || CHANGE_MODE_BTN.contains(mouseX, mouseY);
        } else if (gameState == PLAYING) {
            clickable = hoveredStation() != null || PAUSE_BTN.contains(mouseX, mouseY);
        } else if (gameState == PAUSED) {
            clickable = PAUSE_BTN.contains(mouseX, mouseY) || RESTART_BTN.contains(mouseX, mouseY)
                    || HOME_BTN.contains(mouseX, mouseY);
        }
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging || gameState != PLAYING) {
            dragging = false;
            return;
        }
        int mx = e.getX();
        int my = e.getY();
        dragging = false;

        if (TRASH_BOX.contains(mx, my)) {
            clearStation();
            hint("TOSSED", mx, my);
            return;
        }

        Customer target = customerAt(mx, my);
        if (target != null) {
            tryServe(target, dragType);
        }
        dragType = Sushi.NONE;
    }

    private void tryServe(Customer customer, int sushiType) {
        if (sushiType == Sushi.NONE || customer == null || !customer.isWaiting()) {
            return;
        }
        int popX = customer.drawX() + Customer.DRAW_W / 2;
        int popY = customer.drawY() + 30;
        int result = customer.serve(sushiType, mode.getReorderChance());
        if (result != Customer.SERVE_WRONG) {
            combo++;
            served++;
            if (combo > bestCombo) {
                bestCombo = combo;
            }
            int bonus = (int) (customer.getPatienceRatio() * 50);
            int comboBonus = Math.max(0, (combo - 1) * 15);
            int multiplier = customer.getScoreMultiplier() * (isFever() ? 2 : 1);
            int gained = (100 + bonus + comboBonus) * multiplier;
            score += gained;
            lastServeCorrect = true;
            fx.burst(customer.drawX() + 70, customer.drawY() + 70, true);
            String pop = "+" + gained;
            if (multiplier > 1) {
                pop = pop + " (x" + multiplier + ")";
            }
            fx.popup(pop, popX, popY, true);
            if (result == Customer.SERVE_NEXT) {
                fx.popup("1 MORE TO GO!", popX, popY + 26, true);
            } else if (result == Customer.SERVE_MORE) {
                fx.popup("ONE MORE PLEASE!", popX, popY + 26, true);
            }
            checkComboRewards();
            checkLevelUp();
        } else {
            lastServeCorrect = false;
            flashWrong = 0.25;
            fx.burst(customer.drawX() + 70, customer.drawY() + 70, false);
            fx.popup("WRONG", popX, customer.drawY() + 40, false);
            loseLife();
        }
        feedbackTime = FEEDBACK_TIME;
        clearStation();
    }

    // รางวัลจากคอมโบ: Fever ทุก ๆ 5 คอมโบ และชีวิตเพิ่มทุก ๆ 10 คอมโบ
    private void checkComboRewards() {
        if (combo % FEVER_COMBO == 0) {
            feverTime = FEVER_DURATION;
            fx.confetti(400, 300);
            fx.popup("FEVER TIME!", 400, 330, true);
        }
        if (combo % LIFE_BONUS_COMBO == 0 && lives < START_LIVES) {
            lives++;
            fx.popup("+1 LIFE", 630, 100, true);
        }
    }

    private void checkLevelUp() {
        if (served % SERVES_PER_LEVEL == 0) {
            level++;
            levelBannerTime = LEVEL_BANNER_TIME;
            fx.confetti(200, 280);
            fx.confetti(600, 280);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        boolean confirm = key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        if (gameState == MENU) {
            if (confirm) {
                gameState = MODE_SELECT;
            }
        } else if (gameState == MODE_SELECT) {
            int index = key - KeyEvent.VK_1;
            if (index >= 0 && index < GameMode.ALL.length) {
                startGame(GameMode.ALL[index]);
            } else if (key == KeyEvent.VK_ESCAPE) {
                gameState = MENU;
            }
        } else if (gameState == GAME_OVER) {
            if (confirm) {
                startGame();
            } else if (key == KeyEvent.VK_ESCAPE) {
                gameState = MODE_SELECT;
            }
        } else if (key == KeyEvent.VK_P || key == KeyEvent.VK_ESCAPE) {
            togglePause();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
