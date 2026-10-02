package vn.vanxuan.dtms.module.hocvien;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "hoc_vien")
@Getter
@Setter
public class HocVien {
    public enum GioiTinh { NAM, NU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hoc_vien", nullable = false, unique = true)
    private String maHocVien;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "ngay_sinh", nullable = false)
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "gioi_tinh", nullable = false)
    private GioiTinh gioiTinh;

    @Column(nullable = false, unique = true, length = 12)
    private String cccd;

    @Column(name = "ngay_cap_cccd")
    private LocalDate ngayCapCccd;

    @Column(name = "dia_chi", nullable = false)
    private String diaChi;

    @Column(name = "so_dien_thoai", nullable = false)
    private String soDienThoai;

    private String email;

    @Column(name = "anh_chan_dung_url")
    private String anhChanDungUrl;

    @Column(name = "anh_cccd_truoc_url")
    private String anhCccdTruocUrl;

    @Column(name = "anh_cccd_sau_url")
    private String anhCccdSauUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung nguoiDung;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Tuoi tron tai mot ngay (dung kiem tra BR-02). */
    public int tuoiTai(LocalDate ngay) {
        return Period.between(ngaySinh, ngay).getYears();
    }
}
