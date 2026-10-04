package vn.vanxuan.dtms.module.nguoidung;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.PageResponse;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService.KetQuaCapMatKhau;
import vn.vanxuan.dtms.security.AuthUser;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Quan ly tai khoan nhan vien (FR-03) - chi ADMIN.
 * Tai khoan giao vien cap tu danh muc giao vien; tai khoan hoc vien cap tu ho so hoc vien.
 */
@RestController
@RequestMapping("/api/nguoi-dung")
@PreAuthorize("hasRole('ADMIN')")
public class NguoiDungController {

    /** Vai tro duoc tao / doi truc tiep o man hinh nguoi dung. */
    private static final Set<String> VAI_TRO_NHAN_VIEN = Set.of(VaiTro.ADMIN, VaiTro.LE_TAN);

    public record NguoiDungResponse(Long id, String tenDangNhap, String hoTen, String soDienThoai, String email,
                                    String vaiTro, boolean hoatDong, boolean phaiDoiMatKhau, boolean dangKhoaTam,
                                    LocalDateTime lanDangNhapCuoi) {
        static NguoiDungResponse of(NguoiDung n) {
            boolean khoaTam = n.getKhoaDen() != null && n.getKhoaDen().isAfter(LocalDateTime.now());
            return new NguoiDungResponse(n.getId(), n.getTenDangNhap(), n.getHoTen(), n.getSoDienThoai(),
                    n.getEmail(), n.getVaiTro().getMa(), Boolean.TRUE.equals(n.getTrangThai()),
                    Boolean.TRUE.equals(n.getPhaiDoiMatKhau()), khoaTam, n.getLanDangNhapCuoi());
        }
    }

    public record TaoNguoiDungRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "3–50 ký tự: chữ không dấu, số, . _ -")
            String tenDangNhap,
            @NotBlank @Size(max = 100) String hoTen,
            @Pattern(regexp = "^(0[0-9]{9})?$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0") String soDienThoai,
            @Email String email,
            @NotBlank String vaiTro) {
    }

    public record SuaNguoiDungRequest(
            @NotBlank @Size(max = 100) String hoTen,
            @Pattern(regexp = "^(0[0-9]{9})?$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0") String soDienThoai,
            @Email String email,
            String vaiTro) {
    }

    public record DoiTrangThaiRequest(@NotNull Boolean hoatDong) {
    }

    private final NguoiDungRepository repo;
    private final VaiTroRepository vaiTroRepo;
    private final TaiKhoanService taiKhoan;

    public NguoiDungController(NguoiDungRepository repo, VaiTroRepository vaiTroRepo, TaiKhoanService taiKhoan) {
        this.repo = repo;
        this.vaiTroRepo = vaiTroRepo;
        this.taiKhoan = taiKhoan;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<NguoiDungResponse> ds(@RequestParam(required = false) String vaiTro,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        String tuKhoa = q == null || q.isBlank() ? null : q.trim();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("vaiTro.id", "hoTen"));
        return PageResponse.of(repo.timKiem(vaiTro, tuKhoa, pageable), NguoiDungResponse::of);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public KetQuaCapMatKhau tao(@Valid @RequestBody TaoNguoiDungRequest req, @AuthenticationPrincipal AuthUser admin) {
        if (!VAI_TRO_NHAN_VIEN.contains(req.vaiTro())) {
            throw new BusinessException("VAI_TRO_KHONG_HOP_LE",
                    "Tài khoản giáo viên cấp ở danh mục Giáo viên; tài khoản học viên cấp ở hồ sơ học viên");
        }
        return taiKhoan.tao(req.tenDangNhap(), req.hoTen(), rong(req.soDienThoai()), rong(req.email()), req.vaiTro(),
                admin.id());
    }

    @PutMapping("/{id}")
    @Transactional
    public NguoiDungResponse sua(@PathVariable Long id, @Valid @RequestBody SuaNguoiDungRequest req,
                                 @AuthenticationPrincipal AuthUser admin) {
        NguoiDung nd = lay(id);
        nd.setHoTen(req.hoTen().trim());
        nd.setSoDienThoai(rong(req.soDienThoai()));
        nd.setEmail(rong(req.email()));
        String cu = nd.getVaiTro().getMa();
        if (req.vaiTro() != null && !req.vaiTro().equals(cu)) {
            // Chi doi qua lai giua ADMIN va LE_TAN; khong bo quan tri vien cuoi cung
            if (!VAI_TRO_NHAN_VIEN.contains(cu) || !VAI_TRO_NHAN_VIEN.contains(req.vaiTro())) {
                throw new BusinessException("VAI_TRO_KHONG_HOP_LE", "Chỉ đổi được giữa Quản trị viên và Lễ tân");
            }
            if (VaiTro.ADMIN.equals(cu) && repo.countByVaiTroMaAndTrangThaiTrue(VaiTro.ADMIN) <= 1) {
                throw new BusinessException("QUAN_TRI_CUOI", "Phải còn ít nhất một quản trị viên hoạt động");
            }
            if (id.equals(admin.id())) {
                throw new BusinessException("KHONG_TU_DOI_VAI_TRO", "Không thể tự đổi vai trò của chính mình");
            }
            nd.setVaiTro(vaiTroRepo.findByMa(req.vaiTro()).orElseThrow());
        }
        return NguoiDungResponse.of(nd);
    }

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public NguoiDungResponse doiTrangThai(@PathVariable Long id, @Valid @RequestBody DoiTrangThaiRequest req,
                                          @AuthenticationPrincipal AuthUser admin) {
        NguoiDung nd = lay(id);
        taiKhoan.doiTrangThai(nd, req.hoatDong(), admin.id());
        return NguoiDungResponse.of(nd);
    }

    /** Tra ve mat khau tam DUY NHAT mot lan - quan tri vien chuyen cho nguoi dung. */
    @PostMapping("/{id}/dat-lai-mat-khau")
    @Transactional
    public KetQuaCapMatKhau datLaiMatKhau(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return taiKhoan.datLaiMatKhau(lay(id), admin.id());
    }

    private NguoiDung lay(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("người dùng", id));
    }

    private static String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
