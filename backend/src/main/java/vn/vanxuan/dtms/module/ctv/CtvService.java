package vn.vanxuan.dtms.module.ctv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.danhmuc.CongTacVienRepository;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.hocvien.HocVien;
import vn.vanxuan.dtms.module.hocvien.HocVienRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService.KetQuaCapMatKhau;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/**
 * Vong doi ho so CTV. Phan quyen:
 * - LE_TAN: tao ho so (CHO_DUYET), sua khi con CHO_DUYET; KHONG doi hang / hoa hong, duyet, khoa.
 * - ADMIN: duyet (khac nguoi tao - "nguoi nhap != nguoi duyet"), dat hang, khoa / mo, sua moi thu.
 * - HOC_VIEN: xin lam CTV (tao yeu cau CHO_DUYET), khong tu kich hoat.
 * Khong ai xoa CTV: ngung dung de giu lich su lead va hoa hong.
 */
@Service
public class CtvService {

    private static final Set<String> KIEU_CAM_KET = Set.of("image/jpeg", "image/png", "application/pdf");

    private final CongTacVienRepository ctvRepo;
    private final LeadKhachRepository leadRepo;
    private final DangKyRepository dangKyRepo;
    private final HocVienRepository hocVienRepo;
    private final NguoiDungRepository nguoiDungRepo;
    private final TaiKhoanService taiKhoan;
    private final ThongBaoService thongBao;
    private final NhatKyService nhatKy;
    private final Path thuMucCamKet;

    public CtvService(CongTacVienRepository ctvRepo, LeadKhachRepository leadRepo, DangKyRepository dangKyRepo,
                      HocVienRepository hocVienRepo, NguoiDungRepository nguoiDungRepo, TaiKhoanService taiKhoan,
                      ThongBaoService thongBao, NhatKyService nhatKy,
                      @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.ctvRepo = ctvRepo;
        this.leadRepo = leadRepo;
        this.dangKyRepo = dangKyRepo;
        this.hocVienRepo = hocVienRepo;
        this.nguoiDungRepo = nguoiDungRepo;
        this.taiKhoan = taiKhoan;
        this.thongBao = thongBao;
        this.nhatKy = nhatKy;
        this.thuMucCamKet = Path.of(uploadDir, "ctv").toAbsolutePath().normalize();
        Files.createDirectories(thuMucCamKet);
    }

    // ------------------------------------------------------------------ tra cuu
    @Transactional(readOnly = true)
    public List<CtvResponse> ds(CongTacVien.TrangThai trangThai, String q, AuthUser user) {
        String tuKhoa = q == null || q.isBlank() ? null : q.trim();
        return ctvRepo.timKiem(trangThai, tuKhoa).stream().map(c -> response(c, user)).toList();
    }

    @Transactional(readOnly = true)
    public CtvResponse chiTiet(Long id, AuthUser user) {
        return response(lay(id), user);
    }

    // ------------------------------------------------------------------ tao / sua
    @Transactional
    public CtvResponse tao(CtvRequest req, AuthUser user) {
        kiemTraTrung(req.soDienThoai(), req.cccd(), null);
        CongTacVien c = new CongTacVien();
        gan(c, req);
        c.setTrangThai(CongTacVien.TrangThai.CHO_DUYET);
        c.setHang(CongTacVien.Hang.THUONG);
        c.setNguoiTaoId(user.id());
        ctvRepo.save(c);
        nhatKy.ghi(user.id(), "TAO_CTV", "cong_tac_vien", c.getId(), Map.of("sdt", c.getSoDienThoai()));
        thongBao.guiTheoVaiTro(List.of(VaiTro.ADMIN), ThongBaoService.HO_SO, "CTV mới chờ duyệt: " + c.getHoTen(),
                c.getSoDienThoai() + " – " + nhanLoai(c.getLoai()) + ". Kiểm tra CCCD, cam kết và duyệt hồ sơ.",
                "/ctv", null);
        return response(c, user);
    }

