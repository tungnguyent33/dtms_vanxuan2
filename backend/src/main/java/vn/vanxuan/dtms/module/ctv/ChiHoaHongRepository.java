package vn.vanxuan.dtms.module.ctv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChiHoaHongRepository extends JpaRepository<ChiHoaHong, Long> {
    List<ChiHoaHong> findByCtvIdOrderByIdDesc(Long ctvId);

    List<ChiHoaHong> findAllByOrderByIdDesc();
}
