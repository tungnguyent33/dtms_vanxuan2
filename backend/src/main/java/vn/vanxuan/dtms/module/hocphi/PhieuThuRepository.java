package vn.vanxuan.dtms.module.hocphi;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PhieuThuRepository extends JpaRepository<PhieuThu, Long> {

    @Query("select sum(p.soTien) from PhieuThu p where p.dangKy.id = :dangKyId and p.trangThai = :tt")
    BigDecimal tongTheoTrangThai(@Param("dangKyId") Long dangKyId, @Param("tt") PhieuThu.TrangThai tt);

    /** Tong tien da thu (chi tinh phieu con hieu luc). */
    default BigDecimal tongDaThu(Long dangKyId) {
        BigDecimal tong = tongTheoTrangThai(dangKyId, PhieuThu.TrangThai.HIEU_LUC);
        return tong == null ? BigDecimal.ZERO : tong;   // chua co phieu nao -> SUM tra ve null
    }

    @EntityGraph(attributePaths = {"nguoiThu"})
    List<PhieuThu> findByDangKyIdOrderByNgayThuDesc(Long dangKyId);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.hocVien", "dangKy.khoa", "dangKy.khoa.hang", "nguoiThu"})
    @Query("select p from PhieuThu p where p.id = :id")
    Optional<PhieuThu> findChiTiet(@Param("id") Long id);
}
