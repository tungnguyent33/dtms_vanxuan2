package vn.vanxuan.dtms.module.hocvien;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.PageResponse;
import vn.vanxuan.dtms.common.SoThuTuService;
import vn.vanxuan.dtms.module.danhmuc.CongTacVienRepository;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;
import vn.vanxuan.dtms.module.hocphi.PhieuThuRepository;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTaoRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static vn.vanxuan.dtms.module.hocvien.DangKyDtos.*;

/**
 * Nghiep vu tiep nhan ho so va quan ly dang ky hoc (UC05, UC15).
 * Quy tac: BR-01 (CCCD duy nhat), BR-02 (du tuoi), BR-04 (si so).
 */
@Service
public class DangKyService {

    private static final Set<DangKy.TrangThai> DUOC_HUY = EnumSet.of(
            DangKy.TrangThai.CHO_DUYET, DangKy.TrangThai.DA_TIEP_NHAN,
            DangKy.TrangThai.DANG_HOC, DangKy.TrangThai.CHUA_DAT);

    private final DangKyRepository dangKyRepo;
    private final HocVienRepository hocVienRepo;
    private final KhoaDaoTaoRepository khoaRepo;
    private final CongTacVienRepository ctvRepo;
    private final NguoiDungRepository nguoiDungRepo;
    private final PhieuThuRepository phieuThuRepo;
    private final SoThuTuService soThuTu;
    private final NhatKyService nhatKy;

    public DangKyService(DangKyRepository dangKyRepo, HocVienRepository hocVienRepo, KhoaDaoTaoRepository khoaRepo,
                         CongTacVienRepository ctvRepo, NguoiDungRepository nguoiDungRepo,
                         PhieuThuRepository phieuThuRepo, SoThuTuService soThuTu, NhatKyService nhatKy) {
        this.dangKyRepo = dangKyRepo;
        this.hocVienRepo = hocVienRepo;
        this.khoaRepo = khoaRepo;
        this.ctvRepo = ctvRepo;
        this.nguoiDungRepo = nguoiDungRepo;
        this.phieuThuRepo = phieuThuRepo;
        this.soThuTu = soThuTu;
        this.nhatKy = nhatKy;
    }

    // ------------------------------------------------------------------ tiep nhan
    @Transactional
    public DangKyResponse taoDangKy(TaoDangKyRequest req, AuthUser leTan) {
        // Khoa dong khoa hoc den het transaction -> 2 le tan xep cung luc khong vuot si so
        KhoaDaoTao khoa = khoaRepo.khoaDeXepHocVien(req.khoaId())
                .orElseThrow(() -> new NotFoundException("khóa đào tạo", req.khoaId()));
        kiemTraKhoaNhanHocVien(khoa);

        BigDecimal giamTru = req.giamTru() == null ? BigDecimal.ZERO : req.giamTru();
        if (giamTru.compareTo(khoa.getHocPhi()) > 0) {
            throw new BusinessException("GIAM_TRU_KHONG_HOP_LE", "Giảm trừ không được lớn hơn học phí");
        }
        if (giamTru.signum() > 0 && (req.lyDoGiamTru() == null || req.lyDoGiamTru().isBlank())) {
            throw new BusinessException("THIEU_LY_DO_GIAM_TRU", "Cần nhập lý do giảm trừ học phí");
        }
        if (req.nguon() == DangKy.Nguon.CTV && req.ctvId() == null) {
            throw new BusinessException("THIEU_CTV", "Nguồn CTV phải chọn cộng tác viên giới thiệu");
        }

        HocVien hv = timHoacTaoHocVien(req.hocVien(), true);
        kiemTraTuoi(hv, khoa);
        if (hv.getId() != null && dangKyRepo.existsByHocVienIdAndKhoaId(hv.getId(), khoa.getId())) {
            throw new BusinessException("DANG_KY_TRUNG", "Học viên đã có hồ sơ trong khóa " + khoa.getMaKhoa());
        }

        DangKy dk = new DangKy();
        dk.setHocVien(hv);
        dk.setKhoa(khoa);
        dk.setHinhThucLyThuyet(req.hinhThucLyThuyet());
        dk.setNguon(req.nguon());
        if (req.ctvId() != null) {
            dk.setCtv(ctvRepo.findById(req.ctvId()).orElseThrow(() -> new NotFoundException("cộng tác viên", req.ctvId())));
        }
        dk.setHocPhi(khoa.getHocPhi());
        dk.setGiamTru(giamTru);
        dk.setLyDoGiamTru(req.lyDoGiamTru());
        dk.setGhiChu(req.ghiChu());
        dk.setTrangThai(khoa.getTrangThai() == KhoaDaoTao.TrangThai.DANG_DAO_TAO
                ? DangKy.TrangThai.DANG_HOC : DangKy.TrangThai.DA_TIEP_NHAN);
        dk.setNguoiTiepNhan(nguoiDungRepo.getReferenceById(leTan.id()));
        dk.setMaHoSo(capMaHoSo(khoa.getHang()));
        return DangKyResponse.of(dangKyRepo.save(dk));
    }

