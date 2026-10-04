package vn.vanxuan.dtms.module.danhmuc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ho so cong tac vien tuyen sinh. Vong doi: CHO_DUYET (le tan tao / hoc vien xin) -> HOAT_DONG (admin duyet)
 * <-> TAM_KHOA -> NGUNG. Khong xoa cung de giu lich su hoa hong.
 */
@Entity
@Table(name = "cong_tac_vien")
@Getter
@Setter
public class CongTacVien {
    public enum TrangThai { CHO_DUYET, HOAT_DONG, TAM_KHOA, NGUNG }

    public enum Loai { HOC_VIEN_CU, SINH_VIEN, DOI_TAC }

    /** Hang CTV quyet dinh muc hoa hong (bang chinh_sach_hoa_hong). */
    public enum Hang { THUONG, BAC, VANG }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "so_dien_thoai", nullable = false, unique = true)
    private String soDienThoai;

    private String zalo;

    private String cccd;

    @Column(name = "dia_chi")
    private String diaChi;

    @Column(name = "dia_ban")
    private String diaBan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Loai loai = Loai.DOI_TAC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Hang hang = Hang.THUONG;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.CHO_DUYET;

    @Column(name = "ngan_hang")
    private String nganHang;

    @Column(name = "so_tai_khoan")
    private String soTaiKhoan;

    @Column(name = "chu_tai_khoan")
    private String chuTaiKhoan;

    @Column(name = "cam_ket_url")
    private String camKetUrl;

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "hoc_vien_id")
    private Long hocVienId;

    @Column(name = "nguoi_dung_id")
    private Long nguoiDungId;

    @Column(name = "nguoi_tao_id")
    private Long nguoiTaoId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "nguoi_duyet_id")
    private Long nguoiDuyetId;

    @Column(name = "ngay_duyet")
    private LocalDateTime ngayDuyet;

    public boolean dangHoatDong() {
        return trangThai == TrangThai.HOAT_DONG;
    }
}
