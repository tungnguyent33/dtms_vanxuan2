package vn.vanxuan.dtms.module.ctv;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.CauHinhService;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.SoThuTuService;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.danhmuc.CongTacVienRepository;
import vn.vanxuan.dtms.module.hocphi.PhieuThuRepository;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.hocvien.HocVien;
import vn.vanxuan.dtms.module.hocvien.HocVienRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/**
 * Hoa hong CTV - tinh tu dong, khong nhap tay.
 * - Tao khi dang ky co CTV: chup lai chinh sach dang hieu luc (doi chinh sach sau khong lam sai don cu).
 * - Du dieu kien khi hoc vien da dong >= X% hoc phi (cau hinh, mac dinh 100%) va ho so khong o trang thai cho duyet / huy.
 * - Admin duyet -> chi theo ky thang (ky = thang du dieu kien). Moi lan chi ghi ngay, so tien, nguoi chi.
 */
@Service
public class HoaHongService {

    private static final DateTimeFormatter KY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final BigDecimal NGHIN = BigDecimal.valueOf(1000);

    private final HoaHongRepository repo;
    private final ChinhSachHoaHongRepository csRepo;
    private final ChiHoaHongRepository chiRepo;
    private final CongTacVienRepository ctvRepo;
    private final DangKyRepository dangKyRepo;
    private final HocVienRepository hocVienRepo;
    private final PhieuThuRepository phieuRepo;
    private final CauHinhService cauHinh;
    private final SoThuTuService soThuTu;
    private final ThongBaoService thongBao;
    private final NhatKyService nhatKy;
    private final NguoiDungRepository nguoiDungRepo;

    public HoaHongService(HoaHongRepository repo, ChinhSachHoaHongRepository csRepo, ChiHoaHongRepository chiRepo,
                          CongTacVienRepository ctvRepo, DangKyRepository dangKyRepo, HocVienRepository hocVienRepo,
                          PhieuThuRepository phieuRepo, CauHinhService cauHinh, SoThuTuService soThuTu,
                          ThongBaoService thongBao, NhatKyService nhatKy, NguoiDungRepository nguoiDungRepo) {
        this.repo = repo;
        this.csRepo = csRepo;
        this.chiRepo = chiRepo;
        this.ctvRepo = ctvRepo;
        this.dangKyRepo = dangKyRepo;
        this.hocVienRepo = hocVienRepo;
        this.phieuRepo = phieuRepo;
        this.cauHinh = cauHinh;
        this.soThuTu = soThuTu;
        this.thongBao = thongBao;
        this.nhatKy = nhatKy;
        this.nguoiDungRepo = nguoiDungRepo;
    }

    // ------------------------------------------------------------------ tinh tu dong
    /** Goi khi tao dang ky co CTV hoac khi gan / doi CTV cho dang ky. */
    @Transactional
    public void tinhChoDangKy(DangKy dk) {
        if (dk.getCtv() == null) return;
        HoaHong h = repo.findByDangKyId(dk.getId()).orElseGet(HoaHong::new);
        if (h.getTrangThai() == HoaHong.TrangThai.DA_DUYET || h.getTrangThai() == HoaHong.TrangThai.DA_CHI) {
            throw new BusinessException("HOA_HONG_DA_DUYET", "Hoa hồng của hồ sơ này đã được duyệt / chi, không đổi được CTV");
        }
        CongTacVien ctv = dk.getCtv();
        LocalDate ngayTinh = dk.getNgayDangKy() == null ? LocalDate.now() : dk.getNgayDangKy().toLocalDate();
        Optional<ChinhSachHoaHong> cs = csRepo.apDung(ctv.getHang(), dk.getKhoa().getHang().getMa(), ngayTinh)
                .stream().findFirst();
        h.setDangKyId(dk.getId());
        h.setCtvId(ctv.getId());
        h.setCoSo(dk.phaiDong());
        h.setTrangThai(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);
        h.setKy(null);
        h.setNgayDuDieuKien(null);
        if (cs.isPresent()) {
            h.setChinhSachId(cs.get().getId());
            h.setKieu(cs.get().getKieu());
            h.setGiaTri(cs.get().getGiaTri());
            h.setSoTien(tinhTien(cs.get().getKieu(), cs.get().getGiaTri(), dk.phaiDong()));
            h.setGhiChu(null);
        } else {
            h.setChinhSachId(null);
            h.setKieu(null);
            h.setGiaTri(null);
            h.setSoTien(BigDecimal.ZERO);
            h.setGhiChu("Chưa có chính sách hoa hồng cho hạng CTV " + ctv.getHang() + ", hạng " + dk.getKhoa().getHang().getMa());
        }
        repo.save(h);
        capNhatTrangThai(dk);
    }

