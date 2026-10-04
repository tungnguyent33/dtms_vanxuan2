package vn.vanxuan.dtms.module.hocvien;

import org.springframework.http.HttpStatus;
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
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;
import vn.vanxuan.dtms.module.ctv.HoaHongService;
import vn.vanxuan.dtms.module.ctv.LeadKhach;
import vn.vanxuan.dtms.module.ctv.LeadService;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;

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
    private final ThongBaoService thongBao;
    private final LeadService leadService;
    private final HoaHongService hoaHong;

    public DangKyService(DangKyRepository dangKyRepo, HocVienRepository hocVienRepo, KhoaDaoTaoRepository khoaRepo,
                         CongTacVienRepository ctvRepo, NguoiDungRepository nguoiDungRepo,
                         PhieuThuRepository phieuThuRepo, SoThuTuService soThuTu, NhatKyService nhatKy,
                         ThongBaoService thongBao, LeadService leadService, HoaHongService hoaHong) {
        this.dangKyRepo = dangKyRepo;
        this.hocVienRepo = hocVienRepo;
        this.khoaRepo = khoaRepo;
        this.ctvRepo = ctvRepo;
        this.nguoiDungRepo = nguoiDungRepo;
        this.phieuThuRepo = phieuThuRepo;
        this.soThuTu = soThuTu;
        this.nhatKy = nhatKy;
        this.thongBao = thongBao;
        this.leadService = leadService;
        this.hoaHong = hoaHong;
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
        LeadKhach lead = xacDinhNguon(dk, req, hv);
        dk.setHocPhi(khoa.getHocPhi());
        dk.setGiamTru(giamTru);
        dk.setLyDoGiamTru(req.lyDoGiamTru());
        dk.setGhiChu(req.ghiChu());
        dk.setTrangThai(khoa.getTrangThai() == KhoaDaoTao.TrangThai.DANG_DAO_TAO
                ? DangKy.TrangThai.DANG_HOC : DangKy.TrangThai.DA_TIEP_NHAN);
        dk.setNguoiTiepNhan(nguoiDungRepo.getReferenceById(leTan.id()));
        dk.setMaHoSo(capMaHoSo(khoa.getHang()));
        dangKyRepo.save(dk);
        if (lead != null) leadService.danhDauDaChot(lead, dk.getId());
        hoaHong.tinhChoDangKy(dk);
        return DangKyResponse.of(dk);
    }

    /**
     * Nguon / CTV cua ho so. Neu SDT da co lead chua chot thi lead thang (ghi nhan nguoi gioi thieu dau tien):
     * le tan chon CTV khac ma khong tao tu lead -> bao loi de tranh "cuop" hoa hong; admin giao lai lead neu can.
     */
    private LeadKhach xacDinhNguon(DangKy dk, TaoDangKyRequest req, HocVien hv) {
        LeadKhach lead;
        if (req.leadId() != null) {
            lead = leadService.lay(req.leadId());
            if (lead.getTrangThai() == LeadKhach.TrangThai.DA_CHOT) {
                throw new BusinessException("LEAD_DA_CHOT", "Lead này đã được chốt thành hồ sơ khác");
            }
        } else {
            lead = leadService.leadChuaChot(hv.getSoDienThoai()).orElse(null);
            if (lead != null && lead.getNguon() == LeadKhach.Nguon.CTV
                    && (req.nguon() != DangKy.Nguon.CTV || !lead.getCtvId().equals(req.ctvId()))) {
                CongTacVien ctvLead = ctvRepo.findById(lead.getCtvId()).orElseThrow();
                throw new BusinessException("LEAD_CUA_CTV_KHAC", "SĐT " + hv.getSoDienThoai() + " đã được CTV "
                        + ctvLead.getHoTen() + " giới thiệu trước. Tạo hồ sơ từ lead đó, hoặc nhờ quản trị viên giao lại lead");
            }
        }
        if (lead != null && lead.getNguon() == LeadKhach.Nguon.CTV) {
            dk.setNguon(DangKy.Nguon.CTV);
            dk.setCtv(ctvHoatDong(lead.getCtvId()));
        } else if (lead != null && lead.getNguon() == LeadKhach.Nguon.HOC_VIEN) {
            dk.setNguon(DangKy.Nguon.HOC_VIEN_GIOI_THIEU);
            dk.setGioiThieuHocVienId(lead.getHocVienGioiThieuId());
        } else {
            if (req.nguon() == DangKy.Nguon.HOC_VIEN_GIOI_THIEU) {
                throw new BusinessException("NGUON_KHONG_HOP_LE", "Nguồn \"học viên giới thiệu\" chỉ tạo từ lead do học viên gửi");
            }
            dk.setNguon(req.nguon());
            if (req.nguon() == DangKy.Nguon.CTV) dk.setCtv(ctvHoatDong(req.ctvId()));
        }
        return lead;
    }

    private CongTacVien ctvHoatDong(Long ctvId) {
        CongTacVien c = ctvRepo.findById(ctvId).orElseThrow(() -> new NotFoundException("cộng tác viên", ctvId));
        if (!c.dangHoatDong()) {
            throw new BusinessException("CTV_KHONG_HOAT_DONG", "CTV " + c.getHoTen() + " chưa được duyệt hoặc đang bị khóa");
        }
        return c;
    }

    /** Gan CTV cho ho so da co. Le tan gan khi ho so chua co CTV; doi CTV da gan chi quan tri vien. */
    @Transactional
    public DangKyResponse ganCtv(Long id, Long ctvId, AuthUser user) {
        DangKy dk = layChiTiet(id);
        if (dk.getTrangThai() == DangKy.TrangThai.DA_HUY) {
            throw new BusinessException("HO_SO_DA_HUY", "Hồ sơ đã hủy");
        }
        boolean laAdmin = user.la(VaiTro.ADMIN);
        if (dk.getCtv() != null && !laAdmin) {
            throw new BusinessException("CHI_ADMIN_DOI_CTV", "Hồ sơ đã gắn CTV " + dk.getCtv().getHoTen()
                    + ". Chỉ quản trị viên được đổi CTV", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        CongTacVien moi = ctvHoatDong(ctvId);
        var lead = leadService.leadChuaChot(dk.getHocVien().getSoDienThoai()).orElse(null);
        if (lead != null && lead.getNguon() == LeadKhach.Nguon.CTV && !lead.getCtvId().equals(ctvId) && !laAdmin) {
            throw new BusinessException("LEAD_CUA_CTV_KHAC", "SĐT học viên đã là lead của một CTV khác. Nhờ quản trị viên quyết định");
        }
        Long cu = dk.getCtv() == null ? null : dk.getCtv().getId();
        dk.setCtv(moi);
        dk.setNguon(DangKy.Nguon.CTV);
        hoaHong.tinhChoDangKy(dk);
        if (lead != null) leadService.danhDauDaChot(lead, dk.getId());
        nhatKy.ghi(user.id(), "GAN_CTV", "dang_ky", id, Map.of("tuCtv", String.valueOf(cu), "sangCtv", ctvId));
        return DangKyResponse.of(dk);
    }

    /** Khach tu dang ky tren website: tao ho so CHO_DUYET, le tan duyet sau (UC13, UC15). */
    @Transactional
    public String dangKyTrucTuyen(DangKyTrucTuyenRequest req) {
        KhoaDaoTao khoa = khoaRepo.khoaDeXepHocVien(req.khoaId())
                .orElseThrow(() -> new NotFoundException("khóa đào tạo", req.khoaId()));
        if (khoa.getTrangThai() != KhoaDaoTao.TrangThai.DANG_TUYEN) {
            throw new BusinessException("KHOA_KHONG_TUYEN", "Khóa này hiện không nhận đăng ký");
        }
        kiemTraSiSo(khoa, NHAN_TRUC_TUYEN);
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
        LeadKhach lead = leadService.leadChuaChot(hv.getSoDienThoai()).orElse(null);
        if (lead != null && lead.getNguon() == LeadKhach.Nguon.CTV) {
            ctvRepo.findById(lead.getCtvId()).filter(CongTacVien::dangHoatDong).ifPresent(c -> {
                dk.setNguon(DangKy.Nguon.CTV);
                dk.setCtv(c);
            });
        } else if (lead != null && lead.getNguon() == LeadKhach.Nguon.HOC_VIEN) {
            dk.setNguon(DangKy.Nguon.HOC_VIEN_GIOI_THIEU);
            dk.setGioiThieuHocVienId(lead.getHocVienGioiThieuId());
        }
        dk.setHocPhi(khoa.getHocPhi());
        dk.setTrangThai(DangKy.TrangThai.CHO_DUYET);
        dk.setMaHoSo(capMaHoSo(khoa.getHang()));
        dangKyRepo.save(dk);
        if (lead != null) leadService.danhDauDaChot(lead, dk.getId());
        hoaHong.tinhChoDangKy(dk);
        thongBao.guiTheoVaiTro(List.of(VaiTro.ADMIN, VaiTro.LE_TAN), ThongBaoService.HO_SO,
                "Đăng ký trực tuyến mới " + dk.getMaHoSo(),
                hv.getHoTen() + " (" + hv.getSoDienThoai() + ") đăng ký khóa " + khoa.getMaKhoa()
                        + ". Gọi điện xác nhận và duyệt hồ sơ.", "/hoc-vien", null);
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
        nhatKy.ghi(leTan.id(), "DUYET_DANG_KY", "dang_ky", dk.getId(), null);
        hoaHong.capNhatTrangThai(dk);
        thongBao.gui(dk.getHocVien().nguoiDungId(), ThongBaoService.HO_SO, "Hồ sơ " + dk.getMaHoSo() + " đã được duyệt",
                "Khóa " + dk.getKhoa().getMaKhoa() + " khai giảng ngày " + dk.getKhoa().getNgayKhaiGiang() + ".",
                "/hoc-tap", null);
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
        hoaHong.capNhatTrangThai(dk);
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
        kiemTraSiSo(khoa, NHAN_TAI_QUAY);
    }

    /** Trang thai khoa duoc goi y: le tan xep duoc ca khoa dang dao tao, khach chi dang ky khoa dang tuyen. */
    private static final Set<KhoaDaoTao.TrangThai> NHAN_TAI_QUAY =
            EnumSet.of(KhoaDaoTao.TrangThai.DANG_TUYEN, KhoaDaoTao.TrangThai.DANG_DAO_TAO);
    private static final Set<KhoaDaoTao.TrangThai> NHAN_TRUC_TUYEN = EnumSet.of(KhoaDaoTao.TrangThai.DANG_TUYEN);

    /** BR-04; UC05 - 5b: khoa du si so thi goi y khoa som nhat cung hang con cho (details.goiYKhoaId...). */
    private void kiemTraSiSo(KhoaDaoTao khoa, Set<KhoaDaoTao.TrangThai> trangThaiGoiY) {
        long daCo = dangKyRepo.countByKhoaIdAndTrangThaiNot(khoa.getId(), DangKy.TrangThai.DA_HUY);
        if (daCo < khoa.getSiSoToiDa()) return;
        String thongBao = "Khóa " + khoa.getMaKhoa() + " đã đủ " + khoa.getSiSoToiDa() + " học viên";
        for (KhoaDaoTao k : khoaRepo.khoaCungHang(khoa.getHang().getMa(), khoa.getId(), trangThaiGoiY, LocalDate.now())) {
            long conCho = k.getSiSoToiDa() - dangKyRepo.countByKhoaIdAndTrangThaiNot(k.getId(), DangKy.TrangThai.DA_HUY);
            if (conCho > 0) {
                throw new BusinessException("KHOA_DU_SI_SO", thongBao + ". Gợi ý: khóa " + k.getMaKhoa()
                        + " (khai giảng " + k.getNgayKhaiGiang() + ", còn " + conCho + " chỗ)",
                        HttpStatus.UNPROCESSABLE_ENTITY, Map.of(
                        "goiYKhoaId", k.getId().toString(), "goiYMaKhoa", k.getMaKhoa(),
                        "goiYKhaiGiang", k.getNgayKhaiGiang().toString(), "goiYConCho", Long.toString(conCho)));
            }
        }
        throw new BusinessException("KHOA_DU_SI_SO", thongBao + ". Hiện chưa có khóa cùng hạng còn chỗ");
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
            // Tai khoan cong hoc vien KHONG tao o day: le tan cap mat khau tam sau khi doi chieu giay to
            // (HocVienController#capTaiKhoan) - tranh khach la tao tai khoan qua form dang ky cong khai.
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
