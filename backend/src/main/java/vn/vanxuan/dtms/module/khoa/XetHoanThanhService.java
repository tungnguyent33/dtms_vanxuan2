package vn.vanxuan.dtms.module.khoa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.SoThuTuService;
import vn.vanxuan.dtms.module.hocphi.HocPhiDtos;
import vn.vanxuan.dtms.module.hocphi.HocPhiService;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.lichhoc.TienDoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Xet hoan thanh khoa dao tao (UC10).
 * BR-08: A1/A hoan thanh khi tham du du thoi gian hoc theo quy dinh (TT 14/2025, Dieu 5).
 * BR-09: (chinh sach noi bo, bat/tat trong application.yml) phai het cong no.
 */
@Service
public class XetHoanThanhService {

    public record KetQuaXet(Long dangKyId, String maHoSo, String hoTen, String hinhThucLyThuyet,
                            BigDecimal gioLyThuyet, BigDecimal gioLyThuyetQuyDinh,
                            BigDecimal gioThucHanh, BigDecimal gioThucHanhQuyDinh,
                            BigDecimal conNo, boolean dat, List<String> lyDo) {
    }

    private static final Set<DangKy.TrangThai> DUOC_XET = EnumSet.of(
            DangKy.TrangThai.DA_TIEP_NHAN, DangKy.TrangThai.DANG_HOC, DangKy.TrangThai.CHUA_DAT);

    private final KhoaDaoTaoRepository khoaRepo;
    private final DangKyRepository dangKyRepo;
    private final TienDoService tienDoService;
    private final HocPhiService hocPhiService;
    private final SoThuTuService soThuTu;
    private final NhatKyService nhatKy;
    private final boolean yeuCauHetNo;

    public XetHoanThanhService(KhoaDaoTaoRepository khoaRepo, DangKyRepository dangKyRepo,
                               TienDoService tienDoService, HocPhiService hocPhiService, SoThuTuService soThuTu,
                               NhatKyService nhatKy,
                               @Value("${app.chinh-sach.yeu-cau-het-no-khi-hoan-thanh:true}") boolean yeuCauHetNo) {
        this.khoaRepo = khoaRepo;
        this.dangKyRepo = dangKyRepo;
        this.tienDoService = tienDoService;
        this.hocPhiService = hocPhiService;
        this.soThuTu = soThuTu;
        this.nhatKy = nhatKy;
        this.yeuCauHetNo = yeuCauHetNo;
    }

    /** Buoc 1-4: tinh toan, KHONG ghi CSDL - de quan tri vien xem truoc. */
    @Transactional(readOnly = true)
    public List<KetQuaXet> xemTruoc(Long khoaId) {
        KhoaDaoTao khoa = khoaRepo.findWithHang(khoaId).orElseThrow(() -> new NotFoundException("khóa đào tạo", khoaId));
        return danhSachDuocXet(khoa).stream().map(this::xet).toList();
    }

    /** Buoc 5-6: ghi ket qua, cap so giay xac nhan cho hoc vien dat. */
    @Transactional
    public List<KetQuaXet> xacNhan(Long khoaId, AuthUser admin) {
        KhoaDaoTao khoa = khoaRepo.findWithHang(khoaId).orElseThrow(() -> new NotFoundException("khóa đào tạo", khoaId));
        if (LocalDate.now().isBefore(khoa.getNgayBeGiang())) {
            throw new BusinessException("CHUA_BE_GIANG", "Chỉ xét hoàn thành từ ngày bế giảng " + khoa.getNgayBeGiang());
        }
        List<KetQuaXet> ketQua = new ArrayList<>();
        int soDat = 0;
        for (DangKy dk : danhSachDuocXet(khoa)) {
            KetQuaXet kq = xet(dk);
            if (kq.dat()) {
                int nam = LocalDate.now().getYear();
                dk.setTrangThai(DangKy.TrangThai.HOAN_THANH);
                dk.setSoGiayXacNhan(soThuTu.capMa("XN" + nam, "XN-" + nam + "-", 5));
                dk.setNgayHoanThanh(LocalDate.now());
                soDat++;
            } else {
                dk.setTrangThai(DangKy.TrangThai.CHUA_DAT);
            }
            ketQua.add(kq);
        }
        nhatKy.ghi(admin.id(), "XET_HOAN_THANH", "khoa_dao_tao", khoaId,
                Map.of("tongSo", ketQua.size(), "soDat", soDat));
        return ketQua;
    }

    private List<DangKy> danhSachDuocXet(KhoaDaoTao khoa) {
        // khoa (kem hang) da nam trong persistence context nen dk.getKhoa().getHang() khong truy van them
        return dangKyRepo.findByKhoaIdAndTrangThaiInOrderByHocVienHoTen(khoa.getId(), DUOC_XET);
    }

    private KetQuaXet xet(DangKy dk) {
        var td = tienDoService.tinh(dk);
        HocPhiDtos.CongNo cn = hocPhiService.tinhCongNo(dk);
        List<String> lyDo = new ArrayList<>();
        if (!td.duThucHanh()) {
            lyDo.add("Thiếu giờ thực hành (" + td.gioThucHanhDaHoc() + "/" + td.gioThucHanhQuyDinh() + ")");
        }
        if (td.xetLyThuyet() && !td.duLyThuyet()) {
            lyDo.add("Thiếu giờ lý thuyết (" + td.gioLyThuyetDaHoc() + "/" + td.gioLyThuyetQuyDinh() + ")");
        }
        if (yeuCauHetNo && cn.conNo().signum() > 0) {
            lyDo.add("Còn nợ học phí " + cn.conNo().toPlainString() + "đ");
        }
        return new KetQuaXet(dk.getId(), dk.getMaHoSo(), dk.getHocVien().getHoTen(), dk.getHinhThucLyThuyet().name(),
                td.gioLyThuyetDaHoc(), td.gioLyThuyetQuyDinh(), td.gioThucHanhDaHoc(), td.gioThucHanhQuyDinh(),
                cn.conNo(), lyDo.isEmpty(), lyDo);
    }
}
