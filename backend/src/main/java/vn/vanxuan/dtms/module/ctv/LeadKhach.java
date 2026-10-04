package vn.vanxuan.dtms.module.ctv;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Khach tiem nang do CTV, hoc vien hoac van phong ghi nhan. Chot thanh dang ky thi gan dangKyId. */
@Entity
@Table(name = "lead_khach")
@Getter
@Setter
public class LeadKhach {
    public enum Nguon { CTV, HOC_VIEN, VAN_PHONG }

    public enum TrangThai { MOI, DA_LIEN_HE, DA_CHOT, KHONG_THANH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "so_dien_thoai", nullable = false)
    private String soDienThoai;

    @Column(name = "dia_chi")
    private String diaChi;

    @Column(name = "hang_muon_hoc")
    private String hangMuonHoc;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nguon nguon;

    @Column(name = "ctv_id")
    private Long ctvId;

    @Column(name = "hoc_vien_gioi_thieu_id")
    private Long hocVienGioiThieuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.MOI;

    @Column(name = "dang_ky_id")
    private Long dangKyId;

    @Column(name = "nguoi_nhap_id")
    private Long nguoiNhapId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
