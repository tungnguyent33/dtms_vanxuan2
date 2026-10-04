package vn.vanxuan.dtms.module.thongbao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.CauHinhService;
import vn.vanxuan.dtms.module.ctv.HoaHongService;
import vn.vanxuan.dtms.common.NgayLamViec;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Locale;

/**
 * Tac vu dinh ky (FR-16, BR-12) - chay luc 7h sang moi ngay, an toan khi chay lai nhieu lan
 * nho ma su kien (moi su kien chi gui mot lan cho moi nguoi nhan).
 */
@Service
public class NhacViecService {

    public record KetQuaNhacViec(int nhacLich, int nhacNo, int baoCaoSo, int xeBaoDuong, int hoSoChoDuyet,
                                 int chiHoaHong) {
        public int tong() {
            return nhacLich + nhacNo + baoCaoSo + xeBaoDuong + hoSoChoDuyet + chiHoaHong;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(NhacViecService.class);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> QUAN_TRI = List.of(VaiTro.ADMIN);
    private static final List<String> VAN_PHONG = List.of(VaiTro.ADMIN, VaiTro.LE_TAN);
    /** Nhac no truoc ngay be giang bao nhieu ngay. */
    private static final int NGAY_NHAC_NO = 3;

    private final NamedParameterJdbcTemplate jdbc;
    private final ThongBaoService thongBao;
    private final CauHinhService cauHinh;
    private final HoaHongService hoaHong;

    public NhacViecService(NamedParameterJdbcTemplate jdbc, ThongBaoService thongBao, CauHinhService cauHinh,
                           HoaHongService hoaHong) {
        this.jdbc = jdbc;
        this.thongBao = thongBao;
        this.cauHinh = cauHinh;
        this.hoaHong = hoaHong;
    }

    @Scheduled(cron = "${app.nhac-viec.cron:0 0 7 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void chayDinhKy() {
        KetQuaNhacViec kq = chay();
        log.info("Nhac viec: tao {} thong bao {}", kq.tong(), kq);
    }

    @Transactional
    public KetQuaNhacViec chay() {
        LocalDate homNay = LocalDate.now();
        hoaHong.doiSoatTatCa();   // dam bao so lieu hoa hong dung truoc khi nhac chi
        return new KetQuaNhacViec(nhacLichNgayMai(homNay), nhacNo(homNay), nhacBaoCaoSo(homNay),
                nhacBaoDuongXe(homNay), nhacHoSoChoDuyet(homNay), nhacChiHoaHong(homNay));
    }

    /** Nhac giao vien va hoc vien (co tai khoan) ve buoi hoc ngay mai. Buoi LT chi gom hoc vien TAP_TRUNG (BR-05). */
    int nhacLichNgayMai(LocalDate homNay) {
        LocalDate mai = homNay.plusDays(1);
        var buoi = jdbc.queryForList("""
                SELECT b.id, b.khoa_id, b.loai, b.gio_bat_dau, b.gio_ket_thuc, b.dia_diem, k.ma_khoa,
                       gv.nguoi_dung_id AS gv_nd
                FROM buoi_hoc b
                JOIN khoa_dao_tao k ON k.id = b.khoa_id
                JOIN giao_vien gv ON gv.id = b.giao_vien_id
                WHERE b.ngay = :mai AND b.trang_thai = 'KE_HOACH'
                """, new MapSqlParameterSource("mai", mai));
        int n = 0;
        for (var b : buoi) {
            String loai = "LY_THUYET".equals(b.get("loai")) ? "lý thuyết" : "thực hành";
            String gio = gio(b.get("gio_bat_dau")) + "–" + gio(b.get("gio_ket_thuc"));
            String su = "LICH-" + b.get("id");
            String noiDung = "Buổi " + loai + " khóa " + b.get("ma_khoa") + ", ngày " + mai.format(NGAY) + " lúc "
                    + gio + " tại " + b.get("dia_diem") + ".";
            if (thongBao.gui(soLong(b.get("gv_nd")), ThongBaoService.LICH_HOC, "Lịch dạy ngày mai " + gio, noiDung,
                    "/diem-danh", su)) n++;
            List<Long> hocVien = jdbc.queryForList("""
                    SELECT hv.nguoi_dung_id FROM dang_ky dk JOIN hoc_vien hv ON hv.id = dk.hoc_vien_id
                    WHERE dk.khoa_id = :khoa AND dk.trang_thai IN ('DA_TIEP_NHAN','DANG_HOC','CHUA_DAT')
                      AND hv.nguoi_dung_id IS NOT NULL
                      AND (:loai = 'THUC_HANH' OR dk.hinh_thuc_ly_thuyet = 'TAP_TRUNG')
                    """, new MapSqlParameterSource("khoa", b.get("khoa_id")).addValue("loai", b.get("loai")), Long.class);
            for (Long nd : hocVien) {
                if (thongBao.gui(nd, ThongBaoService.LICH_HOC, "Lịch học ngày mai " + gio, noiDung, "/hoc-tap", su)) n++;
            }
        }
        return n;
    }

    /**
     * Nhac no: hoc vien con no o khoa sap / dang dao tao (moi tuan mot lan);
     * van phong nhan ban tong hop cac ho so con no o khoa sap be giang (moi ngay mot lan).
     */
    int nhacNo(LocalDate homNay) {
        var p = new MapSqlParameterSource("han", homNay.plusDays(NGAY_NHAC_NO));
        var ds = jdbc.queryForList("""
                SELECT v.dang_ky_id, v.ma_khoa, v.con_no, hv.nguoi_dung_id, k.ngay_be_giang
                FROM v_cong_no v
                JOIN dang_ky dk ON dk.id = v.dang_ky_id
                JOIN hoc_vien hv ON hv.id = dk.hoc_vien_id
                JOIN khoa_dao_tao k ON k.id = v.khoa_id
                WHERE v.con_no > 0 AND dk.trang_thai IN ('DA_TIEP_NHAN','DANG_HOC')
                  AND k.trang_thai IN ('DANG_TUYEN','DANG_DAO_TAO') AND k.ngay_khai_giang <= :han
                """, p);
        String tuan = homNay.getYear() + "W" + homNay.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int n = 0;
        int sapBeGiang = 0;
        BigDecimal tongNo = BigDecimal.ZERO;
        for (var r : ds) {
            BigDecimal conNo = (BigDecimal) r.get("con_no");
            if (thongBao.gui(soLong(r.get("nguoi_dung_id")), ThongBaoService.NHAC_NO, "Nhắc học phí còn nợ",
                    "Bạn còn nợ học phí " + tien(conNo) + " khóa " + r.get("ma_khoa")
                            + ". Vui lòng hoàn thành trước ngày bế giảng để được xét hoàn thành khóa.",
                    "/hoc-tap", "NO-" + r.get("dang_ky_id") + "-" + tuan)) n++;
            LocalDate beGiang = ngay(r.get("ngay_be_giang"));
            if (beGiang != null && !beGiang.isAfter(homNay.plusDays(NGAY_NHAC_NO))) {
                sapBeGiang++;
                tongNo = tongNo.add(conNo);
            }
        }
        if (sapBeGiang > 0) {
            n += thongBao.guiTheoVaiTro(VAN_PHONG, ThongBaoService.NHAC_NO, sapBeGiang + " hồ sơ còn nợ ở khóa sắp bế giảng",
                    "Tổng còn nợ " + tien(tongNo) + ". Học viên còn nợ sẽ không được xét hoàn thành khóa (BR-09).",
                    "/cong-no", "NO-TONG-" + homNay);
        }
        return n;
    }

    /** BR-12: nhac lap bao cao dang ky khoa vao ngay khai giang; nhac gui danh sach hoan thanh trong 02 ngay lam viec. */
    int nhacBaoCaoSo(LocalDate homNay) {
        int n = 0;
        var khaiGiang = jdbc.queryForList("""
                SELECT id, ma_khoa, ngay_khai_giang FROM khoa_dao_tao
                WHERE trang_thai = 'DANG_TUYEN' AND ngay_khai_giang <= :homNay
                """, new MapSqlParameterSource("homNay", homNay));
        for (var k : khaiGiang) {
            n += thongBao.guiTheoVaiTro(QUAN_TRI, ThongBaoService.BAO_CAO_SO, "Khóa " + k.get("ma_khoa") + " đến ngày khai giảng",
                    "Chuyển khóa sang \"Đang đào tạo\" và lập báo cáo đăng ký khóa đào tạo gửi Sở Xây dựng (BR-12).",
                    "/khoa", "BC-DK-" + k.get("id"));
        }
        var hoanThanh = jdbc.queryForList("""
                SELECT k.id, k.ma_khoa, dk.ngay_hoan_thanh, COUNT(*) AS so
                FROM dang_ky dk JOIN khoa_dao_tao k ON k.id = dk.khoa_id
                WHERE dk.ngay_hoan_thanh >= :tu AND dk.trang_thai = 'HOAN_THANH'
                GROUP BY k.id, k.ma_khoa, dk.ngay_hoan_thanh
                """, new MapSqlParameterSource("tu", homNay.minusDays(7)));
        for (var k : hoanThanh) {
            LocalDate ngayCap = ngay(k.get("ngay_hoan_thanh"));
            LocalDate han = NgayLamViec.cong(ngayCap, 2);
            if (homNay.isAfter(han)) continue;
            // Ngay cap da co thong bao (XetHoanThanhService); den ngay het han nhac lai mot lan nua
            String su = "BC-HT-" + k.get("id") + "-" + ngayCap + (homNay.equals(han) ? "-han" : "");
            n += thongBao.guiTheoVaiTro(QUAN_TRI, ThongBaoService.BAO_CAO_SO,
                    (homNay.equals(han) ? "Hôm nay hết hạn: " : "") + "Gửi danh sách hoàn thành khóa " + k.get("ma_khoa"),
                    k.get("so") + " học viên được cấp giấy xác nhận ngày " + ngayCap.format(NGAY)
                            + ". Hạn gửi danh sách về Sở Xây dựng: " + han.format(NGAY) + " (02 ngày làm việc).",
                    "/khoa", su);
        }
        return n;
    }

    int nhacBaoDuongXe(LocalDate homNay) {
        int n = 0;
        var xe = jdbc.queryForList("""
                SELECT id, bien_so, ngay_bao_duong_tiep FROM xe_tap_lai
                WHERE trang_thai = 'SAN_SANG' AND ngay_bao_duong_tiep <= :han
                """, new MapSqlParameterSource("han", homNay.plusDays(3)));
        for (var x : xe) {
            LocalDate d = ngay(x.get("ngay_bao_duong_tiep"));
            n += thongBao.guiTheoVaiTro(QUAN_TRI, ThongBaoService.XE, "Xe " + x.get("bien_so") + " đến hạn bảo dưỡng",
                    "Ngày bảo dưỡng: " + d.format(NGAY) + ". Cập nhật trạng thái xe để không xếp lịch tập.",
                    "/quan-tri?tab=xe", "XE-" + x.get("id") + "-" + d);
        }
        return n;
    }

    int nhacHoSoChoDuyet(LocalDate homNay) {
        Long so = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dang_ky WHERE trang_thai = 'CHO_DUYET' AND ngay_dang_ky < :moc
                """, new MapSqlParameterSource("moc", homNay.atStartOfDay()), Long.class);
        if (so == null || so == 0) return 0;
        return thongBao.guiTheoVaiTro(VAN_PHONG, ThongBaoService.HO_SO, so + " hồ sơ đăng ký trực tuyến đang chờ duyệt",
                "Gọi điện xác nhận với học viên và duyệt hồ sơ để giữ chỗ trong khóa.", "/hoc-vien",
                "CHO-DUYET-" + homNay);
    }

    /** Ngay chi hoa hong hang thang (mac dinh ngay 5): nhac quan tri vien chot ky thang truoc. */
    int nhacChiHoaHong(LocalDate homNay) {
        if (homNay.getDayOfMonth() != cauHinh.soNguyen(CauHinhService.HOA_HONG_NGAY_CHI, 5)) return 0;
        String ky = homNay.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        var r = jdbc.queryForMap("""
                SELECT COUNT(DISTINCT ctv_id) AS so_ctv, COALESCE(SUM(so_tien), 0) AS tong FROM hoa_hong
                WHERE trang_thai IN ('DU_DIEU_KIEN','DA_DUYET') AND ky <= :ky
                """, new MapSqlParameterSource("ky", ky));
        long soCtv = ((Number) r.get("so_ctv")).longValue();
        if (soCtv == 0) return 0;
        return thongBao.guiTheoVaiTro(QUAN_TRI, ThongBaoService.HOC_PHI, "Đến ngày chi hoa hồng kỳ " + ky,
                soCtv + " CTV, tổng " + tien((BigDecimal) r.get("tong")) + ". Duyệt các khoản đủ điều kiện rồi ghi nhận chi.",
                "/ctv?tab=hoa-hong", "CHI-HH-" + ky);
    }

    private static Long soLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static LocalDate ngay(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDate d) return d;
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private static String gio(Object o) {
        String s = String.valueOf(o);
        return s.length() >= 5 ? s.substring(0, 5) : s;
    }

    static String tien(BigDecimal v) {
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(v) + " đ";
    }
}
