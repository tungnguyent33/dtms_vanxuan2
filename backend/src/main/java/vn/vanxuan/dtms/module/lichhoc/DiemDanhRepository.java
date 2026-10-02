package vn.vanxuan.dtms.module.lichhoc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiemDanhRepository extends JpaRepository<DiemDanh, Long> {

    List<DiemDanh> findByBuoiHocId(Long buoiHocId);

    Optional<DiemDanh> findByBuoiHocIdAndDangKyId(Long buoiHocId, Long dangKyId);

    /** Tong so gio co mat theo loai buoi (LY_THUYET / THUC_HANH) cua mot dang ky. */
    @Query("""
            select b.loai, sum(d.soGio) from DiemDanh d join d.buoiHoc b
            where d.dangKy.id = :dangKyId and d.coMat = true
            group by b.loai
            """)
    List<Object[]> tongGioTheoLoai(@Param("dangKyId") Long dangKyId);
}
