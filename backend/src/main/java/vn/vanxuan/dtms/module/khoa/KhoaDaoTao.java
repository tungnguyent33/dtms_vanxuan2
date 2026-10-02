package vn.vanxuan.dtms.module.khoa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "khoa_dao_tao")
@Getter
@Setter
public class KhoaDaoTao {
    public enum TrangThai { DU_KIEN, DANG_TUYEN, DANG_DAO_TAO, DA_KET_THUC, HUY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_khoa", nullable = false, unique = true)
    private String maKhoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hang_ma", nullable = false)
    private HangGplx hang;

    @Column(name = "ngay_khai_giang", nullable = false)
    private LocalDate ngayKhaiGiang;

    @Column(name = "ngay_be_giang", nullable = false)
    private LocalDate ngayBeGiang;

    @Column(name = "si_so_toi_da", nullable = false)
    private Integer siSoToiDa;

    @Column(name = "hoc_phi", nullable = false)
    private BigDecimal hocPhi;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.DU_KIEN;

    @Column(name = "ngay_bao_cao_so")
    private LocalDate ngayBaoCaoSo;

    @Column(name = "ghi_chu")
    private String ghiChu;

    /** So ngay cua khoa, tinh ca ngay dau va ngay cuoi. */
    public long soNgay() {
        return ChronoUnit.DAYS.between(ngayKhaiGiang, ngayBeGiang) + 1;
    }
}
