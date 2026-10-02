package vn.vanxuan.dtms.module.hocphi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_thu")
@Getter
@Setter
public class PhieuThu {
    public enum HinhThuc { TIEN_MAT, CHUYEN_KHOAN, VNPAY }

    public enum TrangThai { HIEU_LUC, DA_HUY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "so_phieu", nullable = false, unique = true)
    private String soPhieu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dang_ky_id")
    private DangKy dangKy;

    @Column(name = "so_tien", nullable = false)
    private BigDecimal soTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "hinh_thuc", nullable = false)
    private HinhThuc hinhThuc;

    @Column(name = "noi_dung")
    private String noiDung;

    @Column(name = "ngay_thu", nullable = false)
    private LocalDateTime ngayThu = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_thu_id")
    private NguoiDung nguoiThu;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.HIEU_LUC;

    @Column(name = "ly_do_huy")
    private String lyDoHuy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_huy_id")
    private NguoiDung nguoiHuy;
}
