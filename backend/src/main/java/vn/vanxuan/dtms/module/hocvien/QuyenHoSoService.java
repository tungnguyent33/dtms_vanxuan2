package vn.vanxuan.dtms.module.hocvien;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;

/**
 * Yeu cau phi chuc nang: "hoc vien chi truy cap du lieu cua chinh minh" (ma tran phan quyen, Bang 13).
 * - ADMIN, LE_TAN: moi ho so.
 * - GIAO_VIEN: ho so trong khoa minh duoc phan cong day ("xem hoc vien lop minh").
 * - HOC_VIEN: chi ho so cua chinh minh.
 * Khong co quyen thi tra 404 (khong phai 403) de khong lo ho so nao ton tai.
 */
@Service
public class QuyenHoSoService {

    private final DangKyRepository dangKyRepo;

    public QuyenHoSoService(DangKyRepository dangKyRepo) {
        this.dangKyRepo = dangKyRepo;
    }

    @Transactional(readOnly = true)
    public void kiemTraXem(AuthUser user, Long dangKyId) {
        if (user.la(VaiTro.ADMIN) || user.la(VaiTro.LE_TAN)) return;
        boolean duoc = user.la(VaiTro.HOC_VIEN) ? dangKyRepo.laCuaHocVien(dangKyId, user.id())
                : user.la(VaiTro.GIAO_VIEN) && dangKyRepo.laHocVienCuaGiaoVien(dangKyId, user.id());
        if (!duoc) throw new NotFoundException("hồ sơ đăng ký", dangKyId);
    }
}
