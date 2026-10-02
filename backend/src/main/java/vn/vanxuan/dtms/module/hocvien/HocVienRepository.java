package vn.vanxuan.dtms.module.hocvien;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HocVienRepository extends JpaRepository<HocVien, Long> {
    Optional<HocVien> findByCccd(String cccd);

    Optional<HocVien> findByNguoiDungId(Long nguoiDungId);
}
