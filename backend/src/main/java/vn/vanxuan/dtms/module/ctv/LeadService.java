package vn.vanxuan.dtms.module.ctv;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.PageResponse;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.danhmuc.CongTacVienRepository;
import vn.vanxuan.dtms.module.danhmuc.HangGplxRepository;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.hocvien.HocVien;
import vn.vanxuan.dtms.module.hocvien.HocVienRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/**
 * Lead (khach tiem nang). Chong trung theo SDT: neu SDT da co lead con hieu luc hoac da la hoc vien thi bao trung
 * va giu nguyen nguoi gioi thieu dau tien; admin co the quyet dinh giao lai lead cho CTV khac.
 * CTV / hoc vien chi thay thong bao trung chung chung (khong lo thong tin CTV khac).
 */
@Service
public class LeadService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LeadKhachRepository repo;
    private final CongTacVienRepository ctvRepo;
    private final HocVienRepository hocVienRepo;
    private final DangKyRepository dangKyRepo;
    private final HangGplxRepository hangRepo;
    private final NguoiDungRepository nguoiDungRepo;
    private final CtvService ctvService;
    private final ThongBaoService thongBao;
    private final NhatKyService nhatKy;

    public LeadService(LeadKhachRepository repo, CongTacVienRepository ctvRepo, HocVienRepository hocVienRepo,
                       DangKyRepository dangKyRepo, HangGplxRepository hangRepo, NguoiDungRepository nguoiDungRepo,
                       CtvService ctvService, ThongBaoService thongBao, NhatKyService nhatKy) {
        this.repo = repo;
        this.ctvRepo = ctvRepo;
        this.hocVienRepo = hocVienRepo;
        this.dangKyRepo = dangKyRepo;
        this.hangRepo = hangRepo;
        this.nguoiDungRepo = nguoiDungRepo;
        this.ctvService = ctvService;
        this.thongBao = thongBao;
        this.nhatKy = nhatKy;
    }

    // ------------------------------------------------------------------ chong trung
    /** Mo ta trung (cho nhan vien) hoac rong neu SDT chua co lead / hoc vien. */
    @Transactional(readOnly = true)
    public Optional<String> kiemTraTrung(String sdt) {
        List<LeadKhach> ds = repo.leadConHieuLuc(sdt);
        if (!ds.isEmpty()) {
            LeadKhach l = ds.get(0);
            return Optional.of("SĐT " + sdt + " đã là lead " + moTaNguon(l) + " từ ngày "
                    + l.getCreatedAt().format(NGAY) + " (" + l.getTrangThai() + ")");
        }
        return hocVienRepo.findFirstBySoDienThoai(sdt)
                .map(hv -> "SĐT " + sdt + " đã là học viên của trung tâm: " + hv.getHoTen() + " (" + hv.getMaHocVien() + ")");
    }

    // ------------------------------------------------------------------ tao lead
    /** Le tan / admin nhap lead (thay CTV gui qua Zalo, form giay, hoac khach tu den). */
    @Transactional
    public LeadResponse taoBoiNhanVien(LeadRequest req, AuthUser user) {
        kiemTraTrung(req.soDienThoai()).ifPresent(m -> {
            throw new BusinessException("LEAD_TRUNG", m + ". Hệ thống giữ người giới thiệu đầu tiên");
        });
        LeadKhach l = moi(req, user);
        LeadKhach.Nguon nguon = req.nguon() == null ? LeadKhach.Nguon.VAN_PHONG : req.nguon();
        l.setNguon(nguon);
        if (nguon == LeadKhach.Nguon.CTV) {
            if (req.ctvId() == null) throw new BusinessException("THIEU_CTV", "Chọn CTV giới thiệu từ danh sách");
            CongTacVien ctv = ctvService.lay(req.ctvId());
            if (!ctv.dangHoatDong()) {
                throw new BusinessException("CTV_KHONG_HOAT_DONG", "CTV " + ctv.getHoTen() + " đang " + ctv.getTrangThai());
            }
            l.setCtvId(ctv.getId());
        } else if (nguon == LeadKhach.Nguon.HOC_VIEN) {
            if (req.hocVienGioiThieuId() == null) throw new BusinessException("THIEU_HOC_VIEN", "Chọn học viên giới thiệu");
            hocVienRepo.findById(req.hocVienGioiThieuId())
                    .orElseThrow(() -> new NotFoundException("học viên", req.hocVienGioiThieuId()));
            l.setHocVienGioiThieuId(req.hocVienGioiThieuId());
        }
        repo.save(l);
        return response(l, true);
    }

    /** CTV (tai khoan CTV) hoac hoc vien gui SDT nguoi quen. Hoc vien la CTV dang hoat dong thi tinh nhu lead cua CTV. */
    @Transactional
    public LeadResponse taoBoiCtvHoacHocVien(LeadRequest req, AuthUser user) {
        if (kiemTraTrung(req.soDienThoai()).isPresent()) {
            throw new BusinessException("LEAD_TRUNG",
                    "Số điện thoại này đã được giới thiệu trước đó hoặc đã là học viên của trung tâm");
        }
        LeadKhach l = moi(req, user);
        Optional<CongTacVien> ctv = ctvService.cuaNguoiDung(user).filter(CongTacVien::dangHoatDong);
        if (ctv.isPresent()) {
            l.setNguon(LeadKhach.Nguon.CTV);
            l.setCtvId(ctv.get().getId());
        } else if (user.la(VaiTro.HOC_VIEN)) {
            HocVien hv = hocVienRepo.findByNguoiDungId(user.id())
                    .orElseThrow(() -> new BusinessException("KHONG_PHAI_HOC_VIEN", "Không tìm thấy hồ sơ học viên"));
            l.setNguon(LeadKhach.Nguon.HOC_VIEN);
            l.setHocVienGioiThieuId(hv.getId());
        } else {
            throw new BusinessException("CTV_KHONG_HOAT_DONG", "Tài khoản CTV chưa hoạt động hoặc đang bị khóa",
                    HttpStatus.FORBIDDEN);
        }
        repo.save(l);
        thongBao.guiTheoVaiTro(List.of(VaiTro.LE_TAN, VaiTro.ADMIN), ThongBaoService.HO_SO,
                "Lead mới: " + l.getHoTen() + " – " + l.getSoDienThoai(),
                "Giới thiệu bởi " + moTaNguon(l) + (l.getHangMuonHoc() == null ? "" : ", muốn học hạng " + l.getHangMuonHoc())
                        + ". Gọi tư vấn và cập nhật trạng thái lead.", "/ctv?tab=lead", null);
        return response(l, false);
    }

    private LeadKhach moi(LeadRequest req, AuthUser user) {
        if (req.hangMuonHoc() != null && !req.hangMuonHoc().isBlank() && !hangRepo.existsById(req.hangMuonHoc())) {
            throw new BusinessException("HANG_KHONG_HOP_LE", "Hạng " + req.hangMuonHoc() + " không có trong danh mục");
        }
        LeadKhach l = new LeadKhach();
        l.setHoTen(req.hoTen().trim());
        l.setSoDienThoai(req.soDienThoai());
        l.setDiaChi(rong(req.diaChi()));
        l.setHangMuonHoc(rong(req.hangMuonHoc()));
        l.setGhiChu(rong(req.ghiChu()));
        l.setNguoiNhapId(user.id());
        return l;
    }

    // ------------------------------------------------------------------ xu ly lead
    @Transactional
    public LeadResponse doiTrangThai(Long id, LeadKhach.TrangThai moi, AuthUser user) {
        LeadKhach l = lay(id);
        if (moi == LeadKhach.TrangThai.DA_CHOT) {
            throw new BusinessException("CHOT_QUA_HO_SO", "Lead chỉ chuyển \"Đã chốt\" khi tạo hồ sơ đăng ký từ lead");
        }
        if (l.getTrangThai() == LeadKhach.TrangThai.DA_CHOT) {
            throw new BusinessException("LEAD_DA_CHOT", "Lead đã chốt thành hồ sơ, không đổi trạng thái được");
        }
        l.setTrangThai(moi);
        return response(l, true);
    }

    /** "Admin quyet dinh" khi tranh chap: giao lead cho CTV khac (truoc khi chot). */
    @Transactional
    public LeadResponse doiCtv(Long id, Long ctvId, AuthUser admin) {
        LeadKhach l = lay(id);
        if (l.getTrangThai() == LeadKhach.TrangThai.DA_CHOT) {
            throw new BusinessException("LEAD_DA_CHOT", "Lead đã chốt: đổi CTV ở hồ sơ đăng ký (Gắn CTV)");
        }
        CongTacVien ctv = ctvService.lay(ctvId);
        if (!ctv.dangHoatDong()) throw new BusinessException("CTV_KHONG_HOAT_DONG", "CTV " + ctv.getHoTen() + " không hoạt động");
        nhatKy.ghi(admin.id(), "GIAO_LAI_LEAD", "lead_khach", id,
                Map.of("tuCtv", String.valueOf(l.getCtvId()), "sangCtv", ctvId));
        l.setNguon(LeadKhach.Nguon.CTV);
        l.setCtvId(ctvId);
        l.setHocVienGioiThieuId(null);
        return response(l, true);
    }

    /**
     * Goi tu DangKyService khi tiep nhan ho so: tra ve lead con hieu luc cua SDT (neu co) de gan nguon dung nguoi
     * gioi thieu dau tien. Ho so tu tiep nhan khong duoc "bo qua" lead cua CTV.
     */
    @Transactional(readOnly = true)
    public Optional<LeadKhach> leadChuaChot(String sdt) {
        return repo.leadConHieuLuc(sdt).stream()
                .filter(l -> l.getTrangThai() != LeadKhach.TrangThai.DA_CHOT).findFirst();
    }

    @Transactional
    public void danhDauDaChot(LeadKhach l, Long dangKyId) {
        l.setTrangThai(LeadKhach.TrangThai.DA_CHOT);
        l.setDangKyId(dangKyId);
        repo.save(l);
    }

    // ------------------------------------------------------------------ tra cuu
    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> timKiem(Long ctvId, LeadKhach.TrangThai trangThai, String q, int page, int size) {
        String tuKhoa = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(repo.timKiem(ctvId, trangThai, tuKhoa, PageRequest.of(page, Math.min(size, 100))),
                l -> response(l, true));
    }

    /** Lead cua CTV / hoc vien dang dang nhap - chi cua minh. */
    @Transactional(readOnly = true)
    public List<LeadResponse> cuaToi(AuthUser user) {
        Optional<CongTacVien> ctv = ctvService.cuaNguoiDung(user);
        if (ctv.isPresent()) return repo.findByCtvIdOrderByIdDesc(ctv.get().getId()).stream().map(l -> response(l, false)).toList();
        if (user.la(VaiTro.HOC_VIEN)) {
            return hocVienRepo.findByNguoiDungId(user.id())
                    .map(hv -> repo.findByHocVienGioiThieuIdOrderByIdDesc(hv.getId()).stream().map(l -> response(l, false)).toList())
                    .orElse(List.of());
        }
        return List.of();
    }

    public LeadKhach lay(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("lead", id));
    }

    private String moTaNguon(LeadKhach l) {
        return switch (l.getNguon()) {
            case CTV -> "của CTV " + ctvRepo.findById(l.getCtvId()).map(CongTacVien::getHoTen).orElse("?");
            case HOC_VIEN -> "do học viên " + hocVienRepo.findById(l.getHocVienGioiThieuId()).map(HocVien::getHoTen).orElse("?")
                    + " giới thiệu";
            case VAN_PHONG -> "văn phòng";
        };
    }

    /** day du = nhan vien xem; CTV / hoc vien chi thay thong tin co ban, khong thay ma ho so / nguoi nhap. */
    LeadResponse response(LeadKhach l, boolean dayDu) {
        String tenCtv = l.getCtvId() == null ? null : ctvRepo.findById(l.getCtvId()).map(CongTacVien::getHoTen).orElse(null);
        String maHoSo = !dayDu || l.getDangKyId() == null ? null
                : dangKyRepo.findById(l.getDangKyId()).map(DangKy::getMaHoSo).orElse(null);
        String nguoiNhap = !dayDu || l.getNguoiNhapId() == null ? null
                : nguoiDungRepo.findById(l.getNguoiNhapId()).map(NguoiDung::getHoTen).orElse(null);
        return new LeadResponse(l.getId(), l.getHoTen(), l.getSoDienThoai(), l.getDiaChi(), l.getHangMuonHoc(),
                l.getGhiChu(), l.getNguon().name(), l.getCtvId(), tenCtv, l.getHocVienGioiThieuId(),
                l.getTrangThai().name(), dayDu ? l.getDangKyId() : null, maHoSo, nguoiNhap, l.getCreatedAt());
    }

    private static String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
