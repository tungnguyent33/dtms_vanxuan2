package vn.vanxuan.dtms.module.danhmuc;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface XeTapLaiRepository extends JpaRepository<XeTapLai, Long> {
    @EntityGraph(attributePaths = "hang")
    List<XeTapLai> findAllByOrderByBienSo();

    boolean existsByBienSo(String bienSo);

    boolean existsByBienSoAndIdNot(String bienSo, Long id);
}
