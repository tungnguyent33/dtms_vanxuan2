package vn.vanxuan.dtms.module.danhmuc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CongTacVienRepository extends JpaRepository<CongTacVien, Long> {

    List<CongTacVien> findByTrangThaiOrderByHoTen(CongTacVien.TrangThai trangThai);

    @Query("""
            select c from CongTacVien c
            where (:trangThai is null or c.trangThai = :trangThai)
              and (:q is null or lower(c.hoTen) like lower(concat('%', :q, '%'))
                   or c.soDienThoai like concat('%', :q, '%') or c.cccd like concat('%', :q, '%'))
            order by c.trangThai, c.hoTen
            """)
    List<CongTacVien> timKiem(@Param("trangThai") CongTacVien.TrangThai trangThai, @Param("q") String q);

    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoaiAndIdNot(String soDienThoai, Long id);

    boolean existsByCccd(String cccd);

    boolean existsByCccdAndIdNot(String cccd, Long id);

    Optional<CongTacVien> findByNguoiDungId(Long nguoiDungId);

    Optional<CongTacVien> findByHocVienId(Long hocVienId);
}
