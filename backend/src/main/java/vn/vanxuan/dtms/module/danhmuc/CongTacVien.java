package vn.vanxuan.dtms.module.danhmuc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "cong_tac_vien")
@Getter
@Setter
public class CongTacVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "so_dien_thoai", nullable = false, unique = true)
    private String soDienThoai;

    @Column(name = "dia_ban")
    private String diaBan;

    @Column(name = "muc_hoa_hong", nullable = false)
    private BigDecimal mucHoaHong = BigDecimal.ZERO;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