    /** Le tan chi sua khi ho so con cho duyet; sau khi duyet chi admin sua (dac biet tai khoan ngan hang). */
    @Transactional
    public CtvResponse sua(Long id, CtvRequest req, AuthUser user) {
        CongTacVien c = lay(id);
        if (!user.la(VaiTro.ADMIN) && c.getTrangThai() != CongTacVien.TrangThai.CHO_DUYET) {
            throw new BusinessException("CTV_DA_DUYET", "CTV đã được duyệt, chỉ quản trị viên được sửa hồ sơ",
                    HttpStatus.FORBIDDEN);
        }
        kiemTraTrung(req.soDienThoai(), req.cccd(), id);
        String tkCu = c.getNganHang() + "|" + c.getSoTaiKhoan() + "|" + c.getChuTaiKhoan();
        gan(c, req);
        String tkMoi = c.getNganHang() + "|" + c.getSoTaiKhoan() + "|" + c.getChuTaiKhoan();
        if (!tkCu.equals(tkMoi)) {
            // Doi tai khoan nhan tien la thao tac nhay cam (rui ro chuyen nham / gian lan) -> ghi nhat ky
            nhatKy.ghi(user.id(), "DOI_TK_NGAN_HANG_CTV", "cong_tac_vien", id, Map.of("cu", tkCu, "moi", tkMoi));
        }
        return response(c, user);
    }

    // ------------------------------------------------------------------ admin
    @Transactional
    public CtvResponse duyet(Long id, DuyetCtvRequest req, AuthUser admin) {
        CongTacVien c = lay(id);
        if (c.getTrangThai() != CongTacVien.TrangThai.CHO_DUYET) {
            throw new BusinessException("TRANG_THAI_KHONG_HOP_LE", "Chỉ duyệt được CTV đang chờ duyệt");
        }
        if (admin.id().equals(c.getNguoiTaoId())) {
            throw new BusinessException("NGUOI_NHAP_KHONG_DUYET",
                    "Người nhập hồ sơ không được tự duyệt. Cần một quản trị viên khác duyệt CTV này");
        }
        if (c.getCccd() == null) {
            throw new BusinessException("THIEU_CCCD", "CTV chưa có số CCCD để đối soát khi chi trả");
        }
        if (c.getCamKetUrl() == null) {
            throw new BusinessException("THIEU_CAM_KET", "Chưa tải lên bản cam kết CTV đã ký");
        }
        c.setTrangThai(CongTacVien.TrangThai.HOAT_DONG);
        c.setHang(req.hang());
        c.setNguoiDuyetId(admin.id());
        c.setNgayDuyet(LocalDateTime.now());
        if (req.ngayBatDau() != null) c.setNgayBatDau(req.ngayBatDau());
        if (c.getNgayBatDau() == null) c.setNgayBatDau(LocalDate.now());
        nhatKy.ghi(admin.id(), "DUYET_CTV", "cong_tac_vien", id, Map.of("hang", req.hang().name()));
        guiChoCtv(c, "Bạn đã trở thành cộng tác viên", "Hạng " + req.hang() + ". Bạn có thể gửi thông tin người muốn học "
                + "và theo dõi hoa hồng trong mục Cộng tác viên.");
        thongBao.gui(c.getNguoiTaoId(), ThongBaoService.HO_SO, "CTV " + c.getHoTen() + " đã được duyệt", "Hạng " + req.hang() + ".",
                "/ctv", null);
        return response(c, admin);
    }

    @Transactional
    public CtvResponse tuChoi(Long id, String lyDo, AuthUser admin) {
        CongTacVien c = lay(id);
        if (c.getTrangThai() != CongTacVien.TrangThai.CHO_DUYET) {
            throw new BusinessException("TRANG_THAI_KHONG_HOP_LE", "Chỉ từ chối được CTV đang chờ duyệt");
        }
        c.setTrangThai(CongTacVien.TrangThai.NGUNG);
        String ghiChu = "Từ chối: " + lyDo;
        c.setGhiChu(ghiChu.length() > 255 ? ghiChu.substring(0, 255) : ghiChu);
        nhatKy.ghi(admin.id(), "TU_CHOI_CTV", "cong_tac_vien", id, Map.of("lyDo", lyDo));
        guiChoCtv(c, "Yêu cầu làm CTV chưa được duyệt", lyDo);
        return response(c, admin);
    }

