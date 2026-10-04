package vn.vanxuan.dtms.module.hocvien;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.SoThuTuService;
import vn.vanxuan.dtms.module.danhmuc.CongTacVienRepository;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;
import vn.vanxuan.dtms.module.hocphi.PhieuThuRepository;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTaoRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.module.thongbao.ThongBaoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

/** Kiem thu don vi quy tac nghiep vu khi tiep nhan ho so (BR-02, BR-04) - khong can CSDL. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DangKyServiceTest {

    @Mock DangKyRepository dangKyRepo;
    @Mock HocVienRepository hocVienRepo;
    @Mock KhoaDaoTaoRepository khoaRepo;
    @Mock CongTacVienRepository ctvRepo;
    @Mock NguoiDungRepository nguoiDungRepo;
    @Mock PhieuThuRepository phieuThuRepo;
    @Mock SoThuTuService soThuTu;
    @Mock NhatKyService nhatKy;
    @Mock ThongBaoService thongBao;
    @Mock vn.vanxuan.dtms.module.ctv.LeadService leadService;
    @Mock vn.vanxuan.dtms.module.ctv.HoaHongService hoaHong;
    @InjectMocks DangKyService service;

    private final AuthUser leTan = new AuthUser(2L, "letan01", "LE_TAN");
    private KhoaDaoTao khoa;

    @BeforeEach
    void setUp() {
        HangGplx a1 = new HangGplx();
        a1.setMa("A1");
        a1.setTuoiToiThieu(18);
        a1.setSoNgayKhoaToiDa(10);
        a1.setGioLyThuyet(new BigDecimal("9"));
        a1.setGioThucHanh(new BigDecimal("3"));

        khoa = new KhoaDaoTao();
        khoa.setId(1L);
        khoa.setMaKhoa("A1-TEST");
        khoa.setHang(a1);
        khoa.setNgayKhaiGiang(LocalDate.of(2026, 10, 5));
        khoa.setNgayBeGiang(LocalDate.of(2026, 10, 11));
        khoa.setSiSoToiDa(60);
        khoa.setHocPhi(new BigDecimal("1500000"));
        khoa.setTrangThai(KhoaDaoTao.TrangThai.DANG_TUYEN);

        when(khoaRepo.khoaDeXepHocVien(1L)).thenReturn(Optional.of(khoa));
        when(dangKyRepo.countByKhoaIdAndTrangThaiNot(any(), any())).thenReturn(0L);
        when(hocVienRepo.findByCccd(anyString())).thenReturn(Optional.empty());
        when(hocVienRepo.save(any())).thenAnswer(returnsFirstArg());
        when(dangKyRepo.save(any())).thenAnswer(returnsFirstArg());
        when(soThuTu.capMa(anyString(), anyString(), anyInt())).thenReturn("MA-TEST");
        when(nguoiDungRepo.getReferenceById(any())).thenReturn(new NguoiDung());
    }

    private DangKyDtos.TaoDangKyRequest request(LocalDate ngaySinh) {
        var hv = new DangKyDtos.HocVienInput("Nguyễn Văn Test", ngaySinh, HocVien.GioiTinh.NAM,
                "025200000999", null, "Tam Nông, Phú Thọ", "0912345678", null);
        return new DangKyDtos.TaoDangKyRequest(hv, 1L, DangKy.HinhThucLyThuyet.TU_HOC,
                DangKy.Nguon.TRUC_TIEP, null, null, null, null, null);
    }

    @Test
    void tiepNhanThanhCong_layHocPhiTheoKhoa() {
        var res = service.taoDangKy(request(LocalDate.of(2000, 1, 1)), leTan);
        assertThat(res.trangThai()).isEqualTo("DA_TIEP_NHAN");
        assertThat(res.hocPhi()).isEqualByComparingTo("1500000");
        assertThat(res.hang()).isEqualTo("A1");
    }

    @Test
    void chuaDu18TuoiTaiNgayBeGiang_biTuChoi() {
        // Tron 18 tuoi vao 12/10/2026 - sau ngay be giang 11/10/2026 mot ngay
        assertThatThrownBy(() -> service.taoDangKy(request(LocalDate.of(2008, 10, 12)), leTan))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("HV_CHUA_DU_TUOI");
    }

    @Test
    void du18TuoiDungNgayBeGiang_duocNhan() {
        var res = service.taoDangKy(request(LocalDate.of(2008, 10, 11)), leTan);
        assertThat(res.trangThai()).isEqualTo("DA_TIEP_NHAN");
    }

    @Test
    void khoaDuSiSo_biTuChoi() {
        when(dangKyRepo.countByKhoaIdAndTrangThaiNot(any(), any())).thenReturn(60L);
        assertThatThrownBy(() -> service.taoDangKy(request(LocalDate.of(2000, 1, 1)), leTan))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("KHOA_DU_SI_SO");
    }

    @Test
    void khoaDuSiSo_goiYKhoaKeTiepCungHangConCho() {
        KhoaDaoTao keTiep = new KhoaDaoTao();
        keTiep.setId(2L);
        keTiep.setMaKhoa("A1-TIEP");
        keTiep.setHang(khoa.getHang());
        keTiep.setNgayKhaiGiang(LocalDate.of(2026, 10, 20));
        keTiep.setSiSoToiDa(60);
        when(dangKyRepo.countByKhoaIdAndTrangThaiNot(eq(1L), any())).thenReturn(60L);
        when(dangKyRepo.countByKhoaIdAndTrangThaiNot(eq(2L), any())).thenReturn(55L);
        when(khoaRepo.khoaCungHang(eq("A1"), eq(1L), any(), any())).thenReturn(java.util.List.of(keTiep));

        assertThatThrownBy(() -> service.taoDangKy(request(LocalDate.of(2000, 1, 1)), leTan))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("KHOA_DU_SI_SO");
                    assertThat(e.getDetails()).containsEntry("goiYKhoaId", "2").containsEntry("goiYConCho", "5");
                });
    }

    @Test
    void khoaDaKetThuc_khongNhanHocVien() {
        khoa.setTrangThai(KhoaDaoTao.TrangThai.DA_KET_THUC);
        assertThatThrownBy(() -> service.taoDangKy(request(LocalDate.of(2000, 1, 1)), leTan))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("KHOA_KHONG_TUYEN");
    }
}
