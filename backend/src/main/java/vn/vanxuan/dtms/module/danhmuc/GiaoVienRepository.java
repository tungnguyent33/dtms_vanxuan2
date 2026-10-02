package vn.vanxuan.dtms.module.danhmuc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiaoVienRepository extends JpaRepository<GiaoVien, Long> {
    List<GiaoVien> findByTrangThaiTrueOrderByHoTen();

    Optional<GiaoVien> findByNguoiDungId(Long nguoiDungId);
}
