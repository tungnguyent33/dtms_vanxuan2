package vn.vanxuan.dtms.module.lichhoc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class LichHocDtos {
    private LichHocDtos() {
    }

    public record TaoBuoiRequest(@NotNull Long khoaId,
                                 @NotNull BuoiHoc.Loai loai,
                                 @NotNull LocalDate ngay,
                                 @NotNull LocalTime gioBatDau,
                                 @NotNull LocalTime gioKetThuc,
                                 @NotBlank String diaDiem,
                                 @NotNull Long giaoVienId,
                                 Long xeId) {
    }

    public record BuoiHocResponse(Long id, Long khoaId, String maKhoa, String loai, LocalDate ngay,
                                  LocalTime gioBatDau, LocalTime gioKetThuc, BigDecimal thoiLuongGio,
                                  String diaDiem, Long giaoVienId, String giaoVien, Long xeId, String bienSoXe,
                                  String trangThai) {
        public static BuoiHocResponse of(BuoiHoc b) {
            return new BuoiHocResponse(b.getId(), b.getKhoa().getId(), b.getKhoa().getMaKhoa(), b.getLoai().name(),
                    b.getNgay(), b.getGioBatDau(), b.getGioKetThuc(), b.thoiLuongGio(), b.getDiaDiem(),
                    b.getGiaoVien().getId(), b.getGiaoVien().getHoTen(),
                    b.getXe() == null ? null : b.getXe().getId(),
                    b.getXe() == null ? null : b.getXe().getBienSo(), b.getTrangThai().name());
        }
    }

    /** Mot dong trong danh sach diem danh cua buoi (da diem danh hoac chua). */
    public record DongDiemDanh(Long dangKyId, String maHoSo, String hoTen, String soDienThoai,
                               String hinhThucLyThuyet, Boolean coMat, BigDecimal soGio, String ghiChu) {
    }

    public record DanhSachBuoi(BuoiHocResponse buoi, List<DongDiemDanh> hocVien) {
    }

    public record DiemDanhItem(@NotNull Long dangKyId, @NotNull Boolean coMat,
                               @NotNull @DecimalMin("0") BigDecimal soGio, String ghiChu) {
    }

    public record LuuDiemDanhRequest(@NotEmpty List<@Valid DiemDanhItem> danhSach) {
    }
}