    /** Goi sau moi thay doi tien / trang thai ho so: lap / huy phieu thu, duyet, huy ho so. */
    @Transactional
    public void capNhatTrangThai(DangKy dk) {
        HoaHong h = repo.findByDangKyId(dk.getId()).orElse(null);
        if (h == null) return;
        HoaHong.TrangThai cu = h.getTrangThai();
        if (cu == HoaHong.TrangThai.DA_CHI) {
            if (dk.getTrangThai() == DangKy.TrangThai.DA_HUY && h.getGhiChu() == null) {
                h.setGhiChu("Hồ sơ bị hủy sau khi đã chi hoa hồng – cần đối soát với CTV");
            }
            return;
        }
        if (dk.getTrangThai() == DangKy.TrangThai.DA_HUY) {
            h.setTrangThai(HoaHong.TrangThai.HUY);
            h.setKy(null);
            if (cu == HoaHong.TrangThai.DA_DUYET) baoAdminGoDuyet(h, dk, "hồ sơ đã bị hủy");
            return;
        }
        if (cu == HoaHong.TrangThai.HUY) h.setTrangThai(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);
        boolean du = duDieuKien(dk);
        if (du && h.getTrangThai() == HoaHong.TrangThai.CHUA_DU_DIEU_KIEN) {
            h.setTrangThai(HoaHong.TrangThai.DU_DIEU_KIEN);
            h.setNgayDuDieuKien(LocalDate.now());
            h.setKy(LocalDate.now().format(KY));
            guiChoCtv(h.getCtvId(), "Hoa hồng " + vnd(h.getSoTien()) + " đã đủ điều kiện",
                    "Học viên " + dk.getHocVien().getHoTen() + " đã đóng học phí. Hoa hồng thuộc kỳ " + h.getKy()
                            + ", chờ trung tâm duyệt chi.");
        } else if (!du && (h.getTrangThai() == HoaHong.TrangThai.DU_DIEU_KIEN || h.getTrangThai() == HoaHong.TrangThai.DA_DUYET)) {
            // Phieu thu bi huy lam hoc vien khong con du % -> quay lai, bo duyet
            if (h.getTrangThai() == HoaHong.TrangThai.DA_DUYET) baoAdminGoDuyet(h, dk, "học viên không còn đóng đủ học phí");
            h.setTrangThai(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);
            h.setKy(null);
            h.setNgayDuDieuKien(null);
            h.setNguoiDuyetId(null);
            h.setNgayDuyet(null);
        }
    }

