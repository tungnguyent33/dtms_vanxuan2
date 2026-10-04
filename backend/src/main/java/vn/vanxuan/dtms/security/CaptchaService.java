package vn.vanxuan.dtms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.vanxuan.dtms.common.BusinessException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captcha anh tu sinh cho form cong khai (dang ky truc tuyen, tra cuu ho so) - khong phu thuoc dich vu ngoai.
 * Ma luu trong bo nho (trien khai mot may chu), dung mot lan, het han sau 5 phut.
 * Font DejaVu dong goi san trong jar nen chay duoc ca trong container khong cai font he thong.
 */
@Service
public class CaptchaService {

    public record Captcha(String captchaId, String anh) {
    }

    private record MaCho(String ma, Instant hetHan) {
    }

    private static final String KY_TU = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int DO_DAI = 5;
    private static final int TOI_DA_DANG_CHO = 20_000;
    private static final long GIAY_HET_HAN = 300;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, MaCho> dangCho = new ConcurrentHashMap<>();
    private final Font font;
    private final boolean bat;

    public CaptchaService(@Value("${app.captcha.bat:true}") boolean bat) throws IOException, FontFormatException {
        this.bat = bat;
        try (InputStream in = new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream()) {
            this.font = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(30f);
        }
    }

    public boolean dangBat() {
        return bat;
    }

    public Captcha tao() {
        Instant now = Instant.now();
        if (dangCho.size() >= TOI_DA_DANG_CHO) {
            dangCho.values().removeIf(m -> m.hetHan().isBefore(now));
            if (dangCho.size() >= TOI_DA_DANG_CHO) {
                throw new BusinessException("QUA_NHIEU_YEU_CAU", "Hệ thống đang bận, vui lòng thử lại sau",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DO_DAI; i++) sb.append(KY_TU.charAt(RANDOM.nextInt(KY_TU.length())));
        String id = UUID.randomUUID().toString();
        dangCho.put(id, new MaCho(sb.toString(), now.plusSeconds(GIAY_HET_HAN)));
        return new Captcha(id, "data:image/png;base64," + Base64.getEncoder().encodeToString(ve(sb.toString())));
    }

    /** Dung mot lan: kiem tra xong la xoa, du dung hay sai (chong do thu nhieu lan cung mot ma). */
    public void kiemTra(String captchaId, String traLoi) {
        if (!bat) return;
        MaCho m = captchaId == null ? null : dangCho.remove(captchaId);
        if (m == null || m.hetHan().isBefore(Instant.now()) || traLoi == null
                || !m.ma().equalsIgnoreCase(traLoi.trim())) {
            throw new BusinessException("CAPTCHA_SAI", "Mã xác nhận không đúng hoặc đã hết hạn, vui lòng nhập lại");
        }
    }

    private byte[] ve(String ma) {
        int w = 170, h = 56;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(244, 247, 252));
            g.fillRect(0, 0, w, h);
            // Nhieu nen: cham va duong cong
            for (int i = 0; i < 120; i++) {
                g.setColor(mau(150, 220));
                g.fillOval(RANDOM.nextInt(w), RANDOM.nextInt(h), 2, 2);
            }
            g.setStroke(new BasicStroke(1.4f));
            for (int i = 0; i < 6; i++) {
                g.setColor(mau(120, 200));
                g.drawLine(RANDOM.nextInt(w), RANDOM.nextInt(h), RANDOM.nextInt(w), RANDOM.nextInt(h));
            }
            g.setFont(font);
            for (int i = 0; i < ma.length(); i++) {
                AffineTransform cu = g.getTransform();
                int x = 14 + i * 30 + RANDOM.nextInt(5);
                int y = 38 + RANDOM.nextInt(8);
                g.rotate((RANDOM.nextDouble() - 0.5) * 0.6, x + 10, y - 10);
                g.setColor(mau(20, 110));
                g.drawString(String.valueOf(ma.charAt(i)), x, y);
                g.setTransform(cu);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được ảnh captcha", e);
        } finally {
            g.dispose();
        }
    }

    private static Color mau(int min, int max) {
        return new Color(min + RANDOM.nextInt(max - min), min + RANDOM.nextInt(max - min), min + RANDOM.nextInt(max - min));
    }
}
