package vn.vanxuan.dtms.module.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Khoa dong: hai tab cung lam moi mot token thi chi mot tab thanh cong. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :hash")
    Optional<RefreshToken> khoaTheoHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken r set r.thuHoi = true where r.nguoiDungId = :ndId and r.thuHoi = false")
    int thuHoiTatCa(@Param("ndId") Long nguoiDungId);

    @Modifying
    @Query("delete from RefreshToken r where r.hetHan < :moc")
    int xoaHetHan(@Param("moc") LocalDateTime moc);
}
