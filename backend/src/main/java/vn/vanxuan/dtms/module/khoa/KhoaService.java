package vn.vanxuan.dtms.module.khoa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;
import vn.vanxuan.dtms.module.danhmuc.HangGplxRepository;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static vn.vanxuan.dtms.module.khoa.KhoaDtos.*;

/** Quan ly khoa dao tao (FR-04; BR-03). */
@Service
public class KhoaService {

    /** Chuyen trang thai hop le: DU_KIEN -> DANG_TUYEN -> DANG_DAO_TAO -> DA_KET_THUC; huy khi chua dao tao. */
    private static final Map<KhoaDaoTao.TrangThai, Set<KhoaDaoTao.TrangThai>> CHUYEN = Map.of(
            KhoaDaoTao.TrangThai.DU_KIEN, EnumSet.of(KhoaDaoTao.TrangThai.DANG_TUYEN, KhoaDaoTao.TrangThai.HUY),
            KhoaDaoTao.TrangThai.DANG_TUYEN, EnumSet.of(KhoaDaoTao.TrangThai.DANG_DAO_TAO, KhoaDaoTao.TrangThai.HUY),
            KhoaDaoTao.TrangThai.DANG_DAO_TAO, EnumSet.of(KhoaDaoTao.TrangThai.DA_KET_THUC),
            KhoaDaoTao.TrangThai.DA_KET_THUC, EnumSet.noneOf(KhoaDaoTao.TrangThai.class),
            KhoaDaoTao.TrangThai.HUY, EnumSet.noneOf(KhoaDaoTao.TrangThai.class));

    private final KhoaDaoTaoRepository khoaRepo;
    private final HangGplxRepository hangRepo;
    private final DangKyRepository dangKyRepo;
    private final NhatKyService nhatKy;
    private final ThongBaoService thongBao;

    public KhoaService(KhoaDaoTaoRepository khoaRepo, HangGplxRepository hangRepo, DangKyRepository dangKyRepo,
                       NhatKyService nhatKy, ThongBaoService thongBao) {
        this.khoaRepo = khoaRepo;
        this.hangRepo = hangRepo;
        this.dangKyRepo = dangKyRepo;
        this.nhatKy = nhatKy;
        this.thongBao = thongBao;
    }

    @Transactional
    public KhoaResponse tao(TaoKhoaRequest req) {
        HangGplx hang = hangRepo.findById(req.hangMa()).orElseThrow(() -> new NotFoundException("hạng GPLX", req.hangMa()));
        if (khoaRepo.existsByMaKhoa(req.maKhoa().trim())) {
            throw new BusinessException("TRUNG_MA_KHOA", "Mã khóa " + req.maKhoa() + " đã tồn tại");
        }
        if (req.ngayBeGiang().isBefore(req.ngayKhaiGiang())) {
            throw new BusinessException("NGAY_KHONG_HOP_LE", "Ngày bế giảng phải sau ngày khai giảng");
        }
        KhoaDaoTao k = new KhoaDaoTao();
        k.setMaKhoa(req.maKhoa().trim());
        k.setHang(hang);
        k.setNgayKhaiGiang(req.ngayKhaiGiang());
        k.setNgayBeGiang(req.ngayBeGiang());
        // BR-03: tong thoi gian khoa mo to khong qua so ngay toi da cua hang (10 ngay)
        if (k.soNgay() > hang.getSoNgayKhoaToiDa()) {
            throw new BusinessException("KHOA_QUA_DAI", "Khóa hạng " + hang.getMa() + " không được quá "
                    + hang.getSoNgayKhoaToiDa() + " ngày (đang là " + k.soNgay() + " ngày)");
        }
        k.setSiSoToiDa(req.siSoToiDa());
        k.setHocPhi(req.hocPhi() != null ? req.hocPhi() : hang.getHocPhiMacDinh());
        k.setGhiChu(req.ghiChu());
        k.setTrangThai(KhoaDaoTao.TrangThai.DU_KIEN);
        return KhoaResponse.of(khoaRepo.save(k), 0);
    }

    @Transactional(readOnly = true)
    public List<KhoaResponse> ds(KhoaDaoTao.TrangThai trangThai) {
        return khoaRepo.timKiem(trangThai).stream()
                .map(k -> KhoaResponse.of(k, dangKyRepo.countByKhoaIdAndTrangThaiNot(k.getId(), DangKy.TrangThai.DA_HUY)))
                .toList();
    }

