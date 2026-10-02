package vn.vanxuan.dtms.module.danhmuc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CongTacVienRepository extends JpaRepository<CongTacVien, Long> {
    List<CongTacVien> findByTrangThaiTrueOrderByHoTen();
}
