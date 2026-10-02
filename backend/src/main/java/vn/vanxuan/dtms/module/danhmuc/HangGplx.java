package vn.vanxuan.dtms.module.danhmuc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Hang GPLX (A1, A). So gio de dang cau hinh vi quy dinh thay doi theo thong tu. */
@Entity
@Table(name = "hang_gplx")
@Getter
@Setter
public class HangGplx {
    @Id
    private String ma;

    @Column(nullable = false)
    private String ten;

    @Column(name = "gio_ly_thuyet", nullable = false)
    private BigDecimal gioLyThuyet;

    @Column(name = "gio_thuc_hanh", nullable = false)
    private BigDecimal gioThucHanh;

    @Column(name = "tuoi_toi_thieu", nullable = false)
    private Integer tuoiToiThieu;

    @Column(name = "so_ngay_khoa_toi_da", nullable = false)
    private Integer soNgayKhoaToiDa;

    @Column(name = "hoc_phi_mac_dinh", nullable = false)
    private BigDecimal hocPhiMacDinh;

    @Column(name = "can_cu_phap_ly")
    private String canCuPhapLy;

    @Column(name = "dang_ap_dung", nullable = false)
    private Boolean dangApDung = true;
}
