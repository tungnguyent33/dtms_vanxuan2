package vn.vanxuan.dtms.module.baocao;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vanxuan.dtms.common.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bao cao dung SQL truc tiep (NamedParameterJdbcTemplate) vi la truy van tong hop, khong can entity.
 */
@RestController
@RequestMapping("/api/bao-cao")
@PreAuthorize("hasRole('ADMIN')")
public class BaoCaoController {

    public record TongQuan(long hocVienDangHoc, long hoSoChoDuyet, long khoaDangTuyen, long hoanThanhTrongNam,
                           BigDecimal doanhThuThangNay, BigDecimal tongCongNo) {
    }

    public record DongDoanhThu(String ky, String hang, long soPhieu, BigDecimal doanhThu) {
    }

    public enum Nhom { NGAY, THANG }

    private final NamedParameterJdbcTemplate jdbc;

    public BaoCaoController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/tong-quan")
    public TongQuan tongQuan() {
        LocalDate dauThang = LocalDate.now().withDayOfMonth(1);
        var p = new MapSqlParameterSource()
                .addValue("tu", dauThang)
                .addValue("den", dauThang.plusMonths(1))
                .addValue("nam", LocalDate.now().getYear());
        return new TongQuan(
                dem("SELECT COUNT(*) FROM dang_ky WHERE trang_thai IN ('DA_TIEP_NHAN','DANG_HOC')", p),
                dem("SELECT COUNT(*) FROM dang_ky WHERE trang_thai = 'CHO_DUYET'", p),
                dem("SELECT COUNT(*) FROM khoa_dao_tao WHERE trang_thai = 'DANG_TUYEN'", p),
                dem("SELECT COUNT(*) FROM dang_ky WHERE ngay_hoan_thanh IS NOT NULL AND YEAR(ngay_hoan_thanh) = :nam", p),
                tien("SELECT COALESCE(SUM(so_tien),0) FROM phieu_thu WHERE trang_thai = 'HIEU_LUC' "
                        + "AND ngay_thu >= :tu AND ngay_thu < :den", p),
                tien("SELECT COALESCE(SUM(con_no),0) FROM v_cong_no WHERE con_no > 0", p));
    }

    @GetMapping("/doanh-thu")
    public List<DongDoanhThu> doanhThu(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
                                       @RequestParam(defaultValue = "THANG") Nhom nhom) {
        if (den.isBefore(tu)) throw new BusinessException("KHOANG_NGAY_SAI", "Ngày kết thúc phải sau ngày bắt đầu");
        // Bieu thuc nhom lay tu enum (khong ghep chuoi tu nguoi dung) -> an toan SQL injection
        String ky = nhom == Nhom.NGAY ? "DATE_FORMAT(pt.ngay_thu, '%Y-%m-%d')" : "DATE_FORMAT(pt.ngay_thu, '%Y-%m')";
        String sql = "SELECT " + ky + " AS ky, k.hang_ma AS hang, COUNT(*) AS so_phieu, SUM(pt.so_tien) AS doanh_thu "
                + "FROM phieu_thu pt JOIN dang_ky dk ON dk.id = pt.dang_ky_id JOIN khoa_dao_tao k ON k.id = dk.khoa_id "
                + "WHERE pt.trang_thai = 'HIEU_LUC' AND pt.ngay_thu >= :tu AND pt.ngay_thu < :den "
                + "GROUP BY " + ky + ", k.hang_ma ORDER BY ky, hang";
        var p = new MapSqlParameterSource().addValue("tu", tu).addValue("den", den.plusDays(1));
        return jdbc.query(sql, p, (rs, i) -> new DongDoanhThu(rs.getString("ky"), rs.getString("hang"),
                rs.getLong("so_phieu"), rs.getBigDecimal("doanh_thu")));
    }

    private long dem(String sql, MapSqlParameterSource p) {
        Long v = jdbc.queryForObject(sql, p, Long.class);
        return v == null ? 0 : v;
    }

    private BigDecimal tien(String sql, MapSqlParameterSource p) {
        BigDecimal v = jdbc.queryForObject(sql, p, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
