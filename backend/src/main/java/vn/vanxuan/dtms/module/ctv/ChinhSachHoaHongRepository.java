package vn.vanxuan.dtms.module.ctv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;

import java.time.LocalDate;
import java.util.List;

public interface ChinhSachHoaHongRepository extends JpaRepository<ChinhSachHoaHong, Long> {

    /** Chinh sach dang hieu luc tai mot ngay: dong co ngay hieu luc gan nhat <= ngay do (phan tu dau). */
    @Query("""
            select c from ChinhSachHoaHong c
            where c.hangCtv = :hangCtv and c.hangGplx = :hangGplx and c.hieuLucTu <= :ngay
            order by c.hieuLucTu desc
            """)
    List<ChinhSachHoaHong> apDung(@Param("hangCtv") CongTacVien.Hang hangCtv, @Param("hangGplx") String hangGplx,
                                  @Param("ngay") LocalDate ngay);

    List<ChinhSachHoaHong> findAllByOrderByHangGplxAscHangCtvAscHieuLucTuDesc();

    boolean existsByHangCtvAndHangGplxAndHieuLucTu(CongTacVien.Hang hangCtv, String hangGplx, LocalDate hieuLucTu);
}
