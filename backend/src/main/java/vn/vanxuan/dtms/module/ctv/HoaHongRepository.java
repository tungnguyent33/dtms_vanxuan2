package vn.vanxuan.dtms.module.ctv;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HoaHongRepository extends JpaRepository<HoaHong, Long> {

    Optional<HoaHong> findByDangKyId(Long dangKyId);

    List<HoaHong> findByCtvIdOrderByIdDesc(Long ctvId);

    @Query("""
            select h from HoaHong h
            where (:ctvId is null or h.ctvId = :ctvId)
              and (:ky is null or h.ky = :ky)
              and (:trangThai is null or h.trangThai = :trangThai)
            order by h.id desc
            """)
    List<HoaHong> timKiem(@Param("ctvId") Long ctvId, @Param("ky") String ky,
                          @Param("trangThai") HoaHong.TrangThai trangThai);

    /** Khoa dong khi chi tien: hai nguoi bam "chi" cung luc khong chi trung. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoaHong h where h.ctvId = :ctvId and h.trangThai = 'DA_DUYET' and h.ky <= :ky")
    List<HoaHong> khoaDeChi(@Param("ctvId") Long ctvId, @Param("ky") String ky);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoaHong h where h.id in :ids")
    List<HoaHong> khoaTheoId(@Param("ids") Collection<Long> ids);
}
