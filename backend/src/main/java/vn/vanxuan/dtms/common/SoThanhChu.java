package vn.vanxuan.dtms.common;

/**
 * Doc so tien thanh chu tieng Viet de in tren phieu thu.
 * VD: 1500000 -> "Một triệu năm trăm nghìn đồng"
 */
public final class SoThanhChu {
    private static final String[] SO = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    private static final String[] DON_VI = {"", " nghìn", " triệu", " tỷ", " nghìn tỷ", " triệu tỷ"};

    private SoThanhChu() {
    }

    public static String doc(long so) {
        if (so == 0) return "Không đồng";
        if (so < 0) return "Âm " + doc(-so).toLowerCase();
        StringBuilder kq = new StringBuilder();
        // Tach thanh cac nhom 3 chu so tu phai sang trai
        long[] cacNhom = new long[7];
        int n = 0;
        while (so > 0) {
            cacNhom[n++] = so % 1000;
            so /= 1000;
        }
        boolean coNhomCaoHon = false;
        for (int i = n - 1; i >= 0; i--) {
            int ba = (int) cacNhom[i];
            if (ba == 0) continue;               // bo qua nhom 000 (VD: 1.000.500 -> "một triệu năm trăm")
            if (kq.length() > 0) kq.append(' ');
            kq.append(docBaSo(ba, coNhomCaoHon)).append(DON_VI[i]);
            coNhomCaoHon = true;
        }
        String s = kq.toString().trim();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1) + " đồng";
    }

    /** Doc 3 chu so. docDayDu = true khi phia truoc da co nhom lon hon (can doc "không trăm", "lẻ"). */
    static String docBaSo(int so, boolean docDayDu) {
        int tram = so / 100, chuc = (so / 10) % 10, dv = so % 10;
        StringBuilder sb = new StringBuilder();
        if (tram > 0 || docDayDu) {
            sb.append(SO[tram]).append(" trăm");
            if (chuc == 0 && dv > 0) sb.append(" lẻ");
        }
        if (chuc > 1) {
            sb.append(' ').append(SO[chuc]).append(" mươi");
        } else if (chuc == 1) {
            sb.append(" mười");
        }
        if (dv > 0) {
            String donVi;
            if (dv == 1 && chuc > 1) donVi = "mốt";
            else if (dv == 5 && chuc > 0) donVi = "lăm";
            else if (dv == 4 && chuc > 1) donVi = "tư";
            else donVi = SO[dv];
            sb.append(' ').append(donVi);
        }
        return sb.toString().trim();
    }
}
