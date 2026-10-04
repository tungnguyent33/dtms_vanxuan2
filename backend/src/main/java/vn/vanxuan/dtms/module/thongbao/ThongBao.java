package vn.vanxuan.dtms.module.thongbao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Getter
@Setter
public class ThongBao {
    public enum Kenh { WEB, ZALO, SMS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nguoi_nhan_id", nullable = false)
    private Long nguoiNhanId;

    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;

    @Column(name = "noi_dung", nullable = false)
    private String noiDung;

    @Column(nullable = false)
    private String loai = "CHUNG";

    @Column(name = "duong_dan")
    private String duongDan;

    @Column(name = "ma_su_kien")
    private String maSuKien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kenh kenh = Kenh.WEB;

    @Column(name = "da_doc", nullable = false)
    private Boolean daDoc = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
