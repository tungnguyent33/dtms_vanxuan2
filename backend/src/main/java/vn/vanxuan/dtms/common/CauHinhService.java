package vn.vanxuan.dtms.common;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tham so he thong dang khoa - gia tri (bang cau_hinh), quan tri vien chinh tren giao dien. */
@Service
public class CauHinhService {

    public static final String HOA_HONG_PHAN_TRAM_DONG = "HOA_HONG_PHAN_TRAM_DONG";
    public static final String HOA_HONG_NGAY_CHI = "HOA_HONG_NGAY_CHI";

    private final NamedParameterJdbcTemplate jdbc;

    public CauHinhService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int soNguyen(String khoa, int macDinh) {
        List<String> v = jdbc.queryForList("SELECT gia_tri FROM cau_hinh WHERE khoa = :k",
                new MapSqlParameterSource("k", khoa), String.class);
        try {
            return v.isEmpty() ? macDinh : Integer.parseInt(v.get(0).trim());
        } catch (NumberFormatException e) {
            return macDinh;
        }
    }

    @Transactional
    public void dat(String khoa, String giaTri) {
        int n = jdbc.update("UPDATE cau_hinh SET gia_tri = :v WHERE khoa = :k",
                new MapSqlParameterSource("k", khoa).addValue("v", giaTri));
        if (n == 0) throw new NotFoundException("cấu hình", khoa);
    }
}
