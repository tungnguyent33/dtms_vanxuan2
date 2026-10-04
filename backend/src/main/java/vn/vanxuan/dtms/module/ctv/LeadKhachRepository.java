package vn.vanxuan.dtms.module.ctv;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadKhachRepository extends JpaRepository<LeadKhach, Long> {

    /** Lead con hieu luc cua mot SDT (chong trung: lead "khong thanh" thi duoc gioi thieu lai). */
    @Query("select l from LeadKhach l where l.soDienThoai = :sdt and l.trangThai <> 'KHONG_THANH' order by l.id")
    List<LeadKhach> leadConHieuLuc(@Param("sdt") String soDienThoai);

    @Query("""
            select l from LeadKhach l
            where (:ctvId is null or l.ctvId = :ctvId)
              and (:trangThai is null or l.trangThai = :trangThai)
              and (:q is null or lower(l.hoTen) like lower(concat('%', :q, '%')) or l.soDienThoai like concat('%', :q, '%'))
            order by l.id desc
            """)
    Page<LeadKhach> timKiem(@Param("ctvId") Long ctvId, @Param("trangThai") LeadKhach.TrangThai trangThai,
                            @Param("q") String q, Pageable pageable);

    List<LeadKhach> findByCtvIdOrderByIdDesc(Long ctvId);

    List<LeadKhach> findByHocVienGioiThieuIdOrderByIdDesc(Long hocVienId);

    Optional<LeadKhach> findByDangKyId(Long dangKyId);

    long countByCtvId(Long ctvId);

    long countByCtvIdAndTrangThai(Long ctvId, LeadKhach.TrangThai trangThai);
}
