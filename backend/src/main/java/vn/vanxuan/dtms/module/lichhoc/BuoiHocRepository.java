package vn.vanxuan.dtms.module.lichhoc;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BuoiHocRepository extends JpaRepository<BuoiHoc, Long> {

    @EntityGraph(attributePaths = {"khoa", "giaoVien", "xe"})
    @Query("""
            select b from BuoiHoc b
            where (:khoaId is null or b.khoa.id = :khoaId)
              and (:tu is null or b.ngay >= :tu)
              and (:den is null or b.ngay <= :den)
            order by b.ngay, b.gioBatDau
            """)
    List<BuoiHoc> timKiem(@Param("khoaId") Long khoaId, @Param("tu") LocalDate tu, @Param("den") LocalDate den);

    @EntityGraph(attributePaths = {"khoa", "khoa.hang", "giaoVien", "giaoVien.nguoiDung", "xe"})
    @Query("select b from BuoiHoc b where b.id = :id")
    Optional<BuoiHoc> findChiTiet(@Param("id") Long id);

    @EntityGraph(attributePaths = {"khoa", "xe"})
    @Query("""
            select b from BuoiHoc b
            where b.giaoVien.nguoiDung.id = :nguoiDungId 
              and (:tu is null or b.ngay >= :tu) 
              and (:den is null or b.ngay <= :den) 
              and b.trangThai <> :huy
            order by b.ngay, b.gioBatDau
            """)
    List<BuoiHoc> lichCuaGiaoVien(@Param("nguoiDungId") Long nguoiDungId, @Param("tu") LocalDate tu, 
                                  @Param("den") LocalDate den, @Param("huy") BuoiHoc.TrangThai huy);

    /** BR-06: dem buoi trung khung gio cua giao vien (hai khoang [a,b) va [c,d) giao nhau khi a<d va c<b). */
    @Query("""
            select count(b) from BuoiHoc b
            where b.giaoVien.id = :gvId and b.ngay = :ngay and b.trangThai <> :huy
              and b.gioBatDau < :ketThuc and b.gioKetThuc > :batDau
              and (:boQuaId is null or b.id <> :boQuaId)
            """)
    long demTrungLichGiaoVien(@Param("gvId") Long gvId, @Param("ngay") LocalDate ngay,
                              @Param("batDau") LocalTime batDau, @Param("ketThuc") LocalTime ketThuc,
                              @Param("boQuaId") Long boQuaId, @Param("huy") BuoiHoc.TrangThai huy);

    @Query("""
            select count(b) from BuoiHoc b
            where b.xe.id = :xeId and b.ngay = :ngay and b.trangThai <> :huy
              and b.gioBatDau < :ketThuc and b.gioKetThuc > :batDau
              and (:boQuaId is null or b.id <> :boQuaId)
            """)
    long demTrungLichXe(@Param("xeId") Long xeId, @Param("ngay") LocalDate ngay,
                        @Param("batDau") LocalTime batDau, @Param("ketThuc") LocalTime ketThuc,
                        @Param("boQuaId") Long boQuaId, @Param("huy") BuoiHoc.TrangThai huy);

    @EntityGraph(attributePaths = {"khoa", "xe", "giaoVien"})
    @Query("""
            select distinct b from BuoiHoc b
            join DangKy d on d.khoa.id = b.khoa.id
            where d.hocVien.nguoiDung.id = :nguoiDungId
              and d.trangThai <> :huyDk
              and (:tu is null or b.ngay >= :tu) 
              and (:den is null or b.ngay <= :den) 
              and b.trangThai <> :huyBuoi
              and (b.loai = 'THUC_HANH' or d.hinhThucLyThuyet = 'TAP_TRUNG')
            order by b.ngay, b.gioBatDau
            """)
    List<BuoiHoc> lichCuaHocVien(@Param("nguoiDungId") Long nguoiDungId, @Param("tu") LocalDate tu, 
                                 @Param("den") LocalDate den, @Param("huyBuoi") BuoiHoc.TrangThai huyBuoi,
                                 @Param("huyDk") vn.vanxuan.dtms.module.hocvien.DangKy.TrangThai huyDk);
}
