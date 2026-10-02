package vn.vanxuan.dtms.module.danhmuc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;

@Entity
@Table(name = "giao_vien")
@Getter
@Setter
public class GiaoVien {
    public enum LoaiGiangDay { LY_THUYET, THUC_HANH, CA_HAI }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung nguoiDung;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "so_dien_thoai", nullable = false)
    private String soDienThoai;

    @Column(name = "so_giay_chung_nhan_gv")
    private String soGiayChungNhanGv;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_giang_day", nullable = false)
    private LoaiGiangDay loaiGiangDay = LoaiGiangDay.CA_HAI;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
