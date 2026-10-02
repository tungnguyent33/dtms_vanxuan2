package vn.vanxuan.dtms.module.nguoidung;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vai_tro")
@Getter
@Setter
public class VaiTro {
    public static final String ADMIN = "ADMIN";
    public static final String LE_TAN = "LE_TAN";
    public static final String GIAO_VIEN = "GIAO_VIEN";
    public static final String HOC_VIEN = "HOC_VIEN";

    @Id
    private Integer id;
    private String ma;
    private String ten;
}