    /** Khach tu dang ky tren website: tao ho so CHO_DUYET, le tan duyet sau (UC13, UC15). */
    @Transactional
    public String dangKyTrucTuyen(DangKyTrucTuyenRequest req) {
        KhoaDaoTao khoa = khoaRepo.khoaDeXepHocVien(req.khoaId())
                .orElseThrow(() -> new NotFoundException("khóa đào tạo", req.khoaId()));
        if (khoa.getTrangThai() != KhoaDaoTao.TrangThai.DANG_TUYEN) {
            throw new BusinessException("KHOA_KHONG_TUYEN", "Khóa này hiện không nhận đăng ký");
        }
        kiemTraSiSo(khoa);
        // Khong ghi de thong tin hoc vien da co (tranh nguoi la sua ho so bang CCCD cua nguoi khac)
        HocVien hv = timHoacTaoHocVien(req.hocVien(), false);
        kiemTraTuoi(hv, khoa);
        if (hv.getId() != null && dangKyRepo.existsByHocVienIdAndKhoaId(hv.getId(), khoa.getId())) {
            throw new BusinessException("DANG_KY_TRUNG", "Bạn đã đăng ký khóa này, trung tâm sẽ liên hệ lại");
        }
        DangKy dk = new DangKy();
        dk.setHocVien(hv);
        dk.setKhoa(khoa);
        dk.setHinhThucLyThuyet(req.hinhThucLyThuyet());
        dk.setNguon(DangKy.Nguon.TRUC_TUYEN);
        dk.setHocPhi(khoa.getHocPhi());
        dk.setTrangThai(DangKy.TrangThai.CHO_DUYET);
        dk.setMaHoSo(capMaHoSo(khoa.getHang()));
        dangKyRepo.save(dk);
        return dk.getMaHoSo();
    }

    @Transactional
    public DangKyResponse duyet(Long id, AuthUser leTan) {
        DangKy dk = layChiTiet(id);
        if (dk.getTrangThai() != DangKy.TrangThai.CHO_DUYET) {
            throw new BusinessException("TRANG_THAI_KHONG_HOP_LE", "Chỉ duyệt được hồ sơ đang chờ duyệt");
        }
        dk.setNguoiTiepNhan(nguoiDungRepo.getReferenceById(leTan.id()));
        dk.setTrangThai(dk.getKhoa().getTrangThai() == KhoaDaoTao.TrangThai.DANG_DAO_TAO
                ? DangKy.TrangThai.DANG_HOC : DangKy.TrangThai.DA_TIEP_NHAN);
        return DangKyResponse.of(dk);
    }

