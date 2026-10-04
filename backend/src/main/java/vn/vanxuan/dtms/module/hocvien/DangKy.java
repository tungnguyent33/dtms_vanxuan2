package vn.vanxuan.dtms.module.hocvien;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ho so hoc cua MOT hoc vien trong MOT khoa - trung tam cua mo hinh du lieu. */
@Entity
@Table(name = "dang_ky")
@Getter
@Setter
public class DangKy {
    public enum HinhThucLyThuyet { TU_HOC, TAP_TRUNG }

    public enum Nguon { TRUC_TIEP, TRUC_TUYEN, CTV, HOC_VIEN_GIOI_THIEU }

    public enum TrangThai {
        CHO_DUYET, DA_TIEP_NHAN, DANG_HOC, CHUA_DAT, HOAN_THANH, SAT_HACH_TRUOT, DA_SAT_HACH_DAT, DA_HUY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ho_so", nullable = false, unique = true)
    private String maHoSo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoc_vien_id")
    private HocVien hocVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khoa_id")
    private KhoaDaoTao khoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "hinh_thuc_ly_thuyet", nullable = false)
    private HinhThucLyThuyet hinhThucLyThuyet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nguon nguon = Nguon.TRUC_TIEP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ctv_id")
    private CongTacVien ctv;

    @Column(name = "hoc_phi", nullable = false)
    private BigDecimal hocPhi;

    @Column(name = "giam_tru", nullable = false)
    private BigDecimal giamTru = BigDecimal.ZERO;

    @Column(name = "ly_do_giam_tru")
    private String lyDoGiamTru;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.DA_TIEP_NHAN;

    @Column(name = "ngay_dang_ky", nullable = false)
    private LocalDateTime ngayDangKy = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tiep_nhan_id")
    private NguoiDung nguoiTiepNhan;

    @Column(name = "so_giay_xac_nhan", unique = true)
    private String soGiayXacNhan;

    @Column(name = "ngay_hoan_thanh")
    private LocalDate ngayHoanThanh;

    @Column(name = "ghi_chu")
    private String ghiChu;

    /** Hoc phi phai dong sau giam tru. */
    /** Nguon HOC_VIEN_GIOI_THIEU: hoc vien da gioi thieu nguoi nay (thong ke, khong tu sinh hoa hong). */
    @Column(name = "gioi_thieu_hoc_vien_id")
    private Long gioiThieuHocVienId;

    public BigDecimal phaiDong() {
        return hocPhi.subtract(giamTru);
    }
}
