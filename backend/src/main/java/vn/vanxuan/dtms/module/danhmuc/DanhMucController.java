package vn.vanxuan.dtms.module.danhmuc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.lichhoc.BuoiHoc;
import vn.vanxuan.dtms.module.lichhoc.BuoiHocRepository;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService.KetQuaCapMatKhau;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API danh muc (FR-02, FR-03): hang GPLX, giao vien, xe tap lai. Cong tac vien: module ctv (CtvController).
 * LE_TAN chi xem danh sach dang hoat dong; ADMIN xem tat ca (?tatCa=true) va duoc them / sua / ngung.
 * Khong xoa cung: ngung dung de giu lich su lich hoc, ho so.
 */
@RestController
@RequestMapping("/api")
public class DanhMucController {

    private static final String SDT = "^0[0-9]{9}$";
    private static final String LOI_SDT = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0";

    // ------------------------------------------------------------------ DTO
    public record HangGplxDto(String ma, String ten, BigDecimal gioLyThuyet, BigDecimal gioThucHanh,
                              Integer tuoiToiThieu, Integer soNgayKhoaToiDa, BigDecimal hocPhiMacDinh,
                              String canCuPhapLy, boolean dangApDung) {
        static HangGplxDto of(HangGplx h) {
            return new HangGplxDto(h.getMa(), h.getTen(), h.getGioLyThuyet(), h.getGioThucHanh(),
                    h.getTuoiToiThieu(), h.getSoNgayKhoaToiDa(), h.getHocPhiMacDinh(), h.getCanCuPhapLy(),
                    Boolean.TRUE.equals(h.getDangApDung()));
        }
    }

    public record CapNhatHangRequest(@NotNull @DecimalMin("0") BigDecimal gioLyThuyet,
                                     @NotNull @DecimalMin("0") BigDecimal gioThucHanh,
                                     @NotNull @Min(16) Integer tuoiToiThieu,
                                     @NotNull @Min(1) Integer soNgayKhoaToiDa,
                                     @NotNull @DecimalMin("0") BigDecimal hocPhiMacDinh,
                                     String canCuPhapLy,
                                     Boolean dangApDung) {
    }

    public record GiaoVienDto(Long id, String hoTen, String soDienThoai, String soGiayChungNhanGv,
                              String loaiGiangDay, boolean hoatDong, String tenDangNhap) {
        static GiaoVienDto of(GiaoVien g) {
            return new GiaoVienDto(g.getId(), g.getHoTen(), g.getSoDienThoai(), g.getSoGiayChungNhanGv(),
                    g.getLoaiGiangDay().name(), Boolean.TRUE.equals(g.getTrangThai()),
                    g.getNguoiDung() == null ? null : g.getNguoiDung().getTenDangNhap());
        }
    }

    public record GiaoVienRequest(@NotBlank @Size(max = 100) String hoTen,
                                  @NotBlank @Pattern(regexp = SDT, message = LOI_SDT) String soDienThoai,
                                  @Size(max = 50) String soGiayChungNhanGv,
                                  @NotNull GiaoVien.LoaiGiangDay loaiGiangDay) {
    }