    @Transactional
    public DangKyResponse huy(Long id, String lyDo, AuthUser user) {
        DangKy dk = layChiTiet(id);
        if (!DUOC_HUY.contains(dk.getTrangThai())) {
            throw new BusinessException("TRANG_THAI_KHONG_HOP_LE", "Không thể hủy hồ sơ ở trạng thái " + dk.getTrangThai());
        }
        if (phieuThuRepo.tongDaThu(dk.getId()).signum() > 0) {
            throw new BusinessException("DA_DONG_TIEN",
                    "Học viên đã đóng học phí. Hãy hủy/hoàn phiếu thu trước khi hủy hồ sơ");
        }
        dk.setTrangThai(DangKy.TrangThai.DA_HUY);
        dk.setGhiChu(lyDo);
        nhatKy.ghi(user.id(), "HUY_DANG_KY", "dang_ky", dk.getId(), Map.of("lyDo", lyDo));
        return DangKyResponse.of(dk);
    }

    @Transactional
    public DangKyResponse chuyenKhoa(Long id, Long khoaMoiId, AuthUser user) {
        DangKy dk = layChiTiet(id);
        if (dk.getTrangThai() != DangKy.TrangThai.DA_TIEP_NHAN && dk.getTrangThai() != DangKy.TrangThai.CHUA_DAT) {
            throw new BusinessException("TRANG_THAI_KHONG_HOP_LE", "Chỉ chuyển khóa khi hồ sơ chưa học hoặc chưa đạt");
        }
        KhoaDaoTao moi = khoaRepo.khoaDeXepHocVien(khoaMoiId)
                .orElseThrow(() -> new NotFoundException("khóa đào tạo", khoaMoiId));
        if (!moi.getHang().getMa().equals(dk.getKhoa().getHang().getMa())) {
            throw new BusinessException("KHAC_HANG", "Chỉ chuyển sang khóa cùng hạng");
        }
        kiemTraKhoaNhanHocVien(moi);
        if (dangKyRepo.existsByHocVienIdAndKhoaId(dk.getHocVien().getId(), moi.getId())) {
            throw new BusinessException("DANG_KY_TRUNG", "Học viên đã có hồ sơ trong khóa " + moi.getMaKhoa());
        }
        Long khoaCu = dk.getKhoa().getId();
        dk.setKhoa(moi);
        dk.setTrangThai(moi.getTrangThai() == KhoaDaoTao.TrangThai.DANG_DAO_TAO
                ? DangKy.TrangThai.DANG_HOC : DangKy.TrangThai.DA_TIEP_NHAN);
        nhatKy.ghi(user.id(), "CHUYEN_KHOA", "dang_ky", dk.getId(), Map.of("tuKhoa", khoaCu, "denKhoa", moi.getId()));
        return DangKyResponse.of(dk);
    }

