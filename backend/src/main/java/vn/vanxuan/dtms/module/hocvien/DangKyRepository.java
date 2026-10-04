package vn.vanxuan.dtms.module.hocvien;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DangKyRepository extends JpaRepository<DangKy, Long> {

    /** Tim kiem co phan trang: theo khoa, trang thai, va tu khoa (ho ten / CCCD / SDT / ma ho so). */
    @EntityGraph(attributePaths = {"hocVien", "khoa", "khoa.hang"})
    @Query(value = """
            select d from DangKy d join d.hocVien hv
            where (:khoaId is null or d.khoa.id = :khoaId)
              and (:trangThai is null or d.trangThai = :trangThai)
              and (:q is null or lower(hv.hoTen) like lower(concat('%', :q, '%'))
                   or hv.cccd like concat('%', :q, '%')
                   or hv.soDienThoai like concat('%', :q, '%')
                   or d.maHoSo like concat('%', :q, '%'))
            """,
            countQuery = """
            select count(d) from DangKy d join d.hocVien hv
            where (:khoaId is null or d.khoa.id = :khoaId)
              and (:trangThai is null or d.trangThai = :trangThai)
              and (:q is null or lower(hv.hoTen) like lower(concat('%', :q, '%'))
                   or hv.cccd like concat('%', :q, '%')
                   or hv.soDienThoai like concat('%', :q, '%')
                   or d.maHoSo like concat('%', :q, '%'))
            """)
    Page<DangKy> timKiem(@Param("khoaId") Long khoaId, @Param("trangThai") DangKy.TrangThai trangThai,
                         @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"hocVien", "khoa", "khoa.hang", "ctv"})
    @Query("select d from DangKy d where d.id = :id")
    Optional<DangKy> findChiTiet(@Param("id") Long id);

    /** Khach tra cuu ho so: phai khop ca ma ho so va CCCD. */
    @EntityGraph(attributePaths = {"hocVien", "khoa", "khoa.hang"})
    @Query("select d from DangKy d where d.maHoSo = :maHoSo and d.hocVien.cccd = :cccd")
    Optional<DangKy> traCuu(@Param("maHoSo") String maHoSo, @Param("cccd") String cccd);

    /** Khoa dong dang ky khi lap phieu thu de 2 phieu cung luc khong vuot so con no. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DangKy d where d.id = :id")
    Optional<DangKy> khoaDeThuTien(@Param("id") Long id);

    long countByKhoaIdAndTrangThaiNot(Long khoaId, DangKy.TrangThai trangThai);

    long countByCtvIdAndTrangThaiNot(Long ctvId, DangKy.TrangThai trangThai);

    boolean existsByHocVienIdAndKhoaId(Long hocVienId, Long khoaId);

    /** Ho so co thuoc tai khoan hoc vien nay khong (QuyenHoSoService). */
    @Query("select count(d) > 0 from DangKy d where d.id = :id and d.hocVien.nguoiDung.id = :ndId")
    boolean laCuaHocVien(@Param("id") Long dangKyId, @Param("ndId") Long nguoiDungId);

    /** Ho so co nam trong khoa ma giao vien nay duoc phan cong day khong. */
    @Query("""
            select count(d) > 0 from DangKy d where d.id = :id
              and exists (select 1 from BuoiHoc b where b.khoa.id = d.khoa.id and b.giaoVien.nguoiDung.id = :ndId)
            """)
    boolean laHocVienCuaGiaoVien(@Param("id") Long dangKyId, @Param("ndId") Long nguoiDungId);

    @EntityGraph(attributePaths = {"hocVien"})
    List<DangKy> findByKhoaIdAndTrangThaiInOrderByHocVienHoTen(Long khoaId, Collection<DangKy.TrangThai> trangThai);

    @EntityGraph(attributePaths = {"khoa", "khoa.hang"})
    List<DangKy> findByHocVienIdOrderByNgayDangKyDesc(Long hocVienId);

    /** Khi khoa khai giang: DA_TIEP_NHAN -> DANG_HOC (bieu do trang thai). */
    @Modifying
    @Query("update DangKy d set d.trangThai = :moi where d.khoa.id = :khoaId and d.trangThai = :cu")
    int doiTrangThaiTheoKhoa(@Param("khoaId") Long khoaId, @Param("cu") DangKy.TrangThai cu,
                             @Param("moi") DangKy.TrangThai moi);

    @EntityGraph(attributePaths = {"hocVien", "khoa", "khoa.hang"})
    @Query("""
            select distinct d from DangKy d
            where exists (select 1 from BuoiHoc b where b.khoa.id = d.khoa.id and b.giaoVien.nguoiDung.id = :gvId)
              and d.trangThai <> :huy
            order by d.khoa.maKhoa desc, d.hocVien.hoTen asc
            """)
    List<DangKy> hocVienCuaGiaoVien(@Param("gvId") Long gvId, @Param("huy") DangKy.TrangThai huy);

    @EntityGraph(attributePaths = {"hocVien", "khoa", "khoa.hang"})
    @Query("select d from DangKy d where d.hocVien.nguoiDung.id = :ndId order by d.ngayDangKy desc")
    List<DangKy> findByHocVienNguoiDungId(@Param("ndId") Long ndId);
}
