package vn.vanxuan.dtms.module.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.security.AuthUser;
import vn.vanxuan.dtms.security.JwtService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String tenDangNhap, @NotBlank String matKhau) {
    }

    public record LoginResponse(String token, long hetHanSauPhut, Long id, String tenDangNhap,
                                String hoTen, String vaiTro) {
    }

    public record MeResponse(Long id, String tenDangNhap, String hoTen, String vaiTro) {
    }

    private final AuthenticationManager authenticationManager;
    private final NguoiDungRepository nguoiDungRepo;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, NguoiDungRepository nguoiDungRepo,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.nguoiDungRepo = nguoiDungRepo;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Transactional
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // Nem BadCredentialsException / DisabledException neu sai -> GlobalExceptionHandler tra 401
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.tenDangNhap(), req.matKhau()));
        NguoiDung nd = nguoiDungRepo.findByTenDangNhap(req.tenDangNhap()).orElseThrow();
        nd.setLanDangNhapCuoi(LocalDateTime.now());
        AuthUser user = new AuthUser(nd.getId(), nd.getTenDangNhap(), nd.getVaiTro().getMa());
        return new LoginResponse(jwtService.taoToken(user), jwtService.getExpirationMinutes(),
                nd.getId(), nd.getTenDangNhap(), nd.getHoTen(), nd.getVaiTro().getMa());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthUser user) {
        NguoiDung nd = nguoiDungRepo.findById(user.id()).orElseThrow(() -> new NotFoundException("người dùng", user.id()));
        return new MeResponse(nd.getId(), nd.getTenDangNhap(), nd.getHoTen(), nd.getVaiTro().getMa());
    }
}
