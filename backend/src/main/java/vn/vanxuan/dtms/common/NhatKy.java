package vn.vanxuan.dtms.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nhat_ky")
@Getter
@Setter
public class NhatKy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nguoi_dung_id")
    private Long nguoiDungId;

    @Column(name = "hanh_dong", nullable = false)
    private String hanhDong;

    @Column(name = "doi_tuong", nullable = false)
    private String doiTuong;

    @Column(name = "doi_tuong_id")
    private Long doiTuongId;

    @Column(name = "du_lieu", columnDefinition = "json")
    private String duLieu;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();
}
