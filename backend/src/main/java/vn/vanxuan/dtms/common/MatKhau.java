package vn.vanxuan.dtms.common;

import java.security.SecureRandom;

/** Sinh mat khau tam va kiem tra do manh mat khau (FR-01). */
public final class MatKhau {
    // Bo ky tu de doc qua dien thoai: bo 0/O, 1/l/I
    private static final String CHU = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String SO = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private MatKhau() {
    }

    /** Mat khau tam 10 ky tu, luon co ca chu va so. */
    public static String taoTam() {
        char[] a = new char[10];
        a[0] = CHU.charAt(RANDOM.nextInt(CHU.length()));
        a[1] = SO.charAt(RANDOM.nextInt(SO.length()));
        String tatCa = CHU + SO;
        for (int i = 2; i < a.length; i++) {
            a[i] = tatCa.charAt(RANDOM.nextInt(tatCa.length()));
        }
        // Xao tron de chu/so bat buoc khong luon nam o dau
        for (int i = a.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
        return new String(a);
    }

    /** Toi thieu 8 ky tu, co chu va so. Nem BusinessException neu khong dat. */
    public static void kiemTraDoManh(String mk) {
        if (mk == null || mk.length() < 8 || !mk.matches(".*[A-Za-z].*") || !mk.matches(".*[0-9].*")) {
            throw new BusinessException("MAT_KHAU_YEU", "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số");
        }
    }
}