    public record CapTaiKhoanGvRequest(
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "3–50 ký tự: chữ không dấu, số, . _ -")
            String tenDangNhap) {
    }

    public record XeDto(Long id, String bienSo, String hang, String nhanHieu, Integer namSanXuat, String trangThai,
                        LocalDate ngayBaoDuongTiep) {
        static XeDto of(XeTapLai x) {
            return new XeDto(x.getId(), x.getBienSo(), x.getHang().getMa(), x.getNhanHieu(), x.getNamSanXuat(),
                    x.getTrangThai().name(), x.getNgayBaoDuongTiep());
        }
    }

    public record XeRequest(@NotBlank @Size(max = 20) String bienSo,
                            @NotBlank String hangMa,
                            @Size(max = 50) String nhanHieu,
                            @Min(1990) @Max(2100) Integer namSanXuat,
                            @NotNull XeTapLai.TrangThaiXe trangThai,
                            LocalDate ngayBaoDuongTiep) {
    }

    public record DoiTrangThaiRequest(@NotNull Boolean hoatDong) {
    }

    private final HangGplxRepository hangRepo;
    private final GiaoVienRepository gvRepo;
    private final XeTapLaiRepository xeRepo;
    private final BuoiHocRepository buoiRepo;
    private final TaiKhoanService taiKhoan;
    private final NhatKyService nhatKy;

    public DanhMucController(HangGplxRepository hangRepo, GiaoVienRepository gvRepo, XeTapLaiRepository xeRepo,
                             BuoiHocRepository buoiRepo,
                             TaiKhoanService taiKhoan, NhatKyService nhatKy) {
        this.hangRepo = hangRepo;
        this.gvRepo = gvRepo;
        this.xeRepo = xeRepo;
        this.buoiRepo = buoiRepo;
        this.taiKhoan = taiKhoan;
        this.nhatKy = nhatKy;
    }

    // ------------------------------------------------------------------ hang GPLX
    @GetMapping("/hang-gplx")
    public List<HangGplxDto> dsHang() {
        return hangRepo.findAll().stream().map(HangGplxDto::of).toList();
    }

    /** Cap nhat so gio khi co thong tu moi - chi ADMIN, ghi nhat ky gia tri cu. */
    @PutMapping("/hang-gplx/{ma}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public HangGplxDto capNhatHang(@PathVariable String ma, @Valid @RequestBody CapNhatHangRequest req,
                                   @AuthenticationPrincipal AuthUser admin) {
        HangGplx h = hangRepo.findById(ma).orElseThrow(() -> new NotFoundException("hạng GPLX", ma));
        nhatKy.ghi(admin.id(), "SUA_HANG_GPLX", "hang_gplx", null, Map.of("ma", ma,
                "cu", HangGplxDto.of(h)));
        h.setGioLyThuyet(req.gioLyThuyet());
        h.setGioThucHanh(req.gioThucHanh());
        h.setTuoiToiThieu(req.tuoiToiThieu());
        h.setSoNgayKhoaToiDa(req.soNgayKhoaToiDa());
        h.setHocPhiMacDinh(req.hocPhiMacDinh());
        h.setCanCuPhapLy(req.canCuPhapLy());
        if (req.dangApDung() != null) h.setDangApDung(req.dangApDung());
        return HangGplxDto.of(h);
    }

    // ------------------------------------------------------------------ giao vien
    @GetMapping("/giao-vien")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    @Transactional(readOnly = true)
    public List<GiaoVienDto> dsGiaoVien(@RequestParam(defaultValue = "false") boolean tatCa) {
        return (tatCa ? gvRepo.findAllByOrderByTrangThaiDescHoTen() : gvRepo.findByTrangThaiTrueOrderByHoTen())
                .stream().map(GiaoVienDto::of).toList();
    }

    @PostMapping("/giao-vien")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public GiaoVienDto taoGiaoVien(@Valid @RequestBody GiaoVienRequest req) {
        GiaoVien g = new GiaoVien();
        ganGiaoVien(g, req);
        return GiaoVienDto.of(gvRepo.save(g));
    }

    @PutMapping("/giao-vien/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public GiaoVienDto suaGiaoVien(@PathVariable Long id, @Valid @RequestBody GiaoVienRequest req) {
        GiaoVien g = layGiaoVien(id);
        ganGiaoVien(g, req);
        if (g.getNguoiDung() != null) g.getNguoiDung().setHoTen(g.getHoTen());
        return GiaoVienDto.of(g);
    }

    /** Ngung giao vien: khong duoc con buoi day ke hoach; khoa luon tai khoan dang nhap. */
    @PatchMapping("/giao-vien/{id}/trang-thai")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public GiaoVienDto doiTrangThaiGiaoVien(@PathVariable Long id, @Valid @RequestBody DoiTrangThaiRequest req,
                                            @AuthenticationPrincipal AuthUser admin) {
        GiaoVien g = layGiaoVien(id);
        if (!req.hoatDong()) {
            long conLich = buoiRepo.countByGiaoVienIdAndTrangThaiAndNgayGreaterThanEqual(
                    id, BuoiHoc.TrangThai.KE_HOACH, LocalDate.now());
            if (conLich > 0) {
                throw new BusinessException("GV_CON_LICH", "Giáo viên còn " + conLich
                        + " buổi dạy theo kế hoạch. Hãy phân công lại trước khi ngừng.");
            }
        }
        g.setTrangThai(req.hoatDong());
        if (g.getNguoiDung() != null && !req.hoatDong().equals(g.getNguoiDung().getTrangThai())) {
            taiKhoan.doiTrangThai(g.getNguoiDung(), req.hoatDong(), admin.id());
        }
        return GiaoVienDto.of(g);
    }

    /** Cap tai khoan cho giao vien (chua co) hoac dat lai mat khau (da co). Tra mat khau tam MOT lan. */
    @PostMapping("/giao-vien/{id}/tai-khoan")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public KetQuaCapMatKhau capTaiKhoanGiaoVien(@PathVariable Long id, @Valid @RequestBody CapTaiKhoanGvRequest req,
                                                @AuthenticationPrincipal AuthUser admin) {
        GiaoVien g = layGiaoVien(id);
        if (g.getNguoiDung() != null) {
            return taiKhoan.datLaiMatKhau(g.getNguoiDung(), admin.id());
        }
        if (req.tenDangNhap() == null || req.tenDangNhap().isBlank()) {
            throw new BusinessException("THIEU_TEN_DANG_NHAP", "Nhập tên đăng nhập cho giáo viên");
        }
        KetQuaCapMatKhau kq = taiKhoan.tao(req.tenDangNhap(), g.getHoTen(), g.getSoDienThoai(), null,
                VaiTro.GIAO_VIEN, admin.id());
        g.setNguoiDung(taiKhoan.layThamChieu(kq.nguoiDungId()));
        return kq;
    }

    private GiaoVien layGiaoVien(Long id) {
        return gvRepo.findById(id).orElseThrow(() -> new NotFoundException("giáo viên", id));
    }

    private static void ganGiaoVien(GiaoVien g, GiaoVienRequest req) {
        g.setHoTen(req.hoTen().trim());
        g.setSoDienThoai(req.soDienThoai());
        g.setSoGiayChungNhanGv(rong(req.soGiayChungNhanGv()));
        g.setLoaiGiangDay(req.loaiGiangDay());
    }

    // ------------------------------------------------------------------ xe tap lai
    @GetMapping("/xe-tap-lai")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    @Transactional(readOnly = true)
    public List<XeDto> dsXe() {
        return xeRepo.findAllByOrderByBienSo().stream().map(XeDto::of).toList();
    }

    @PostMapping("/xe-tap-lai")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public XeDto taoXe(@Valid @RequestBody XeRequest req) {
        String bienSo = req.bienSo().trim().toUpperCase();
        if (xeRepo.existsByBienSo(bienSo)) {
            throw new BusinessException("TRUNG_BIEN_SO", "Biển số " + bienSo + " đã có trong danh mục");
        }
        XeTapLai x = new XeTapLai();
        ganXe(x, req, bienSo);
        return XeDto.of(xeRepo.save(x));
    }

    @PutMapping("/xe-tap-lai/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public XeDto suaXe(@PathVariable Long id, @Valid @RequestBody XeRequest req) {
        XeTapLai x = xeRepo.findById(id).orElseThrow(() -> new NotFoundException("xe tập lái", id));
        String bienSo = req.bienSo().trim().toUpperCase();
        if (xeRepo.existsByBienSoAndIdNot(bienSo, id)) {
            throw new BusinessException("TRUNG_BIEN_SO", "Biển số " + bienSo + " đã có trong danh mục");
        }
        if (req.trangThai() != XeTapLai.TrangThaiXe.SAN_SANG) {
            long conLich = buoiRepo.countByXeIdAndTrangThaiAndNgayGreaterThanEqual(
                    id, BuoiHoc.TrangThai.KE_HOACH, LocalDate.now());
            if (conLich > 0) {
                throw new BusinessException("XE_CON_LICH", "Xe còn " + conLich
                        + " buổi tập theo kế hoạch. Hãy đổi xe cho các buổi đó trước.");
            }
        }
        ganXe(x, req, bienSo);
        return XeDto.of(x);
    }

    private void ganXe(XeTapLai x, XeRequest req, String bienSo) {
        x.setBienSo(bienSo);
        x.setHang(hangRepo.findById(req.hangMa()).orElseThrow(() -> new NotFoundException("hạng GPLX", req.hangMa())));
        x.setNhanHieu(rong(req.nhanHieu()));
        x.setNamSanXuat(req.namSanXuat());
        x.setTrangThai(req.trangThai());
        x.setNgayBaoDuongTiep(req.ngayBaoDuongTiep());
    }

    private static String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
