package vn.vanxuan.dtms.module.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.auth.AuthService.PhienDangNhap;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.security.AuthUser;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String tenDangNhap, @NotBlank String matKhau) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record DoiMatKhauRequest(@NotBlank String matKhauCu, @NotBlank String matKhauMoi) {
    }

    public record MeResponse(Long id, String tenDangNhap, String hoTen, String vaiTro, String soDienThoai,
                             String email, boolean phaiDoiMatKhau) {
    }

    private final AuthService authService;
    private final NguoiDungRepository nguoiDungRepo;

    public AuthController(AuthService authService, NguoiDungRepository nguoiDungRepo) {
        this.authService = authService;
        this.nguoiDungRepo = nguoiDungRepo;
    }

    /** Sai mat khau -> 401; sai qua so lan cho phep -> 423 (tam khoa). */
    @PostMapping("/login")
    public PhienDangNhap login(@Valid @RequestBody LoginRequest req) {
        return authService.dangNhap(req.tenDangNhap().trim(), req.matKhau());
    }

    @PostMapping("/refresh")
    public PhienDangNhap refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.lamMoi(req.refreshToken());
    }

    @PostMapping("/dang-xuat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dangXuat(@Valid @RequestBody RefreshRequest req) {
        authService.dangXuat(req.refreshToken());
    }

    @PutMapping("/doi-mat-khau")
    public PhienDangNhap doiMatKhau(@Valid @RequestBody DoiMatKhauRequest req, @AuthenticationPrincipal AuthUser user) {
        return authService.doiMatKhau(user, req.matKhauCu(), req.matKhauMoi());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthUser user) {
        NguoiDung nd = nguoiDungRepo.findById(user.id()).orElseThrow(() -> new NotFoundException("người dùng", user.id()));
        return new MeResponse(nd.getId(), nd.getTenDangNhap(), nd.getHoTen(), nd.getVaiTro().getMa(),
                nd.getSoDienThoai(), nd.getEmail(), Boolean.TRUE.equals(nd.getPhaiDoiMatKhau()));
    }
}
