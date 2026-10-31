import java.awt.Color;

// ค่าตั้งของแต่ละโหมดเกม และคะแนนสูงสุดแยกของโหมดนั้น
public class GameMode {
    // ลำดับค่า: ชื่อ, คำอธิบาย, เวลา(วินาที), ความเร็วเดิน, ความเร็วเดินที่เพิ่มต่อเลเวล,
    // ความเร็วลดความอดทน, ความเร็วลดความอดทนที่เพิ่มต่อเลเวล, โอกาสสั่ง 2 จาน, โอกาสสั่งเพิ่ม
    public static final GameMode INFINITE = new GameMode("INFINITE", "Gets faster every level",
            0, 1.0, 0.08, 1.0, 0.05, 0.15, 0.30, "highscore.txt", new Color(120, 200, 255));
    public static final GameMode EASY = new GameMode("EASY", "Slow, patient customers",
            180, 0.7, 0, 0.6, 0, 0.15, 0.35, "highscore_easy.txt", new Color(130, 225, 120));
    public static final GameMode MEDIUM = new GameMode("MEDIUM", "Normal customers",
            120, 1.0, 0, 1.2, 0, 0.25, 0.30, "highscore_medium.txt", new Color(255, 205, 70));
    public static final GameMode HARD = new GameMode("HARD", "Fast, impatient customers",
            60, 1.45, 0, 2.0, 0, 0.35, 0.25, "highscore_hard.txt", new Color(255, 110, 100));

    // เพดานของการเพิ่มตามเลเวล (เช่น 2.0 = เร็วขึ้นได้สูงสุด 2 เท่าของค่าเริ่มต้น)
    static final double MAX_WALK_BOOST = 2.0;
    static final double MAX_DRAIN_BOOST = 1.6;

    public static final GameMode[] ALL = {INFINITE, EASY, MEDIUM, HARD};

    private final String name;
    private final String description;
    private final int timeLimit;
    private final double walkSpeed;
    private final double walkSpeedPerLevel;
    private final double patienceDrain;
    private final double patienceDrainPerLevel;
    private final double doubleOrderChance;
    private final double reorderChance;
    private final HighScore highScore;
    private final Color color;

    public GameMode(String name, String description, int timeLimit,
            double walkSpeed, double walkSpeedPerLevel,
            double patienceDrain, double patienceDrainPerLevel,
            double doubleOrderChance, double reorderChance,
            String scoreFile, Color color) {
        this.name = name;
        this.description = description;
        this.timeLimit = timeLimit;
        this.walkSpeed = walkSpeed;
        this.walkSpeedPerLevel = walkSpeedPerLevel;
        this.patienceDrain = patienceDrain;
        this.patienceDrainPerLevel = patienceDrainPerLevel;
        this.doubleOrderChance = doubleOrderChance;
        this.reorderChance = reorderChance;
        this.highScore = new HighScore(scoreFile);
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // เวลาของโหมด (วินาที) ถ้าเป็น 0 แปลว่าไม่จำกัดเวลา
    public int getTimeLimit() {
        return timeLimit;
    }

    public boolean isTimed() {
        return timeLimit > 0;
    }

    // ความเร็วเดินของลูกค้าที่เลเวลนี้ (1.0 = ปกติ)
    public double getWalkSpeed(int level) {
        double boost = Math.min(MAX_WALK_BOOST, 1 + (level - 1) * walkSpeedPerLevel);
        return walkSpeed * boost;
    }

    // หลอดความอดทนลดลงเร็วแค่ไหนที่เลเวลนี้ (1.0 = ลด 1 วินาทีต่อวินาที)
    public double getPatienceDrain(int level) {
        double boost = Math.min(MAX_DRAIN_BOOST, 1 + (level - 1) * patienceDrainPerLevel);
        return patienceDrain * boost;
    }

    // โหมดนี้ลูกค้าเร็วขึ้นตามเลเวลหรือไม่
    public boolean speedsUpWithLevel() {
        return walkSpeedPerLevel > 0;
    }

    // โอกาสที่ลูกค้าจะสั่ง 2 จานตั้งแต่แรก
    public double getDoubleOrderChance() {
        return doubleOrderChance;
    }

    // โอกาสที่ลูกค้าจะอยู่ต่อแล้วสั่งเพิ่มอีกจาน
    public double getReorderChance() {
        return reorderChance;
    }

    public HighScore getHighScore() {
        return highScore;
    }

    public Color getColor() {
        return color;
    }

    public String getTimeLabel() {
        return isTimed() ? formatTime(timeLimit) : "ENDLESS";
    }

    // แปลงวินาทีเป็นรูปแบบ m:ss เช่น 125 -> 2:05
    public static String formatTime(double seconds) {
        int total = (int) Math.ceil(Math.max(0, seconds));
        int min = total / 60;
        int sec = total % 60;
        return min + ":" + (sec < 10 ? "0" : "") + sec;
    }
}
