package vn.vanxuan.dtms.module.danhmuc;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiaoVienRepository extends JpaRepository<GiaoVien, Long> {
    @EntityGraph(attributePaths = "nguoiDung")
    List<GiaoVien> findByTrangThaiTrueOrderByHoTen();

    @EntityGraph(attributePaths = "nguoiDung")
    List<GiaoVien> findAllByOrderByTrangThaiDescHoTen();

    Optional<GiaoVien> findByNguoiDungId(Long nguoiDungId);
}
