package vn.vanxuan.dtms.module.ctv;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hoa hong cua mot dang ky do CTV gioi thieu. Tinh tu dong, khong nhap tay.
 * CHUA_DU_DIEU_KIEN -> DU_DIEU_KIEN (hoc vien dong du % hoc phi) -> DA_DUYET (admin) -> DA_CHI; HUY khi ho so huy.
 */
@Entity
@Table(name = "hoa_hong")
@Getter
@Setter
public class HoaHong {
    public enum TrangThai { CHUA_DU_DIEU_KIEN, DU_DIEU_KIEN, DA_DUYET, DA_CHI, HUY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dang_ky_id", nullable = false, unique = true)
    private Long dangKyId;

    @Column(name = "ctv_id", nullable = false)
    private Long ctvId;

    @Column(name = "chinh_sach_id")
    private Long chinhSachId;

    @Enumerated(EnumType.STRING)
    private ChinhSachHoaHong.Kieu kieu;

    @Column(name = "gia_tri")
    private BigDecimal giaTri;

    @Column(name = "co_so", nullable = false)
    private BigDecimal coSo = BigDecimal.ZERO;

    @Column(name = "so_tien", nullable = false)
    private BigDecimal soTien = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.CHUA_DU_DIEU_KIEN;

    @Column(name = "ngay_du_dieu_kien")
    private LocalDate ngayDuDieuKien;

    /** yyyy-MM: thang hoa hong du dieu kien (ky chot). */
    private String ky;

    @Column(name = "nguoi_duyet_id")
    private Long nguoiDuyetId;

    @Column(name = "ngay_duyet")
    private LocalDateTime ngayDuyet;

    @Column(name = "chi_hoa_hong_id")
    private Long chiHoaHongId;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
