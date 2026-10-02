package vn.vanxuan.dtms.module.hocphi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class HocPhiDtos {
    private HocPhiDtos() {
    }

    public record LapPhieuRequest(@NotNull Long dangKyId,
                                  @NotNull @DecimalMin(value = "1000", message = "Số tiền tối thiểu 1.000đ") BigDecimal soTien,
                                  @NotNull PhieuThu.HinhThuc hinhThuc,
                                  @Size(max = 255) String noiDung) {
    }

    public record HuyPhieuRequest(@NotBlank @Size(max = 255) String lyDo) {
    }

    public record CongNo(BigDecimal phaiDong, BigDecimal daDong, BigDecimal conNo) {
    }

    public record PhieuThuResponse(Long id, String soPhieu, Long dangKyId, BigDecimal soTien, String hinhThuc,
                                   String noiDung, LocalDateTime ngayThu, String nguoiThu, String trangThai,
                                   String lyDoHuy) {
        public static PhieuThuResponse of(PhieuThu p) {
            return new PhieuThuResponse(p.getId(), p.getSoPhieu(), p.getDangKy().getId(), p.getSoTien(),
                    p.getHinhThuc().name(), p.getNoiDung(), p.getNgayThu(), p.getNguoiThu().getHoTen(),
                    p.getTrangThai().name(), p.getLyDoHuy());
        }
    }

    public record LapPhieuResponse(PhieuThuResponse phieu, CongNo congNo) {
    }
}