    // ------------------------------------------------------------------ tra cuu
    @Transactional(readOnly = true)
    public PageResponse<DangKyResponse> timKiem(Long khoaId, DangKy.TrangThai trangThai, String q, int page, int size) {
        String tuKhoa = (q == null || q.isBlank()) ? null : q.trim();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "ngayDangKy"));
        return PageResponse.of(dangKyRepo.timKiem(khoaId, trangThai, tuKhoa, pageable), DangKyResponse::of);
    }

    @Transactional(readOnly = true)
    public DangKyResponse chiTiet(Long id) {
        return DangKyResponse.of(layChiTiet(id));
    }

    @Transactional(readOnly = true)
    public List<DangKyResponse> hoSoCuaToi(AuthUser user) {
        if (user.la(vn.vanxuan.dtms.module.nguoidung.VaiTro.GIAO_VIEN)) {
            return dangKyRepo.hocVienCuaGiaoVien(user.id(), DangKy.TrangThai.DA_HUY).stream()
                    .map(DangKyResponse::of).toList();
        } else if (user.la(vn.vanxuan.dtms.module.nguoidung.VaiTro.HOC_VIEN)) {
            return dangKyRepo.findByHocVienNguoiDungId(user.id()).stream()
                    .map(DangKyResponse::of).toList();
        }
        return java.util.List.of();
    }

    // ------------------------------------------------------------------ ho tro
    DangKy layChiTiet(Long id) {
        return dangKyRepo.findChiTiet(id).orElseThrow(() -> new NotFoundException("hồ sơ đăng ký", id));
    }

    private void kiemTraKhoaNhanHocVien(KhoaDaoTao khoa) {
        if (khoa.getTrangThai() != KhoaDaoTao.TrangThai.DANG_TUYEN
                && khoa.getTrangThai() != KhoaDaoTao.TrangThai.DANG_DAO_TAO) {
            throw new BusinessException("KHOA_KHONG_TUYEN",
                    "Khóa " + khoa.getMaKhoa() + " không nhận học viên (trạng thái " + khoa.getTrangThai() + ")");
        }
        kiemTraSiSo(khoa);
    }

    private void kiemTraSiSo(KhoaDaoTao khoa) {
        long daCo = dangKyRepo.countByKhoaIdAndTrangThaiNot(khoa.getId(), DangKy.TrangThai.DA_HUY);
        if (daCo >= khoa.getSiSoToiDa()) {
            throw new BusinessException("KHOA_DU_SI_SO", "Khóa " + khoa.getMaKhoa() + " đã đủ " + khoa.getSiSoToiDa() + " học viên");
        }
    }

    /**
     * BR-02: du tuoi toi thieu cua hang. Ngay du kien sat hach duoc lay xap xi bang
     * ngay be giang cua khoa (hoc vien chi du thi sau khi hoan thanh khoa).
     */
    private void kiemTraTuoi(HocVien hv, KhoaDaoTao khoa) {
        int tuoiToiThieu = khoa.getHang().getTuoiToiThieu();
        LocalDate ngayDuKien = khoa.getNgayBeGiang();
        if (hv.tuoiTai(ngayDuKien) < tuoiToiThieu) {
            throw new BusinessException("HV_CHUA_DU_TUOI",
                    "Học viên chưa đủ " + tuoiToiThieu + " tuổi tại ngày " + ngayDuKien + " (dự kiến sát hạch)");
        }
    }

    private HocVien timHoacTaoHocVien(HocVienInput in, boolean capNhatNeuDaCo) {
        HocVien hv = hocVienRepo.findByCccd(in.cccd()).orElse(null);
        if (hv == null) {
            hv = new HocVien();
            hv.setCccd(in.cccd());
            hv.setMaHocVien(soThuTu.capMa("HV" + LocalDate.now().getYear(), "HV" + LocalDate.now().getYear(), 6));
            ganThongTin(hv, in);
            
            vn.vanxuan.dtms.module.nguoidung.NguoiDung nd = new vn.vanxuan.dtms.module.nguoidung.NguoiDung();
            nd.setTenDangNhap(in.cccd());
            nd.setMatKhauHash("$2a$10$BT8/E6B2jJgsawzdvcvuzuc0FJ/vr5Qx4pr.RjBpzJo69mUvEJrP2"); // 123456
            nd.setHoTen(in.hoTen().trim());
            nd.setSoDienThoai(in.soDienThoai());
            vn.vanxuan.dtms.module.nguoidung.VaiTro vt = new vn.vanxuan.dtms.module.nguoidung.VaiTro();
            vt.setId(4); // HOC_VIEN
            nd.setVaiTro(vt);
            nd.setTrangThai(true);
            nd = nguoiDungRepo.save(nd);
            hv.setNguoiDung(nd);

            return hocVienRepo.save(hv);
        }
        if (capNhatNeuDaCo) {
            ganThongTin(hv, in);   // le tan doi chieu giay to that nen duoc cap nhat
        }
        return hv;
    }

    private static void ganThongTin(HocVien hv, HocVienInput in) {
        hv.setHoTen(in.hoTen().trim());
        hv.setNgaySinh(in.ngaySinh());
        hv.setGioiTinh(in.gioiTinh());
        hv.setNgayCapCccd(in.ngayCapCccd());
        hv.setDiaChi(in.diaChi().trim());
        hv.setSoDienThoai(in.soDienThoai());
        hv.setEmail(in.email());
    }

    private String capMaHoSo(HangGplx hang) {
        String tienTo = "HS-" + hang.getMa() + "-";
        return soThuTu.capMa("HS-" + hang.getMa(), tienTo, 4);
    }
}
