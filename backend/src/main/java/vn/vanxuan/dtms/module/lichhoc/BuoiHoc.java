package vn.vanxuan.dtms.module.lichhoc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.danhmuc.GiaoVien;
import vn.vanxuan.dtms.module.danhmuc.XeTapLai;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "buoi_hoc")
@Getter
@Setter
public class BuoiHoc {
    public enum Loai { LY_THUYET, THUC_HANH }

    public enum TrangThai { KE_HOACH, DA_DAY, HUY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khoa_id")
    private KhoaDaoTao khoa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Loai loai;

    @Column(nullable = false)
    private LocalDate ngay;

    @Column(name = "gio_bat_dau", nullable = false)
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalTime gioKetThuc;

    @Column(name = "dia_diem", nullable = false)
    private String diaDiem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giao_vien_id")
    private GiaoVien giaoVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xe_id")
    private XeTapLai xe;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.KE_HOACH;

    /** Thoi luong buoi hoc (gio), lam tron 2 chu so. */
    public BigDecimal thoiLuongGio() {
        long phut = Duration.between(gioBatDau, gioKetThuc).toMinutes();
        return BigDecimal.valueOf(phut).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
