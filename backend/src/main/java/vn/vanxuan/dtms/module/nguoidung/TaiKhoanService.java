package vn.vanxuan.dtms.module.nguoidung;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.MatKhau;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.module.auth.AuthService;

import java.util.Map;

/**
 * Tao tai khoan va cap mat khau tam (FR-03). Mat khau tam chi tra ve DUY NHAT mot lan cho nguoi cap,
 * khong luu dang ro; nguoi dung bi buoc doi o lan dang nhap dau (FR-01).
 */
@Service
public class TaiKhoanService {

    public record KetQuaCapMatKhau(Long nguoiDungId, String tenDangNhap, String matKhauTam) {
    }

    private final NguoiDungRepository nguoiDungRepo;
    private final VaiTroRepository vaiTroRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final NhatKyService nhatKy;

    public TaiKhoanService(NguoiDungRepository nguoiDungRepo, VaiTroRepository vaiTroRepo,
                           PasswordEncoder passwordEncoder, AuthService authService, NhatKyService nhatKy) {
        this.nguoiDungRepo = nguoiDungRepo;
        this.vaiTroRepo = vaiTroRepo;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.nhatKy = nhatKy;
    }

    /** Tao tai khoan moi kem mat khau tam. Nguoi goi phai dang trong transaction. */
    @Transactional
    public KetQuaCapMatKhau tao(String tenDangNhap, String hoTen, String soDienThoai, String email, String vaiTroMa,
                                Long nguoiThucHienId) {
        String ten = tenDangNhap.trim();
        if (nguoiDungRepo.existsByTenDangNhap(ten)) {
            throw new BusinessException("TRUNG_TEN_DANG_NHAP", "Tên đăng nhập " + ten + " đã được dùng");
        }
        VaiTro vt = vaiTroRepo.findByMa(vaiTroMa)
                .orElseThrow(() -> new BusinessException("VAI_TRO_KHONG_HOP_LE", "Vai trò không hợp lệ: " + vaiTroMa));
        String mk = MatKhau.taoTam();
        NguoiDung nd = new NguoiDung();
        nd.setTenDangNhap(ten);
        nd.setHoTen(hoTen.trim());
        nd.setSoDienThoai(soDienThoai);
        nd.setEmail(email);
        nd.setVaiTro(vt);
        nd.setTrangThai(true);
        nd.setMatKhauHash(passwordEncoder.encode(mk));
        nd.setPhaiDoiMatKhau(true);
        nguoiDungRepo.save(nd);
        nhatKy.ghi(nguoiThucHienId, "TAO_TAI_KHOAN", "nguoi_dung", nd.getId(), Map.of("vaiTro", vaiTroMa, "ten", ten));
        return new KetQuaCapMatKhau(nd.getId(), ten, mk);
    }

    public NguoiDung layThamChieu(Long id) {
        return nguoiDungRepo.getReferenceById(id);
    }

    /** Dat lai mat khau tam, mo khoa tam va dang xuat moi thiet bi. */
    @Transactional
    public KetQuaCapMatKhau datLaiMatKhau(NguoiDung nd, Long nguoiThucHienId) {
        String mk = MatKhau.taoTam();
        nd.setMatKhauHash(passwordEncoder.encode(mk));
        nd.setPhaiDoiMatKhau(true);
        nd.setSoLanSai(0);
        nd.setKhoaDen(null);
        authService.thuHoiMoiPhien(nd.getId());
        nhatKy.ghi(nguoiThucHienId, "DAT_LAI_MAT_KHAU", "nguoi_dung", nd.getId(), null);
        return new KetQuaCapMatKhau(nd.getId(), nd.getTenDangNhap(), mk);
    }

    /** Khoa / mo tai khoan. Khoa thi dang xuat moi thiet bi ngay. */
    @Transactional
    public void doiTrangThai(NguoiDung nd, boolean hoatDong, Long nguoiThucHienId) {
        if (!hoatDong && nd.getId().equals(nguoiThucHienId)) {
            throw new BusinessException("KHONG_TU_KHOA", "Không thể tự khóa tài khoản đang đăng nhập");
        }
        if (!hoatDong && VaiTro.ADMIN.equals(nd.getVaiTro().getMa())
                && Boolean.TRUE.equals(nd.getTrangThai())
                && nguoiDungRepo.countByVaiTroMaAndTrangThaiTrue(VaiTro.ADMIN) <= 1) {
            throw new BusinessException("QUAN_TRI_CUOI", "Phải còn ít nhất một quản trị viên hoạt động");
        }
        nd.setTrangThai(hoatDong);
        if (!hoatDong) authService.thuHoiMoiPhien(nd.getId());
        nhatKy.ghi(nguoiThucHienId, hoatDong ? "MO_TAI_KHOAN" : "KHOA_TAI_KHOAN", "nguoi_dung", nd.getId(), null);
    }
}
