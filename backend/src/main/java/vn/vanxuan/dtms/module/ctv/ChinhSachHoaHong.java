package vn.vanxuan.dtms.module.ctv;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Muc hoa hong theo hang CTV x hang GPLX, co ngay hieu luc. Doi chinh sach = them dong moi voi ngay hieu luc moi;
 * dong cu giu nguyen nen hoa hong da tinh khong bi sai.
 */
@Entity
@Table(name = "chinh_sach_hoa_hong")
@Getter
@Setter
public class ChinhSachHoaHong {
    public enum Kieu { PHAN_TRAM, CO_DINH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "hang_ctv", nullable = false)
    private CongTacVien.Hang hangCtv;

    @Column(name = "hang_gplx", nullable = false)
    private String hangGplx;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kieu kieu;

    @Column(name = "gia_tri", nullable = false)
    private BigDecimal giaTri;

    @Column(name = "hieu_luc_tu", nullable = false)
    private LocalDate hieuLucTu;

    @Column(name = "nguoi_tao_id")
    private Long nguoiTaoId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