    /** HOAT_DONG <-> TAM_KHOA, -> NGUNG. Khoa thi khoa luon tai khoan CTV rieng (khong dung den tai khoan hoc vien). */
    @Transactional
    public CtvResponse doiTrangThai(Long id, CongTacVien.TrangThai moi, String lyDo, AuthUser admin) {
        CongTacVien c = lay(id);
        if (c.getTrangThai() == CongTacVien.TrangThai.CHO_DUYET || moi == CongTacVien.TrangThai.CHO_DUYET) {
            throw new BusinessException("TRANG_THAI_KHONG_HOP_LE", "CTV chờ duyệt phải được duyệt hoặc từ chối");
        }
        var cu = c.getTrangThai();
        c.setTrangThai(moi);
        if (c.getNguoiDungId() != null) {
            NguoiDung nd = nguoiDungRepo.findById(c.getNguoiDungId()).orElseThrow();
            boolean moTk = moi == CongTacVien.TrangThai.HOAT_DONG;
            if (!Boolean.valueOf(moTk).equals(nd.getTrangThai())) taiKhoan.doiTrangThai(nd, moTk, admin.id());
        }
        nhatKy.ghi(admin.id(), "DOI_TRANG_THAI_CTV", "cong_tac_vien", id,
                Map.of("tu", cu.name(), "sang", moi.name(), "lyDo", lyDo == null ? "" : lyDo));
        return response(c, admin);
    }

    /** Hang chi anh huong dang ky MOI; hoa hong da tinh giu muc cu. */
    @Transactional
    public CtvResponse doiHang(Long id, CongTacVien.Hang hang, AuthUser admin) {
        CongTacVien c = lay(id);
        nhatKy.ghi(admin.id(), "DOI_HANG_CTV", "cong_tac_vien", id, Map.of("tu", c.getHang().name(), "sang", hang.name()));
        c.setHang(hang);
        guiChoCtv(c, "Hạng CTV của bạn: " + hang, "Áp dụng cho học viên giới thiệu từ hôm nay.");
        return response(c, admin);
    }

    /** Tai khoan rieng vai tro CTV (ten dang nhap = SDT). CTV la hoc vien thi dung tai khoan hoc vien. */
    @Transactional
    public KetQuaCapMatKhau capTaiKhoan(Long id, AuthUser admin) {
        CongTacVien c = lay(id);
        if (!c.dangHoatDong()) {
            throw new BusinessException("CTV_CHUA_HOAT_DONG", "Chỉ cấp tài khoản cho CTV đang hoạt động");
        }
        if (c.getHocVienId() != null) {
            throw new BusinessException("CTV_LA_HOC_VIEN",
                    "CTV này là học viên: đăng nhập bằng tài khoản học viên để dùng chức năng cộng tác viên");
        }
        if (c.getNguoiDungId() != null) {
            return taiKhoan.datLaiMatKhau(nguoiDungRepo.findById(c.getNguoiDungId()).orElseThrow(), admin.id());
        }
        KetQuaCapMatKhau kq = taiKhoan.tao(c.getSoDienThoai(), c.getHoTen(), c.getSoDienThoai(), null, VaiTro.CTV,
                admin.id());
        c.setNguoiDungId(kq.nguoiDungId());
        return kq;
    }

