package vn.vanxuan.dtms.module.ctv;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Phieu chi hoa hong cho mot CTV trong mot ky. Ghi ngay chi, so tien, nguoi chi. */
@Entity
@Table(name = "chi_hoa_hong")
@Getter
@Setter
public class ChiHoaHong {
    public enum HinhThuc { TIEN_MAT, CHUYEN_KHOAN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "so_phieu", nullable = false, unique = true)
    private String soPhieu;

    @Column(name = "ctv_id", nullable = false)
    private Long ctvId;

    @Column(nullable = false)
    private String ky;

    @Column(name = "so_tien", nullable = false)
    private BigDecimal soTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "hinh_thuc", nullable = false)
    private HinhThuc hinhThuc;

    @Column(name = "ngay_chi", nullable = false)
    private LocalDate ngayChi;

    @Column(name = "nguoi_chi_id", nullable = false)
    private Long nguoiChiId;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
