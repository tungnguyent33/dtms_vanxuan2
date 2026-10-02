package vn.vanxuan.dtms.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bo_dem")
@Getter
@Setter
public class BoDem {
    @Id
    private String ten;

    @Column(name = "gia_tri", nullable = false)
    private Long giaTri;
}