    @Transactional(readOnly = true)
    public KhoaResponse chiTiet(Long id) {
        KhoaDaoTao k = khoaRepo.findWithHang(id).orElseThrow(() -> new NotFoundException("khóa đào tạo", id));
        return KhoaResponse.of(k, dangKyRepo.countByKhoaIdAndTrangThaiNot(id, DangKy.TrangThai.DA_HUY));
    }

    @Transactional
    public KhoaResponse sua(Long id, SuaKhoaRequest req) {
        KhoaDaoTao k = khoaRepo.findWithHang(id).orElseThrow(() -> new NotFoundException("khóa đào tạo", id));
        HangGplx hang = hangRepo.findById(req.hangMa()).orElseThrow(() -> new NotFoundException("hạng GPLX", req.hangMa()));
        if (!k.getMaKhoa().equals(req.maKhoa().trim()) && khoaRepo.existsByMaKhoa(req.maKhoa().trim())) {
            throw new BusinessException("TRUNG_MA_KHOA", "Mã khóa " + req.maKhoa() + " đã tồn tại");
        }
        if (req.ngayBeGiang().isBefore(req.ngayKhaiGiang())) {
            throw new BusinessException("NGAY_KHONG_HOP_LE", "Ngày bế giảng phải sau ngày khai giảng");
        }
        k.setMaKhoa(req.maKhoa().trim());
        k.setHang(hang);
        k.setNgayKhaiGiang(req.ngayKhaiGiang());
        k.setNgayBeGiang(req.ngayBeGiang());
        if (k.soNgay() > hang.getSoNgayKhoaToiDa()) {
            throw new BusinessException("KHOA_QUA_DAI", "Khóa hạng " + hang.getMa() + " không được quá "
                    + hang.getSoNgayKhoaToiDa() + " ngày (đang là " + k.soNgay() + " ngày)");
        }
        k.setSiSoToiDa(req.siSoToiDa());
        k.setHocPhi(req.hocPhi() != null ? req.hocPhi() : hang.getHocPhiMacDinh());
        k.setGhiChu(req.ghiChu());
        return KhoaResponse.of(k, dangKyRepo.countByKhoaIdAndTrangThaiNot(id, DangKy.TrangThai.DA_HUY));
    }

    @Transactional
    public KhoaResponse doiTrangThai(Long id, KhoaDaoTao.TrangThai moi, AuthUser admin) {
        KhoaDaoTao k = khoaRepo.findWithHang(id).orElseThrow(() -> new NotFoundException("khóa đào tạo", id));
        if (!CHUYEN.get(k.getTrangThai()).contains(moi)) {
            throw new BusinessException("CHUYEN_TRANG_THAI_SAI",
                    "Không thể chuyển khóa từ " + k.getTrangThai() + " sang " + moi);
        }
        KhoaDaoTao.TrangThai cu = k.getTrangThai();
        k.setTrangThai(moi);
        if (moi == KhoaDaoTao.TrangThai.DANG_DAO_TAO) {
            // Khai giang: hoc vien da tiep nhan chuyen sang dang hoc; ghi nhan ngay bao cao So (BR-12)
            dangKyRepo.doiTrangThaiTheoKhoa(id, DangKy.TrangThai.DA_TIEP_NHAN, DangKy.TrangThai.DANG_HOC);
            if (k.getNgayBaoCaoSo() == null) k.setNgayBaoCaoSo(LocalDate.now());
            thongBao.guiTheoVaiTro(List.of(VaiTro.ADMIN), ThongBaoService.BAO_CAO_SO, "Lập báo cáo đăng ký khóa " + k.getMaKhoa(),
                    "Khóa đã khai giảng. Lập báo cáo đăng ký khóa đào tạo gửi Sở Xây dựng (BR-12).", "/khoa", "BC-DK-" + id);
            for (DangKy dk : dangKyRepo.findByKhoaIdAndTrangThaiInOrderByHocVienHoTen(id, Set.of(DangKy.TrangThai.DANG_HOC))) {
                thongBao.gui(dk.getHocVien().nguoiDungId(), ThongBaoService.LICH_HOC, "Khóa " + k.getMaKhoa() + " đã khai giảng",
                        "Xem lịch học và theo dõi tiến độ trong mục Tiến độ học tập.", "/hoc-tap", "KHAI-GIANG-" + id);
            }
        }
        nhatKy.ghi(admin.id(), "DOI_TRANG_THAI_KHOA", "khoa_dao_tao", id, Map.of("tu", cu.name(), "sang", moi.name()));
        return KhoaResponse.of(k, dangKyRepo.countByKhoaIdAndTrangThaiNot(id, DangKy.TrangThai.DA_HUY));
    }
}
