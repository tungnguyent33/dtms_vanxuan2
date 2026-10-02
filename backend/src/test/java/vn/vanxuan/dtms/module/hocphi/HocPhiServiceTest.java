package vn.vanxuan.dtms.module.hocphi;

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
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** BR-10: so tien thu khong vuot so con no; ho so da huy khong duoc thu. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HocPhiServiceTest {

    @Mock PhieuThuRepository phieuRepo;
    @Mock DangKyRepository dangKyRepo;
    @Mock NguoiDungRepository nguoiDungRepo;
    @Mock SoThuTuService soThuTu;
    @Mock NhatKyService nhatKy;
    @InjectMocks HocPhiService service;

    private final AuthUser leTan = new AuthUser(2L, "letan01", "LE_TAN");

    private DangKy dangKy(DangKy.TrangThai tt) {
        DangKy dk = new DangKy();
        dk.setId(1L);
        dk.setHocPhi(new BigDecimal("1500000"));
        dk.setGiamTru(new BigDecimal("100000"));
        dk.setTrangThai(tt);
        return dk;
    }

    @Test
    void tinhCongNo_truGiamTruVaDaDong() {
        DangKy dk = dangKy(DangKy.TrangThai.DANG_HOC);
        when(phieuRepo.tongDaThu(1L)).thenReturn(new BigDecimal("500000"));
        var cn = service.tinhCongNo(dk);
        assertThat(cn.phaiDong()).isEqualByComparingTo("1400000");
        assertThat(cn.conNo()).isEqualByComparingTo("900000");
    }

    @Test
    void thuVuotSoConNo_biTuChoi() {
        when(dangKyRepo.khoaDeThuTien(1L)).thenReturn(Optional.of(dangKy(DangKy.TrangThai.DANG_HOC)));
        when(phieuRepo.tongDaThu(1L)).thenReturn(new BigDecimal("1000000"));
        var req = new HocPhiDtos.LapPhieuRequest(1L, new BigDecimal("500000"), PhieuThu.HinhThuc.TIEN_MAT, null);
        assertThatThrownBy(() -> service.lapPhieu(req, leTan))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SO_TIEN_KHONG_HOP_LE");
    }

    @Test
    void hoSoDaHuy_khongDuocThu() {
        when(dangKyRepo.khoaDeThuTien(1L)).thenReturn(Optional.of(dangKy(DangKy.TrangThai.DA_HUY)));
        var req = new HocPhiDtos.LapPhieuRequest(1L, new BigDecimal("100000"), PhieuThu.HinhThuc.TIEN_MAT, null);
        assertThatThrownBy(() -> service.lapPhieu(req, leTan))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("HO_SO_DA_HUY");
    }
}
