package vn.vanxuan.dtms.module.khoa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class KhoaDtos {
    private KhoaDtos() {
    }

    public record TaoKhoaRequest(
            @NotBlank String maKhoa,
            @NotBlank String hangMa,
            @NotNull LocalDate ngayKhaiGiang,
            @NotNull LocalDate ngayBeGiang,
            @NotNull @Min(1) Integer siSoToiDa,
            @DecimalMin("0") BigDecimal hocPhi,     // de trong -> lay hoc phi mac dinh cua hang
            String ghiChu) {
    }

    public record SuaKhoaRequest(
            @NotBlank String maKhoa,
            @NotBlank String hangMa,
            @NotNull LocalDate ngayKhaiGiang,
            @NotNull LocalDate ngayBeGiang,
            @NotNull @Min(1) Integer siSoToiDa,
            @DecimalMin("0") BigDecimal hocPhi,
            String ghiChu) {
    }

    public record DoiTrangThaiRequest(@NotNull KhoaDaoTao.TrangThai trangThai) {
    }

    public record KhoaResponse(Long id, String maKhoa, String hangMa, LocalDate ngayKhaiGiang, LocalDate ngayBeGiang,
                               long soNgay, Integer siSoToiDa, long soDangKy, BigDecimal hocPhi, String trangThai,
                               LocalDate ngayBaoCaoSo, String ghiChu) {
        public static KhoaResponse of(KhoaDaoTao k, long soDangKy) {
            return new KhoaResponse(k.getId(), k.getMaKhoa(), k.getHang().getMa(), k.getNgayKhaiGiang(),
                    k.getNgayBeGiang(), k.soNgay(), k.getSiSoToiDa(), soDangKy, k.getHocPhi(),
                    k.getTrangThai().name(), k.getNgayBaoCaoSo(), k.getGhiChu());
        }
    }
}
