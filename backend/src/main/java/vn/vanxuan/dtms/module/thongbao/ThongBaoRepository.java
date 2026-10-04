package vn.vanxuan.dtms.module.thongbao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    Page<ThongBao> findByNguoiNhanIdOrderByIdDesc(Long nguoiNhanId, Pageable pageable);

    Page<ThongBao> findByNguoiNhanIdAndDaDocFalseOrderByIdDesc(Long nguoiNhanId, Pageable pageable);

    long countByNguoiNhanIdAndDaDocFalse(Long nguoiNhanId);

    boolean existsByNguoiNhanIdAndMaSuKien(Long nguoiNhanId, String maSuKien);

    Optional<ThongBao> findByIdAndNguoiNhanId(Long id, Long nguoiNhanId);

    @Modifying
    @Query("update ThongBao t set t.daDoc = true where t.nguoiNhanId = :nd and t.daDoc = false")
    int danhDauDaDocTatCa(@Param("nd") Long nguoiNhanId);
}
