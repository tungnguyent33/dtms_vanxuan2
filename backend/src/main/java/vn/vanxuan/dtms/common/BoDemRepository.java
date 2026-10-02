package vn.vanxuan.dtms.common;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoDemRepository extends JpaRepository<BoDem, String> {

    /** Tao dong bo dem neu chua co (khong loi neu da ton tai). */
    @Modifying
    @Query(value = "INSERT IGNORE INTO bo_dem (ten, gia_tri) VALUES (:ten, 0)", nativeQuery = true)
    void taoNeuChuaCo(@Param("ten") String ten);

    /** SELECT ... FOR UPDATE: khoa dong den khi transaction ket thuc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BoDem b where b.ten = :ten")
    Optional<BoDem> khoaDeCapSo(@Param("ten") String ten);
}
