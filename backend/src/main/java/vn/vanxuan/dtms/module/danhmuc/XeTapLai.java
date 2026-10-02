package vn.vanxuan.dtms.module.danhmuc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "xe_tap_lai")
@Getter
@Setter
public class XeTapLai {
    public enum TrangThaiXe { SAN_SANG, BAO_DUONG, NGUNG }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bien_so", nullable = false, unique = true)
    private String bienSo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hang_ma", nullable = false)
    private HangGplx hang;

    @Column(name = "nhan_hieu")
    private String nhanHieu;

    @Column(name = "nam_san_xuat")
    private Integer namSanXuat;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiXe trangThai = TrangThaiXe.SAN_SANG;

    @Column(name = "ngay_bao_duong_tiep")
    private LocalDate ngayBaoDuongTiep;
}