    // ------------------------------------------------------------------ cam ket
    @Transactional
    public CtvResponse luuCamKet(Long id, MultipartFile file, AuthUser user) throws IOException {
        CongTacVien c = lay(id);
        if (!user.la(VaiTro.ADMIN) && c.getTrangThai() != CongTacVien.TrangThai.CHO_DUYET) {
            throw new BusinessException("CTV_DA_DUYET", "CTV đã được duyệt, chỉ quản trị viên được thay cam kết",
                    HttpStatus.FORBIDDEN);
        }
        if (file.isEmpty() || !KIEU_CAM_KET.contains(file.getContentType())) {
            throw new BusinessException("TEP_KHONG_HOP_LE", "Chỉ nhận ảnh JPG, PNG hoặc tệp PDF");
        }
        String duoi = switch (Objects.requireNonNull(file.getContentType())) {
            case "image/png" -> ".png";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
        String ten = id + "_cam_ket_" + UUID.randomUUID() + duoi;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, thuMucCamKet.resolve(ten), StandardCopyOption.REPLACE_EXISTING);
        }
        String cu = c.getCamKetUrl();
        c.setCamKetUrl(ten);
        if (cu != null) Files.deleteIfExists(thuMucCamKet.resolve(cu).normalize());
        return response(c, user);
    }

    @Transactional(readOnly = true)
    public Path duongDanCamKet(Long id) {
        CongTacVien c = lay(id);
        if (c.getCamKetUrl() == null) throw new NotFoundException("cam kết của CTV", id);
        Path p = thuMucCamKet.resolve(c.getCamKetUrl()).normalize();
        if (!p.startsWith(thuMucCamKet) || !Files.exists(p)) throw new NotFoundException("cam kết của CTV", id);
        return p;
    }

    // ------------------------------------------------------------------ hoc vien xin lam CTV
    @Transactional
    public CtvResponse xinLamCtv(XinLamCtvRequest req, AuthUser user) {
        HocVien hv = hocVienRepo.findByNguoiDungId(user.id())
                .orElseThrow(() -> new BusinessException("KHONG_PHAI_HOC_VIEN", "Không tìm thấy hồ sơ học viên"));
        ctvRepo.findByHocVienId(hv.getId()).ifPresent(c -> {
            throw new BusinessException("DA_GUI_YEU_CAU", c.getTrangThai() == CongTacVien.TrangThai.CHO_DUYET
                    ? "Bạn đã gửi yêu cầu, trung tâm đang xem xét" : "Bạn đã có hồ sơ cộng tác viên");
        });
        if (ctvRepo.existsBySoDienThoai(hv.getSoDienThoai()) || ctvRepo.existsByCccd(hv.getCccd())) {
            throw new BusinessException("TRUNG_CTV", "Số điện thoại hoặc CCCD đã thuộc một hồ sơ CTV khác. Liên hệ trung tâm");
        }
        CongTacVien c = new CongTacVien();
        c.setHoTen(hv.getHoTen());
        c.setSoDienThoai(hv.getSoDienThoai());
        c.setCccd(hv.getCccd());
        c.setDiaChi(hv.getDiaChi());
        c.setZalo(rong(req.zalo()));
        c.setNganHang(rong(req.nganHang()));
        c.setSoTaiKhoan(rong(req.soTaiKhoan()));
        c.setChuTaiKhoan(rong(req.chuTaiKhoan()));
        c.setGhiChu(rong(req.ghiChu()));
        c.setLoai(CongTacVien.Loai.HOC_VIEN_CU);
        c.setHocVienId(hv.getId());
        c.setNguoiTaoId(user.id());
        c.setTrangThai(CongTacVien.TrangThai.CHO_DUYET);
        ctvRepo.save(c);
        thongBao.guiTheoVaiTro(List.of(VaiTro.ADMIN, VaiTro.LE_TAN), ThongBaoService.HO_SO,
                "Học viên xin làm CTV: " + hv.getHoTen(),
                hv.getSoDienThoai() + ". Lễ tân nhận bản cam kết đã ký, tải lên hồ sơ; quản trị viên duyệt.", "/ctv", null);
        return response(c, user);
    }

    // ------------------------------------------------------------------ ho tro
    /** CTV gan voi tai khoan dang dang nhap: vai tro CTV, hoac hoc vien co ho so CTV. */
    @Transactional(readOnly = true)
    public Optional<CongTacVien> cuaNguoiDung(AuthUser user) {
        if (user.la(VaiTro.CTV)) return ctvRepo.findByNguoiDungId(user.id());
        if (user.la(VaiTro.HOC_VIEN)) {
            return hocVienRepo.findByNguoiDungId(user.id()).flatMap(hv -> ctvRepo.findByHocVienId(hv.getId()));
        }
        return Optional.empty();
    }

    public CongTacVien lay(Long id) {
        return ctvRepo.findById(id).orElseThrow(() -> new NotFoundException("cộng tác viên", id));
    }

    private void kiemTraTrung(String sdt, String cccd, Long boQuaId) {
        boolean trungSdt = boQuaId == null ? ctvRepo.existsBySoDienThoai(sdt) : ctvRepo.existsBySoDienThoaiAndIdNot(sdt, boQuaId);
        if (trungSdt) throw new BusinessException("TRUNG_SDT_CTV", "Số điện thoại " + sdt + " đã thuộc một CTV khác");
        if (cccd != null) {
            boolean trungCccd = boQuaId == null ? ctvRepo.existsByCccd(cccd) : ctvRepo.existsByCccdAndIdNot(cccd, boQuaId);
            if (trungCccd) throw new BusinessException("TRUNG_CCCD_CTV", "CCCD " + cccd + " đã thuộc một CTV khác");
        }
    }

    private static void gan(CongTacVien c, CtvRequest req) {
        c.setHoTen(req.hoTen().trim());
        c.setSoDienThoai(req.soDienThoai());
        c.setZalo(rong(req.zalo()));
        c.setCccd(req.cccd());
        c.setDiaChi(rong(req.diaChi()));
        c.setDiaBan(rong(req.diaBan()));
        c.setLoai(req.loai());
        c.setNganHang(rong(req.nganHang()));
        c.setSoTaiKhoan(rong(req.soTaiKhoan()));
        c.setChuTaiKhoan(rong(req.chuTaiKhoan()));
        if (req.ngayBatDau() != null) c.setNgayBatDau(req.ngayBatDau());
        c.setGhiChu(rong(req.ghiChu()));
    }

    private void guiChoCtv(CongTacVien c, String tieuDe, String noiDung) {
        Long nd = c.getNguoiDungId();
        if (nd == null && c.getHocVienId() != null) {
            nd = hocVienRepo.findById(c.getHocVienId()).map(HocVien::nguoiDungId).orElse(null);
        }
        thongBao.gui(nd, ThongBaoService.HO_SO, tieuDe, noiDung, "/ctv-cua-toi", null);
    }

    CtvResponse response(CongTacVien c, AuthUser user) {
        // Le tan khong can thay du so tai khoan cua CTV da duyet (chi admin chi tien)
        String stk = c.getSoTaiKhoan();
        if (stk != null && !user.la(VaiTro.ADMIN) && c.getTrangThai() != CongTacVien.TrangThai.CHO_DUYET
                && stk.length() > 4) {
            stk = "••••" + stk.substring(stk.length() - 4);
        }
        String tenDangNhap = c.getNguoiDungId() == null ? null
                : nguoiDungRepo.findById(c.getNguoiDungId()).map(NguoiDung::getTenDangNhap).orElse(null);
        return new CtvResponse(c.getId(), c.getHoTen(), c.getSoDienThoai(), c.getZalo(), c.getCccd(), c.getDiaChi(),
                c.getDiaBan(), c.getLoai().name(), c.getHang().name(), c.getTrangThai().name(), c.getNganHang(), stk,
                c.getChuTaiKhoan(), c.getCamKetUrl() != null, c.getNgayBatDau(), c.getGhiChu(), c.getHocVienId(),
                tenDangNhap, c.getNguoiTaoId(), tenNguoiDung(c.getNguoiTaoId()), c.getCreatedAt(), tenNguoiDung(c.getNguoiDuyetId()),
                c.getNgayDuyet(), c.getId() == null ? 0 : leadRepo.countByCtvId(c.getId()),
                c.getId() == null ? 0 : dangKyRepo.countByCtvIdAndTrangThaiNot(c.getId(), DangKy.TrangThai.DA_HUY));
    }

    String tenNguoiDung(Long id) {
        return id == null ? null : nguoiDungRepo.findById(id).map(NguoiDung::getHoTen).orElse(null);
    }

    static String nhanLoai(CongTacVien.Loai l) {
        return switch (l) {
            case HOC_VIEN_CU -> "học viên cũ";
            case SINH_VIEN -> "sinh viên";
            case DOI_TAC -> "đối tác ngoài";
        };
    }

    private static String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
