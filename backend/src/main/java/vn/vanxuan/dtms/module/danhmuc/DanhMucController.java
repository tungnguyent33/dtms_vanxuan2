package vn.vanxuan.dtms.module.danhmuc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.NotFoundException;

import java.math.BigDecimal;
import java.util.List;

/** API danh muc: hang GPLX, giao vien, xe tap lai, cong tac vien. */
@RestController
@RequestMapping("/api")
public class DanhMucController {

    public record HangGplxDto(String ma, String ten, BigDecimal gioLyThuyet, BigDecimal gioThucHanh,
                              Integer tuoiToiThieu, Integer soNgayKhoaToiDa, BigDecimal hocPhiMacDinh,
                              String canCuPhapLy) {
        static HangGplxDto of(HangGplx h) {
            return new HangGplxDto(h.getMa(), h.getTen(), h.getGioLyThuyet(), h.getGioThucHanh(),
                    h.getTuoiToiThieu(), h.getSoNgayKhoaToiDa(), h.getHocPhiMacDinh(), h.getCanCuPhapLy());
        }
    }

    public record CapNhatHangRequest(@NotNull @DecimalMin("0") BigDecimal gioLyThuyet,
                                     @NotNull @DecimalMin("0") BigDecimal gioThucHanh,
                                     @NotNull @Min(16) Integer tuoiToiThieu,
                                     @NotNull @Min(1) Integer soNgayKhoaToiDa,
                                     @NotNull @DecimalMin("0") BigDecimal hocPhiMacDinh,
                                     String canCuPhapLy) {
    }

    public record GiaoVienDto(Long id, String hoTen, String soDienThoai, String loaiGiangDay) {
    }

    public record XeDto(Long id, String bienSo, String hang, String nhanHieu, String trangThai) {
    }

    public record CtvDto(Long id, String hoTen, String soDienThoai, String diaBan) {
    }

    private final HangGplxRepository hangRepo;
    private final GiaoVienRepository gvRepo;
    private final XeTapLaiRepository xeRepo;
    private final CongTacVienRepository ctvRepo;

    public DanhMucController(HangGplxRepository hangRepo, GiaoVienRepository gvRepo,
                             XeTapLaiRepository xeRepo, CongTacVienRepository ctvRepo) {
        this.hangRepo = hangRepo;
        this.gvRepo = gvRepo;
        this.xeRepo = xeRepo;
        this.ctvRepo = ctvRepo;
    }

    @GetMapping("/hang-gplx")
    public List<HangGplxDto> dsHang() {
        return hangRepo.findAll().stream().map(HangGplxDto::of).toList();
    }

    /** Cap nhat so gio khi co thong tu moi - chi ADMIN. */
    @PutMapping("/hang-gplx/{ma}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public HangGplxDto capNhatHang(@PathVariable String ma, @Valid @RequestBody CapNhatHangRequest req) {
        HangGplx h = hangRepo.findById(ma).orElseThrow(() -> new NotFoundException("hạng GPLX", ma));
        h.setGioLyThuyet(req.gioLyThuyet());
        h.setGioThucHanh(req.gioThucHanh());
        h.setTuoiToiThieu(req.tuoiToiThieu());
        h.setSoNgayKhoaToiDa(req.soNgayKhoaToiDa());
        h.setHocPhiMacDinh(req.hocPhiMacDinh());
        h.setCanCuPhapLy(req.canCuPhapLy());
        return HangGplxDto.of(h);
    }

    @GetMapping("/giao-vien")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public List<GiaoVienDto> dsGiaoVien() {
        return gvRepo.findByTrangThaiTrueOrderByHoTen().stream()
                .map(g -> new GiaoVienDto(g.getId(), g.getHoTen(), g.getSoDienThoai(), g.getLoaiGiangDay().name()))
                .toList();
    }

    @GetMapping("/xe-tap-lai")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    @Transactional(readOnly = true)
    public List<XeDto> dsXe() {
        return xeRepo.findAllByOrderByBienSo().stream()
                .map(x -> new XeDto(x.getId(), x.getBienSo(), x.getHang().getMa(), x.getNhanHieu(), x.getTrangThai().name()))
                .toList();
    }

    @GetMapping("/ctv")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public List<CtvDto> dsCtv() {
        return ctvRepo.findByTrangThaiTrueOrderByHoTen().stream()
                .map(c -> new CtvDto(c.getId(), c.getHoTen(), c.getSoDienThoai(), c.getDiaBan()))
                .toList();
    }
}
