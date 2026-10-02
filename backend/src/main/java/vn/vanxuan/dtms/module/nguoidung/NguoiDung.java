package vn.vanxuan.dtms.module.nguoidung;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", nullable = false, unique = true)
    private String tenDangNhap;

    @Column(name = "mat_khau_hash", nullable = false)
    private String matKhauHash;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vai_tro_id", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "lan_dang_nhap_cuoi")
    private LocalDateTime lanDangNhapCuoi;
}
