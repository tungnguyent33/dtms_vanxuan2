package vn.vanxuan.dtms.module.nguoidung;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {
    Optional<VaiTro> findByMa(String ma);
}
