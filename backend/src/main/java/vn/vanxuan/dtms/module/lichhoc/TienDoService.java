package vn.vanxuan.dtms.module.lichhoc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tien do hoc: gio da hoc so voi gio quy dinh cua hang (TT 14/2025, sua doi boi TT 17/2026).
 * BR-05: hoc vien TU_HOC ly thuyet khong xet gio ly thuyet tai trung tam.
 */
@Service
public class TienDoService {

    public record TienDo(Long dangKyId, String hang, String hinhThucLyThuyet,
                         BigDecimal gioLyThuyetDaHoc, BigDecimal gioLyThuyetQuyDinh, boolean xetLyThuyet,
                         BigDecimal gioThucHanhDaHoc, BigDecimal gioThucHanhQuyDinh,
                         boolean duLyThuyet, boolean duThucHanh) {
        public boolean duThoiGian() {
            return duThucHanh && (!xetLyThuyet || duLyThuyet);
        }
    }

    private final DiemDanhRepository diemDanhRepo;
    private final DangKyRepository dangKyRepo;

    public TienDoService(DiemDanhRepository diemDanhRepo, DangKyRepository dangKyRepo) {
        this.diemDanhRepo = diemDanhRepo;
        this.dangKyRepo = dangKyRepo;
    }

    @Transactional(readOnly = true)
    public TienDo tinh(Long dangKyId) {
        DangKy dk = dangKyRepo.findChiTiet(dangKyId).orElseThrow(() -> new NotFoundException("hồ sơ đăng ký", dangKyId));
        return tinh(dk);
    }

    /** Dung lai trong xet hoan thanh (da co DangKy kem khoa, hang). */
    public TienDo tinh(DangKy dk) {
        BigDecimal lt = BigDecimal.ZERO, th = BigDecimal.ZERO;
        List<Object[]> rows = diemDanhRepo.tongGioTheoLoai(dk.getId());
        for (Object[] r : rows) {
            BuoiHoc.Loai loai = (BuoiHoc.Loai) r[0];
            BigDecimal gio = r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1];
            if (loai == BuoiHoc.Loai.LY_THUYET) lt = gio;
            else th = gio;
        }
        HangGplx hang = dk.getKhoa().getHang();
        boolean xetLt = dk.getHinhThucLyThuyet() == DangKy.HinhThucLyThuyet.TAP_TRUNG;
        return new TienDo(dk.getId(), hang.getMa(), dk.getHinhThucLyThuyet().name(),
                lt, hang.getGioLyThuyet(), xetLt,
                th, hang.getGioThucHanh(),
                lt.compareTo(hang.getGioLyThuyet()) >= 0,
                th.compareTo(hang.getGioThucHanh()) >= 0);
    }
}
