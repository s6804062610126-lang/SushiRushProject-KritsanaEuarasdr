import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

// เก็บคะแนนสูงสุดลงไฟล์ เพื่อให้ปิดเกมแล้วเปิดใหม่ยังจำได้
public class HighScore {
    private final File file;
    private int best;

    public HighScore(String path) {
        this.file = new File(path);
        this.best = load();
    }

    public int getBest() {
        return best;
    }

    // คืน true ถ้าคะแนนนี้ทำลายสถิติเดิม
    public boolean submit(int score) {
        if (score <= best) {
            return false;
        }
        best = score;
        save();
        return true;
    }

    private int load() {
        if (!file.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line == null) {
                return 0;
            }
            return Math.max(0, Integer.parseInt(line.trim()));
        } catch (IOException | NumberFormatException e) {
            System.out.println("อ่านไฟล์คะแนนสูงสุดไม่ได้ เริ่มนับใหม่จาก 0");
            return 0;
        }
    }

    private void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println(best);
        } catch (IOException e) {
            System.out.println("บันทึกคะแนนสูงสุดไม่สำเร็จ");
        }
    }
}
