package vn.vanxuan.dtms.module.nguoidung;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    @Query("""
            select n from NguoiDung n
            where (:vaiTro is null or n.vaiTro.ma = :vaiTro)
              and (:q is null or lower(n.hoTen) like lower(concat('%', :q, '%'))
                   or n.tenDangNhap like concat('%', :q, '%')
                   or n.soDienThoai like concat('%', :q, '%'))
            """)
    Page<NguoiDung> timKiem(@Param("vaiTro") String vaiTro, @Param("q") String q, Pageable pageable);

    long countByVaiTroMaAndTrangThaiTrue(String vaiTroMa);

    /** Nguoi nhan thong bao theo vai tro (FR-16). */
    @Query("select n.id from NguoiDung n where n.vaiTro.ma in :vaiTro and n.trangThai = true")
    List<Long> idTheoVaiTro(@Param("vaiTro") List<String> vaiTro);
}