    /**
     * Tinh lai trang thai moi khoan chua chi (sau khi chuyen du lieu cu, hoac khi admin doi nguong % dong).
     * Chay luc khoi dong va trong tac vu nhac viec hang ngay. @return so khoan doi trang thai.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public int doiSoatTatCa() {
        int n = 0;
        for (HoaHong h : repo.timKiem(null, null, null)) {
            if (h.getTrangThai() == HoaHong.TrangThai.DA_CHI) continue;
            var cu = h.getTrangThai();
            dangKyRepo.findChiTiet(h.getDangKyId()).ifPresent(this::capNhatTrangThai);
            if (cu != h.getTrangThai()) n++;
        }
        return n;
    }

    private boolean duDieuKien(DangKy dk) {
        if (dk.getTrangThai() == DangKy.TrangThai.CHO_DUYET) return false;
        BigDecimal phaiDong = dk.phaiDong();
        BigDecimal daDong = phieuRepo.tongDaThu(dk.getId());
        int pct = cauHinh.soNguyen(CauHinhService.HOA_HONG_PHAN_TRAM_DONG, 100);
        if (phaiDong.signum() == 0) return true;
        return daDong.multiply(BigDecimal.valueOf(100)).compareTo(phaiDong.multiply(BigDecimal.valueOf(pct))) >= 0;
    }

    static BigDecimal tinhTien(ChinhSachHoaHong.Kieu kieu, BigDecimal giaTri, BigDecimal coSo) {
        if (kieu == ChinhSachHoaHong.Kieu.CO_DINH) return giaTri.setScale(0, RoundingMode.HALF_UP);
        // Phan tram: lam tron den nghin dong
        return coSo.multiply(giaTri).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .divide(NGHIN, 0, RoundingMode.HALF_UP).multiply(NGHIN);
    }

    // ------------------------------------------------------------------ admin duyet / chi
    @Transactional
    public int duyet(List<Long> ids, AuthUser admin) {
        List<HoaHong> ds = repo.khoaTheoId(ids);
        int n = 0;
        for (HoaHong h : ds) {
            if (h.getTrangThai() != HoaHong.TrangThai.DU_DIEU_KIEN) {
                throw new BusinessException("CHUA_DU_DIEU_KIEN", "Hoa hồng #" + h.getId() + " đang ở trạng thái "
                        + h.getTrangThai() + ", chỉ duyệt được khoản đủ điều kiện");
            }
            if (h.getSoTien().signum() <= 0) {
                throw new BusinessException("HOA_HONG_BANG_0", "Hoa hồng #" + h.getId() + " bằng 0 (chưa có chính sách)");
            }
            h.setTrangThai(HoaHong.TrangThai.DA_DUYET);
            h.setNguoiDuyetId(admin.id());
            h.setNgayDuyet(LocalDateTime.now());
            n++;
        }
        nhatKy.ghi(admin.id(), "DUYET_HOA_HONG", "hoa_hong", null, Map.of("ids", ids));
        return n;
    }

    /** Chi tat ca khoan da duyet cua mot CTV tu ky chi dinh tro ve truoc. */
    @Transactional
    public ChiHoaHongResponse chi(ChiHoaHongRequest req, AuthUser admin) {
        CongTacVien ctv = ctvRepo.findById(req.ctvId())
                .orElseThrow(() -> new BusinessException("KHONG_TIM_THAY", "Không tìm thấy CTV"));
        List<HoaHong> ds = repo.khoaDeChi(req.ctvId(), req.ky());
        if (ds.isEmpty()) {
            throw new BusinessException("KHONG_CO_KHOAN_CHI", "CTV không có khoản hoa hồng đã duyệt đến kỳ " + req.ky());
        }
        if (req.hinhThuc() == ChiHoaHong.HinhThuc.CHUYEN_KHOAN && ctv.getSoTaiKhoan() == null) {
            throw new BusinessException("THIEU_TAI_KHOAN", "CTV chưa có tài khoản ngân hàng. Chọn tiền mặt hoặc cập nhật hồ sơ");
        }
        if (req.ngayChi().isAfter(LocalDate.now())) {
            throw new BusinessException("NGAY_CHI_SAI", "Ngày chi không được sau hôm nay");
        }
        BigDecimal tong = ds.stream().map(HoaHong::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        String thang = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        ChiHoaHong p = new ChiHoaHong();
        p.setSoPhieu(soThuTu.capMa("PC-" + thang, "PC-" + thang + "-", 4));
        p.setCtvId(ctv.getId());
        p.setKy(req.ky());
        p.setSoTien(tong);
        p.setHinhThuc(req.hinhThuc());
        p.setNgayChi(req.ngayChi());
        p.setNguoiChiId(admin.id());
        p.setGhiChu(req.ghiChu());
        chiRepo.save(p);
        for (HoaHong h : ds) {
            h.setTrangThai(HoaHong.TrangThai.DA_CHI);
            h.setChiHoaHongId(p.getId());
        }
        nhatKy.ghi(admin.id(), "CHI_HOA_HONG", "chi_hoa_hong", p.getId(),
                Map.of("soPhieu", p.getSoPhieu(), "ctvId", ctv.getId(), "soTien", tong, "soDon", ds.size()));
        guiChoCtv(ctv.getId(), "Đã chi hoa hồng " + vnd(tong), "Phiếu " + p.getSoPhieu() + " ngày "
                + req.ngayChi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ", " + ds.size() + " học viên.");
        return chiResponse(p, ctv.getHoTen(), ds.size());
    }

    // ------------------------------------------------------------------ tra cuu
    @Transactional(readOnly = true)
    public List<HoaHongResponse> ds(Long ctvId, String ky, HoaHong.TrangThai trangThai) {
        return repo.timKiem(ctvId, ky, trangThai).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<HoaHongResponse> cuaCtv(Long ctvId) {
        return repo.findByCtvIdOrderByIdDesc(ctvId).stream().map(this::response).toList();
    }

    /** Tong hop de chi: moi CTV con khoan du dieu kien / da duyet den ky. */
    @Transactional(readOnly = true)
    public List<TongHopKy> tongHop(String ky) {
        Map<Long, List<HoaHong>> theoCtv = new LinkedHashMap<>();
        for (HoaHong h : repo.timKiem(null, null, null)) {
            boolean trongKy = h.getKy() != null && h.getKy().compareTo(ky) <= 0;
            boolean chiTrongKy = h.getTrangThai() == HoaHong.TrangThai.DA_CHI && ky.equals(h.getKy());
            if ((trongKy && (h.getTrangThai() == HoaHong.TrangThai.DU_DIEU_KIEN || h.getTrangThai() == HoaHong.TrangThai.DA_DUYET))
                    || chiTrongKy) {
                theoCtv.computeIfAbsent(h.getCtvId(), k -> new ArrayList<>()).add(h);
            }
        }
        List<TongHopKy> kq = new ArrayList<>();
        theoCtv.forEach((ctvId, ds) -> {
            CongTacVien c = ctvRepo.findById(ctvId).orElseThrow();
            kq.add(new TongHopKy(ctvId, c.getHoTen(), c.getSoDienThoai(), c.getNganHang(), c.getSoTaiKhoan(),
                    c.getChuTaiKhoan(), ds.size(), tong(ds, HoaHong.TrangThai.DU_DIEU_KIEN),
                    tong(ds, HoaHong.TrangThai.DA_DUYET), tong(ds, HoaHong.TrangThai.DA_CHI)));
        });
        return kq;
    }

    @Transactional(readOnly = true)
    public List<ChiHoaHongResponse> dsPhieuChi(Long ctvId) {
        var ds = ctvId == null ? chiRepo.findAllByOrderByIdDesc() : chiRepo.findByCtvIdOrderByIdDesc(ctvId);
        return ds.stream().map(p -> chiResponse(p, ctvRepo.findById(p.getCtvId()).map(CongTacVien::getHoTen).orElse(""),
                (int) repo.findByCtvIdOrderByIdDesc(p.getCtvId()).stream()
                        .filter(h -> p.getId().equals(h.getChiHoaHongId())).count())).toList();
    }

    // ------------------------------------------------------------------ chinh sach
    @Transactional(readOnly = true)
    public List<ChinhSachResponse> dsChinhSach() {
        LocalDate homNay = LocalDate.now();
        List<ChinhSachHoaHong> tatCa = csRepo.findAllByOrderByHangGplxAscHangCtvAscHieuLucTuDesc();
        Set<Long> dangApDung = new HashSet<>();
        for (CongTacVien.Hang hc : CongTacVien.Hang.values()) {
            tatCa.stream().map(ChinhSachHoaHong::getHangGplx).distinct().forEach(hg ->
                    csRepo.apDung(hc, hg, homNay).stream().findFirst().ifPresent(c -> dangApDung.add(c.getId())));
        }
        List<HoaHong> hh = repo.findAll();
        return tatCa.stream().map(c -> new ChinhSachResponse(c.getId(), c.getHangCtv().name(), c.getHangGplx(),
                c.getKieu().name(), c.getGiaTri(), c.getHieuLucTu(), dangApDung.contains(c.getId()),
                hh.stream().filter(h -> c.getId().equals(h.getChinhSachId())).count())).toList();
    }

    /** Doi chinh sach = them muc moi co ngay hieu luc; khong sua muc cu (don cu giu muc da chup). */
    @Transactional
    public ChinhSachResponse themChinhSach(ChinhSachRequest req, AuthUser admin) {
        if (req.kieu() == ChinhSachHoaHong.Kieu.PHAN_TRAM && req.giaTri().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("GIA_TRI_SAI", "Tỷ lệ phần trăm không quá 100");
        }
        if (req.hieuLucTu().isBefore(LocalDate.now())) {
            throw new BusinessException("HIEU_LUC_QUA_KHU", "Ngày hiệu lực phải từ hôm nay trở đi (không áp ngược cho đơn cũ)");
        }
        if (csRepo.existsByHangCtvAndHangGplxAndHieuLucTu(req.hangCtv(), req.hangGplx(), req.hieuLucTu())) {
            throw new BusinessException("TRUNG_CHINH_SACH", "Đã có mức hoa hồng cho hạng CTV và hạng bằng này từ ngày đó");
        }
        ChinhSachHoaHong c = new ChinhSachHoaHong();
        c.setHangCtv(req.hangCtv());
        c.setHangGplx(req.hangGplx());
        c.setKieu(req.kieu());
        c.setGiaTri(req.giaTri());
        c.setHieuLucTu(req.hieuLucTu());
        c.setNguoiTaoId(admin.id());
        csRepo.save(c);
        nhatKy.ghi(admin.id(), "THEM_CHINH_SACH_HOA_HONG", "chinh_sach_hoa_hong", c.getId(),
                Map.of("hangCtv", req.hangCtv().name(), "hangGplx", req.hangGplx(), "kieu", req.kieu().name(),
                        "giaTri", req.giaTri(), "hieuLucTu", req.hieuLucTu().toString()));
        return new ChinhSachResponse(c.getId(), c.getHangCtv().name(), c.getHangGplx(), c.getKieu().name(),
                c.getGiaTri(), c.getHieuLucTu(), !c.getHieuLucTu().isAfter(LocalDate.now()), 0);
    }

    /** Chi xoa duoc muc chua co hieu luc va chua dung cho don nao (nhap nham). */
    @Transactional
    public void xoaChinhSach(Long id, AuthUser admin) {
        ChinhSachHoaHong c = csRepo.findById(id).orElseThrow(() -> new BusinessException("KHONG_TIM_THAY", "Không tìm thấy chính sách"));
        if (!c.getHieuLucTu().isAfter(LocalDate.now())) {
            throw new BusinessException("CHINH_SACH_DA_HIEU_LUC", "Chính sách đã có hiệu lực, không xóa được. Hãy thêm mức mới");
        }
        csRepo.delete(c);
        nhatKy.ghi(admin.id(), "XOA_CHINH_SACH_HOA_HONG", "chinh_sach_hoa_hong", id, null);
    }

    // ------------------------------------------------------------------ ho tro
    HoaHongResponse response(HoaHong h) {
        DangKy dk = dangKyRepo.findChiTiet(h.getDangKyId()).orElseThrow();
        String tenCtv = ctvRepo.findById(h.getCtvId()).map(CongTacVien::getHoTen).orElse("");
        String soPhieu = h.getChiHoaHongId() == null ? null
                : chiRepo.findById(h.getChiHoaHongId()).map(ChiHoaHong::getSoPhieu).orElse(null);
        return new HoaHongResponse(h.getId(), dk.getId(), dk.getMaHoSo(), dk.getHocVien().getHoTen(),
                dk.getKhoa().getMaKhoa(), dk.getKhoa().getHang().getMa(), h.getCtvId(), tenCtv,
                h.getKieu() == null ? null : h.getKieu().name(), h.getGiaTri(), h.getCoSo(), h.getSoTien(),
                h.getTrangThai().name(), h.getNgayDuDieuKien(), h.getKy(), h.getNgayDuyet(), soPhieu, h.getGhiChu());
    }

    private ChiHoaHongResponse chiResponse(ChiHoaHong p, String tenCtv, int soDon) {
        String nguoiChi = nguoiDungRepo.findById(p.getNguoiChiId()).map(NguoiDung::getHoTen).orElse(null);
        return new ChiHoaHongResponse(p.getId(), p.getSoPhieu(), p.getCtvId(), tenCtv, p.getKy(), p.getSoTien(),
                p.getHinhThuc().name(), p.getNgayChi(), nguoiChi, p.getGhiChu(), soDon);
    }

    private static BigDecimal tong(List<HoaHong> ds, HoaHong.TrangThai tt) {
        return ds.stream().filter(h -> h.getTrangThai() == tt).map(HoaHong::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void baoAdminGoDuyet(HoaHong h, DangKy dk, String lyDo) {
        thongBao.guiTheoVaiTro(List.of(VaiTro.ADMIN), ThongBaoService.HOC_PHI,
                "Hoa hồng hồ sơ " + dk.getMaHoSo() + " bị bỏ duyệt",
                "Khoản " + vnd(h.getSoTien()) + " đã duyệt nhưng " + lyDo + ". Hệ thống đưa về chưa đủ điều kiện.",
                "/ctv?tab=hoa-hong", null);
    }

    private void guiChoCtv(Long ctvId, String tieuDe, String noiDung) {
        ctvRepo.findById(ctvId).ifPresent(c -> {
            Long nd = c.getNguoiDungId();
            if (nd == null && c.getHocVienId() != null) {
                nd = hocVienRepo.findById(c.getHocVienId()).map(HocVien::nguoiDungId).orElse(null);
            }
            thongBao.gui(nd, ThongBaoService.HOC_PHI, tieuDe, noiDung, "/ctv-cua-toi", null);
        });
    }

    static String vnd(BigDecimal v) {
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(v) + " đ";
    }
}
