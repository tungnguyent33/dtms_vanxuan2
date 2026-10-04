package vn.vanxuan.dtms.module.ctv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.CauHinhService;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.SoThuTuService;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.danhmuc.CongTacVienRepository;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;
import vn.vanxuan.dtms.module.hocphi.PhieuThuRepository;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.hocvien.HocVien;
import vn.vanxuan.dtms.module.hocvien.HocVienRepository;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Quy tac hoa hong CTV: tinh tien theo chinh sach, dieu kien phat sinh, duyet. Khong can CSDL. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HoaHongServiceTest {

    @Mock HoaHongRepository repo;
    @Mock ChinhSachHoaHongRepository csRepo;
    @Mock ChiHoaHongRepository chiRepo;
    @Mock CongTacVienRepository ctvRepo;
    @Mock DangKyRepository dangKyRepo;
    @Mock HocVienRepository hocVienRepo;
    @Mock PhieuThuRepository phieuRepo;
    @Mock CauHinhService cauHinh;
    @Mock SoThuTuService soThuTu;
    @Mock ThongBaoService thongBao;
    @Mock NhatKyService nhatKy;
    @Mock NguoiDungRepository nguoiDungRepo;
    @InjectMocks HoaHongService service;

    private DangKy dk;
    private HoaHong h;

    @BeforeEach
    void setUp() {
        HangGplx a1 = new HangGplx();
        a1.setMa("A1");
        KhoaDaoTao khoa = new KhoaDaoTao();
        khoa.setHang(a1);
        khoa.setMaKhoa("A1-TEST");
        CongTacVien ctv = new CongTacVien();
        ctv.setId(7L);
        ctv.setHang(CongTacVien.Hang.BAC);
        ctv.setTrangThai(CongTacVien.TrangThai.HOAT_DONG);
        HocVien hv = new HocVien();
        hv.setHoTen("Học Viên Test");

        dk = new DangKy();
        dk.setId(100L);
        dk.setKhoa(khoa);
        dk.setCtv(ctv);
        dk.setHocVien(hv);
        dk.setHocPhi(new BigDecimal("1500000"));
        dk.setGiamTru(BigDecimal.ZERO);
        dk.setTrangThai(DangKy.TrangThai.DA_TIEP_NHAN);
        dk.setNgayDangKy(LocalDateTime.of(2026, 10, 3, 9, 0));

        h = new HoaHong();
        h.setId(1L);
        h.setDangKyId(100L);
        h.setCtvId(7L);
        h.setSoTien(new BigDecimal("150000"));

        when(repo.findByDangKyId(100L)).thenReturn(Optional.of(h));
        when(cauHinh.soNguyen(eq(CauHinhService.HOA_HONG_PHAN_TRAM_DONG), anyInt())).thenReturn(100);
        when(ctvRepo.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void tinhTien_phanTramLamTronDenNghin_coDinhGiuNguyen() {
        assertThat(HoaHongService.tinhTien(ChinhSachHoaHong.Kieu.PHAN_TRAM, new BigDecimal("7.5"), new BigDecimal("1500000")))
                .isEqualByComparingTo("113000");   // 112.500 -> 113.000
        assertThat(HoaHongService.tinhTien(ChinhSachHoaHong.Kieu.CO_DINH, new BigDecimal("150000"), new BigDecimal("1500000")))
                .isEqualByComparingTo("150000");
    }

    @Test
    void taoHoaHong_chupLaiChinhSachDangHieuLuc() {
        when(repo.findByDangKyId(100L)).thenReturn(Optional.empty());
        ChinhSachHoaHong cs = new ChinhSachHoaHong();
        cs.setId(9L);
        cs.setKieu(ChinhSachHoaHong.Kieu.PHAN_TRAM);
        cs.setGiaTri(new BigDecimal("10"));
        when(csRepo.apDung(CongTacVien.Hang.BAC, "A1", LocalDate.of(2026, 10, 3))).thenReturn(List.of(cs));
        when(repo.save(any())).thenAnswer(inv -> {
            HoaHong moi = inv.getArgument(0);
            assertThat(moi.getChinhSachId()).isEqualTo(9L);
            assertThat(moi.getSoTien()).isEqualByComparingTo("150000");
            assertThat(moi.getCoSo()).isEqualByComparingTo("1500000");
            return moi;
        });
        when(phieuRepo.tongDaThu(100L)).thenReturn(BigDecimal.ZERO);
        service.tinhChoDangKy(dk);
    }

    @Test
    void chuaDongDu_chuaDuDieuKien_dongDu_thiDuDieuKienVaCoKy() {
        when(phieuRepo.tongDaThu(100L)).thenReturn(new BigDecimal("1000000"));
        service.capNhatTrangThai(dk);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);

        when(phieuRepo.tongDaThu(100L)).thenReturn(new BigDecimal("1500000"));
        service.capNhatTrangThai(dk);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.DU_DIEU_KIEN);
        assertThat(h.getKy()).matches("\\d{4}-\\d{2}");
    }

    @Test
    void daDuyetRoiHuyPhieuThu_quayVeChuaDuVaBoDuyet() {
        h.setTrangThai(HoaHong.TrangThai.DA_DUYET);
        h.setNguoiDuyetId(1L);
        when(phieuRepo.tongDaThu(100L)).thenReturn(BigDecimal.ZERO);
        service.capNhatTrangThai(dk);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);
        assertThat(h.getNguoiDuyetId()).isNull();
    }

    @Test
    void hoSoHuy_hoaHongHuy_nhungDaChiThiGiuNguyen() {
        dk.setTrangThai(DangKy.TrangThai.DA_HUY);
        service.capNhatTrangThai(dk);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.HUY);

        h.setTrangThai(HoaHong.TrangThai.DA_CHI);
        service.capNhatTrangThai(dk);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.DA_CHI);
        assertThat(h.getGhiChu()).contains("đối soát");
    }

    @Test
    void hoSoChoDuyet_khongPhatSinhHoaHongDuDaDongTien() {
        dk.setTrangThai(DangKy.TrangThai.CHO_DUYET);
        when(phieuRepo.tongDaThu(100L)).thenReturn(new BigDecimal("1500000"));
        service.capNhatTrangThai(dk);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);
    }

    @Test
    void chiDuyetDuocKhoanDuDieuKien() {
        h.setTrangThai(HoaHong.TrangThai.CHUA_DU_DIEU_KIEN);
        when(repo.khoaTheoId(List.of(1L))).thenReturn(List.of(h));
        assertThatThrownBy(() -> service.duyet(List.of(1L), new AuthUser(1L, "admin", "ADMIN")))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("CHUA_DU_DIEU_KIEN");

        h.setTrangThai(HoaHong.TrangThai.DU_DIEU_KIEN);
        assertThat(service.duyet(List.of(1L), new AuthUser(1L, "admin", "ADMIN"))).isEqualTo(1);
        assertThat(h.getTrangThai()).isEqualTo(HoaHong.TrangThai.DA_DUYET);
    }
}
