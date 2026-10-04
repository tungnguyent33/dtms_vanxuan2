package vn.vanxuan.dtms.module.khoa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KhoaDaoTaoRepository extends JpaRepository<KhoaDaoTao, Long> {

    @EntityGraph(attributePaths = "hang")
    @Query("select k from KhoaDaoTao k where (:trangThai is null or k.trangThai = :trangThai) " +
           "order by k.ngayKhaiGiang desc")
    List<KhoaDaoTao> timKiem(@Param("trangThai") KhoaDaoTao.TrangThai trangThai);

    @EntityGraph(attributePaths = "hang")
    @Query("select k from KhoaDaoTao k where k.id = :id")
    Optional<KhoaDaoTao> findWithHang(@Param("id") Long id);

    /** Khoa dong khoa hoc khi xep hoc vien, tranh vuot si so khi 2 le tan cung xep (BR-04). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from KhoaDaoTao k join fetch k.hang where k.id = :id")
    Optional<KhoaDaoTao> khoaDeXepHocVien(@Param("id") Long id);

    boolean existsByMaKhoa(String maKhoa);

    /** UC05 - 5b: cac khoa cung hang co the goi y khi khoa da du si so (som nhat truoc). */
    @EntityGraph(attributePaths = "hang")
    @Query("""
            select k from KhoaDaoTao k
            where k.hang.ma = :hang and k.id <> :boQuaId and k.trangThai in :trangThai
              and k.ngayKhaiGiang >= :tuNgay
            order by k.ngayKhaiGiang
            """)
    List<KhoaDaoTao> khoaCungHang(@Param("hang") String hang, @Param("boQuaId") Long boQuaId,
                                  @Param("trangThai") java.util.Collection<KhoaDaoTao.TrangThai> trangThai,
                                  @Param("tuNgay") java.time.LocalDate tuNgay);
}
