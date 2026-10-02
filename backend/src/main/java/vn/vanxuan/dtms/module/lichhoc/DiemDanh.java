package vn.vanxuan.dtms.module.lichhoc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "diem_danh")
@Getter
@Setter
public class DiemDanh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buoi_hoc_id")
    private BuoiHoc buoiHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dang_ky_id")
    private DangKy dangKy;

    @Column(name = "co_mat", nullable = false)
    private Boolean coMat;

    @Column(name = "so_gio", nullable = false)
    private BigDecimal soGio = BigDecimal.ZERO;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_ghi_id")
    private NguoiDung nguoiGhi;

    @Column(name = "ghi_luc", nullable = false)
    private LocalDateTime ghiLuc = LocalDateTime.now();
}
