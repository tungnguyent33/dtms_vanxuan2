package vn.vanxuan.dtms.module.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.MatKhau;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.security.AuthUser;
import vn.vanxuan.dtms.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Dang nhap, lam moi phien, doi mat khau (FR-01).
 * Access token JWT ngan han; refresh token ngau nhien, luu bam SHA-256, xoay vong moi lan dung.
 * Dung lai mot refresh token da thu hoi -> coi nhu bi lo, thu hoi toan bo phien cua nguoi dung.
 */
@Service
public class AuthService {

    public record PhienDangNhap(String token, long hetHanSauPhut, String refreshToken, Long id, String tenDangNhap,
                                String hoTen, String vaiTro, boolean phaiDoiMatKhau) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private final AuthenticationManager authenticationManager;
    private final NguoiDungRepository nguoiDungRepo;
    private final RefreshTokenRepository refreshRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final NhatKyService nhatKy;
    private final TransactionTemplate tx;
    private final long refreshDays;
    private final int soLanSaiToiDa;
    private final int phutKhoaTam;

    public AuthService(AuthenticationManager authenticationManager, NguoiDungRepository nguoiDungRepo,
                       RefreshTokenRepository refreshRepo, JwtService jwtService, PasswordEncoder passwordEncoder,
                       NhatKyService nhatKy, PlatformTransactionManager txManager,
                       @Value("${app.jwt.refresh-days:7}") long refreshDays,
                       @Value("${app.dang-nhap.so-lan-sai-toi-da:5}") int soLanSaiToiDa,
                       @Value("${app.dang-nhap.phut-khoa-tam:15}") int phutKhoaTam) {
        this.authenticationManager = authenticationManager;
        this.nguoiDungRepo = nguoiDungRepo;
        this.refreshRepo = refreshRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.nhatKy = nhatKy;
        this.tx = new TransactionTemplate(txManager);
        this.refreshDays = refreshDays;
        this.soLanSaiToiDa = soLanSaiToiDa;
        this.phutKhoaTam = phutKhoaTam;
    }

    /** Khong de @Transactional o day: lan dang nhap sai van phai duoc ghi lai (khong bi rollback). */
    public PhienDangNhap dangNhap(String tenDangNhap, String matKhau) {
        nguoiDungRepo.findByTenDangNhap(tenDangNhap).ifPresent(nd -> {
            if (nd.getKhoaDen() != null && nd.getKhoaDen().isAfter(LocalDateTime.now())) {
                throw new BusinessException("TAI_KHOAN_TAM_KHOA", "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau "
                        + nd.getKhoaDen().format(GIO), HttpStatus.LOCKED);
            }
        });
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(tenDangNhap, matKhau));
        } catch (BadCredentialsException e) {
            tx.executeWithoutResult(s -> ghiLanSai(tenDangNhap));
            throw e;
        }
        return tx.execute(s -> {
            NguoiDung nd = nguoiDungRepo.findByTenDangNhap(tenDangNhap).orElseThrow();
            nd.setSoLanSai(0);
            nd.setKhoaDen(null);
            nd.setLanDangNhapCuoi(LocalDateTime.now());
            return phatHanh(nd);
        });
    }

    private void ghiLanSai(String tenDangNhap) {
        nguoiDungRepo.findByTenDangNhap(tenDangNhap).ifPresent(nd -> {
            int sai = nd.getSoLanSai() + 1;
            if (sai >= soLanSaiToiDa) {
                nd.setKhoaDen(LocalDateTime.now().plusMinutes(phutKhoaTam));
                nd.setSoLanSai(0);
                nhatKy.ghi(nd.getId(), "KHOA_TAM_DANG_NHAP", "nguoi_dung", nd.getId(), Map.of("phut", phutKhoaTam));
            } else {
                nd.setSoLanSai(sai);
            }
        });
    }

    /** Doi refresh token cu lay cap token moi. */
    @Transactional(noRollbackFor = BusinessException.class)
    public PhienDangNhap lamMoi(String refreshToken) {
        RefreshToken rt = refreshRepo.khoaTheoHash(bam(refreshToken)).orElseThrow(this::phienHetHan);
        if (rt.getThuHoi()) {
            // Token da dung roi ma con bi gui lai: co the da bi danh cap -> dang xuat moi thiet bi
            refreshRepo.thuHoiTatCa(rt.getNguoiDungId());
            throw phienHetHan();
        }
        if (rt.getHetHan().isBefore(LocalDateTime.now())) {
            throw phienHetHan();
        }
        NguoiDung nd = nguoiDungRepo.findById(rt.getNguoiDungId()).orElseThrow(this::phienHetHan);
        if (!Boolean.TRUE.equals(nd.getTrangThai())) {
            refreshRepo.thuHoiTatCa(nd.getId());
            throw new BusinessException("TAI_KHOAN_BI_KHOA", "Tài khoản đã bị khóa", HttpStatus.UNAUTHORIZED);
        }
        rt.setThuHoi(true);
        return phatHanh(nd);
    }

    @Transactional
    public void dangXuat(String refreshToken) {
        refreshRepo.khoaTheoHash(bam(refreshToken)).ifPresent(rt -> rt.setThuHoi(true));
    }

    /** Doi mat khau: thu hoi moi phien cu, cap phien moi cho thiet bi dang dung. */
    @Transactional
    public PhienDangNhap doiMatKhau(AuthUser user, String matKhauCu, String matKhauMoi) {
        NguoiDung nd = nguoiDungRepo.findById(user.id()).orElseThrow(() -> new NotFoundException("người dùng", user.id()));
        if (!passwordEncoder.matches(matKhauCu, nd.getMatKhauHash())) {
            throw new BusinessException("MAT_KHAU_CU_SAI", "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(matKhauMoi, nd.getMatKhauHash())) {
            throw new BusinessException("TRUNG_MAT_KHAU_CU", "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        MatKhau.kiemTraDoManh(matKhauMoi);
        nd.setMatKhauHash(passwordEncoder.encode(matKhauMoi));
        nd.setPhaiDoiMatKhau(false);
        refreshRepo.thuHoiTatCa(nd.getId());
        nhatKy.ghi(nd.getId(), "DOI_MAT_KHAU", "nguoi_dung", nd.getId(), null);
        return phatHanh(nd);
    }

    /** Thu hoi moi phien cua nguoi dung (khi khoa tai khoan, dat lai mat khau). */
    @Transactional
    public void thuHoiMoiPhien(Long nguoiDungId) {
        refreshRepo.thuHoiTatCa(nguoiDungId);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void donTokenHetHan() {
        refreshRepo.xoaHetHan(LocalDateTime.now().minusDays(1));
    }

    private PhienDangNhap phatHanh(NguoiDung nd) {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        RefreshToken rt = new RefreshToken();
        rt.setNguoiDungId(nd.getId());
        rt.setTokenHash(bam(raw));
        rt.setHetHan(LocalDateTime.now().plusDays(refreshDays));
        refreshRepo.save(rt);
        boolean pdm = Boolean.TRUE.equals(nd.getPhaiDoiMatKhau());
        AuthUser user = new AuthUser(nd.getId(), nd.getTenDangNhap(), nd.getVaiTro().getMa(), pdm);
        return new PhienDangNhap(jwtService.taoToken(user), jwtService.getExpirationMinutes(), raw, nd.getId(),
                nd.getTenDangNhap(), nd.getHoTen(), nd.getVaiTro().getMa(), pdm);
    }

    private BusinessException phienHetHan() {
        return new BusinessException("PHIEN_HET_HAN", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
                HttpStatus.UNAUTHORIZED);
    }

    static String bam(String raw) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
