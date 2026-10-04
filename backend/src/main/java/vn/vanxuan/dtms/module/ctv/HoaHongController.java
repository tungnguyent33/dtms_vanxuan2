package vn.vanxuan.dtms.module.ctv;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.CauHinhService;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/**
 * Hoa hong, chinh sach, chi tra va bao cao CTV - chi ADMIN (le tan khong doi ty le, khong duyet chi).
 * Ngoai le: le tan / admin xem hoa hong cua mot ho so (/dang-ky/{id}).
 */
@RestController
@RequestMapping("/api/hoa-hong")
@PreAuthorize("hasRole('ADMIN')")
public class HoaHongController {

    private final HoaHongService service;
    private final HoaHongRepository repo;
    private final CauHinhService cauHinh;
    private final NhatKyService nhatKy;
    private final NamedParameterJdbcTemplate jdbc;

    public HoaHongController(HoaHongService service, HoaHongRepository repo, CauHinhService cauHinh,
                             NhatKyService nhatKy, NamedParameterJdbcTemplate jdbc) {
        this.service = service;
        this.repo = repo;
        this.cauHinh = cauHinh;
        this.nhatKy = nhatKy;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<HoaHongResponse> ds(@RequestParam(required = false) Long ctvId,
                                    @RequestParam(required = false) String ky,
                                    @RequestParam(required = false) HoaHong.TrangThai trangThai) {
        return service.ds(ctvId, ky == null || ky.isBlank() ? null : ky, trangThai);
    }

    @GetMapping("/dang-ky/{dangKyId}")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    @Transactional(readOnly = true)
    public HoaHongResponse cuaDangKy(@PathVariable Long dangKyId) {
        return repo.findByDangKyId(dangKyId).map(service::response)
                .orElseThrow(() -> new BusinessException("KHONG_CO_HOA_HONG", "Hồ sơ không có hoa hồng CTV", HttpStatus.NOT_FOUND));
    }

    @GetMapping("/tong-hop")
    public List<TongHopKy> tongHop(@RequestParam String ky) {
        return service.tongHop(ky);
    }

    @PostMapping("/duyet")
    public Map<String, Integer> duyet(@Valid @RequestBody DuyetHoaHongRequest req, @AuthenticationPrincipal AuthUser admin) {
        return Map.of("soDaDuyet", service.duyet(req.ids(), admin));
    }

    @PostMapping("/chi")
    @ResponseStatus(HttpStatus.CREATED)
    public ChiHoaHongResponse chi(@Valid @RequestBody ChiHoaHongRequest req, @AuthenticationPrincipal AuthUser admin) {
        return service.chi(req, admin);
    }

    @GetMapping("/phieu-chi")
    public List<ChiHoaHongResponse> phieuChi(@RequestParam(required = false) Long ctvId) {
        return service.dsPhieuChi(ctvId);
    }

    // ------------------------------------------------------------------ chinh sach & cau hinh
    @GetMapping("/chinh-sach")
    public List<ChinhSachResponse> chinhSach() {
        return service.dsChinhSach();
    }

    @PostMapping("/chinh-sach")
    @ResponseStatus(HttpStatus.CREATED)
    public ChinhSachResponse themChinhSach(@Valid @RequestBody ChinhSachRequest req, @AuthenticationPrincipal AuthUser admin) {
        return service.themChinhSach(req, admin);
    }

    @DeleteMapping("/chinh-sach/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void xoaChinhSach(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        service.xoaChinhSach(id, admin);
    }

    @GetMapping("/cau-hinh")
    public CauHinhHoaHong cauHinh() {
        return new CauHinhHoaHong(cauHinh.soNguyen(CauHinhService.HOA_HONG_PHAN_TRAM_DONG, 100),
                cauHinh.soNguyen(CauHinhService.HOA_HONG_NGAY_CHI, 5));
    }

    @PutMapping("/cau-hinh")
    public CauHinhHoaHong luuCauHinh(@Valid @RequestBody CauHinhHoaHong req, @AuthenticationPrincipal AuthUser admin) {
        nhatKy.ghi(admin.id(), "SUA_CAU_HINH_HOA_HONG", "cau_hinh", null, Map.of("cu", cauHinh(), "moi", req));
        cauHinh.dat(CauHinhService.HOA_HONG_PHAN_TRAM_DONG, req.phanTramDongToiThieu().toString());
        cauHinh.dat(CauHinhService.HOA_HONG_NGAY_CHI, req.ngayChi().toString());
        service.doiSoatTatCa();   // nguong % moi ap dung ngay cho cac khoan chua duyet
        return req;
    }

    // ------------------------------------------------------------------ bao cao
    /** So lead, ty le chot, doanh thu, hoa hong phat sinh / da chi / con phai tra theo tung CTV. */
    @GetMapping("/bao-cao")
    public List<BaoCaoCtv> baoCao(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den) {
        if (den.isBefore(tu)) throw new BusinessException("KHOANG_NGAY_SAI", "Ngày kết thúc phải sau ngày bắt đầu");
        var p = new MapSqlParameterSource("tu", tu).addValue("den", den.plusDays(1));
        return jdbc.query("""
                SELECT c.id, c.ho_ten, c.hang, c.trang_thai,
                  (SELECT COUNT(*) FROM lead_khach l WHERE l.ctv_id = c.id AND l.created_at >= :tu AND l.created_at < :den) AS so_lead,
                  (SELECT COUNT(*) FROM lead_khach l WHERE l.ctv_id = c.id AND l.trang_thai = 'DA_CHOT'
                     AND l.created_at >= :tu AND l.created_at < :den) AS so_chot,
                  (SELECT COUNT(*) FROM dang_ky d WHERE d.ctv_id = c.id AND d.trang_thai <> 'DA_HUY'
                     AND d.ngay_dang_ky >= :tu AND d.ngay_dang_ky < :den) AS so_hoc_vien,
                  (SELECT COALESCE(SUM(pt.so_tien), 0) FROM phieu_thu pt JOIN dang_ky d ON d.id = pt.dang_ky_id
                     WHERE d.ctv_id = c.id AND pt.trang_thai = 'HIEU_LUC' AND pt.ngay_thu >= :tu AND pt.ngay_thu < :den) AS doanh_thu,
                  (SELECT COALESCE(SUM(h.so_tien), 0) FROM hoa_hong h JOIN dang_ky d ON d.id = h.dang_ky_id
                     WHERE h.ctv_id = c.id AND h.trang_thai <> 'HUY' AND d.ngay_dang_ky >= :tu AND d.ngay_dang_ky < :den) AS phat_sinh,
                  (SELECT COALESCE(SUM(ch.so_tien), 0) FROM chi_hoa_hong ch
                     WHERE ch.ctv_id = c.id AND ch.ngay_chi >= :tu AND ch.ngay_chi < :den) AS da_chi,
                  (SELECT COALESCE(SUM(h.so_tien), 0) FROM hoa_hong h
                     WHERE h.ctv_id = c.id AND h.trang_thai IN ('DU_DIEU_KIEN','DA_DUYET')) AS con_phai_tra
                FROM cong_tac_vien c
                WHERE c.trang_thai <> 'CHO_DUYET'
                ORDER BY doanh_thu DESC, c.ho_ten
                """, p, (rs, i) -> {
            long soLead = rs.getLong("so_lead");
            long soChot = rs.getLong("so_chot");
            return new BaoCaoCtv(rs.getLong("id"), rs.getString("ho_ten"), rs.getString("hang"), rs.getString("trang_thai"),
                    soLead, soChot, soLead == 0 ? 0 : Math.round(soChot * 1000.0 / soLead) / 10.0,
                    rs.getLong("so_hoc_vien"), so(rs.getBigDecimal("doanh_thu")), so(rs.getBigDecimal("phat_sinh")),
                    so(rs.getBigDecimal("da_chi")), so(rs.getBigDecimal("con_phai_tra")));
        });
    }

    private static BigDecimal so(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
