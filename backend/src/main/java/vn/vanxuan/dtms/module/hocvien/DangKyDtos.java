package vn.vanxuan.dtms.module.hocvien;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DangKyDtos {
    private DangKyDtos() {
    }

    public record HocVienInput(
            @NotBlank @Size(max = 100) String hoTen,
            @NotNull @Past LocalDate ngaySinh,
            @NotNull HocVien.GioiTinh gioiTinh,
            @NotBlank @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải gồm 12 chữ số") String cccd,
            LocalDate ngayCapCccd,
            @NotBlank @Size(max = 255) String diaChi,
            @NotBlank @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
            String soDienThoai,
            @Email String email) {
    }

    /** Le tan tiep nhan ho so (UC05). */
    public record TaoDangKyRequest(
            @NotNull @Valid HocVienInput hocVien,
            @NotNull Long khoaId,
            @NotNull DangKy.HinhThucLyThuyet hinhThucLyThuyet,
            @NotNull DangKy.Nguon nguon,
            Long ctvId,
            @DecimalMin("0") BigDecimal giamTru,
            String lyDoGiamTru,
            String ghiChu,
            Long leadId) {
    }

    /** Gan / doi CTV cho ho so da co (le tan gan khi chua co CTV; doi CTV da gan chi admin). */
    public record GanCtvRequest(@NotNull Long ctvId) {
    }

    /** Khach dang ky truc tuyen (khong co giam tru, nguon luon TRUC_TUYEN). */
    public record DangKyTrucTuyenRequest(
            @NotNull @Valid HocVienInput hocVien,
            @NotNull Long khoaId,
            @NotNull DangKy.HinhThucLyThuyet hinhThucLyThuyet,
            String captchaId,
            String captcha) {
    }

    public record HuyRequest(@NotBlank String lyDo) {
    }

    public record ChuyenKhoaRequest(@NotNull Long khoaMoiId) {
    }

    public record HocVienResponse(Long id, String maHocVien, String hoTen, LocalDate ngaySinh, String gioiTinh,
                                  String cccd, LocalDate ngayCapCccd, String diaChi, String soDienThoai, String email,
                                  boolean coAnhChanDung, boolean coAnhCccd, boolean coTaiKhoan) {
        public static HocVienResponse of(HocVien h) {
            // getNguoiDung() la proxy LAZY: so sanh null khong can nap tu CSDL
            return new HocVienResponse(h.getId(), h.getMaHocVien(), h.getHoTen(), h.getNgaySinh(),
                    h.getGioiTinh().name(), h.getCccd(), h.getNgayCapCccd(), h.getDiaChi(), h.getSoDienThoai(),
                    h.getEmail(), h.getAnhChanDungUrl() != null,
                    h.getAnhCccdTruocUrl() != null && h.getAnhCccdSauUrl() != null, h.getNguoiDung() != null);
        }
    }

    public record DangKyResponse(Long id, String maHoSo, HocVienResponse hocVien, Long khoaId, String maKhoa,
                                 String hang, String hinhThucLyThuyet, String nguon, Long ctvId, BigDecimal hocPhi,
                                 BigDecimal giamTru, String trangThai, LocalDateTime ngayDangKy,
                                 String soGiayXacNhan, LocalDate ngayHoanThanh, String ghiChu, String tenCtv,
                                 Long gioiThieuHocVienId) {
        public static DangKyResponse of(DangKy d) {
            return new DangKyResponse(d.getId(), d.getMaHoSo(), HocVienResponse.of(d.getHocVien()),
                    d.getKhoa().getId(), d.getKhoa().getMaKhoa(), d.getKhoa().getHang().getMa(),
                    d.getHinhThucLyThuyet().name(), d.getNguon().name(),
                    d.getCtv() == null ? null : d.getCtv().getId(), d.getHocPhi(), d.getGiamTru(),
                    d.getTrangThai().name(), d.getNgayDangKy(), d.getSoGiayXacNhan(), d.getNgayHoanThanh(),
                    d.getGhiChu(), d.getCtv() == null ? null : d.getCtv().getHoTen(), d.getGioiThieuHocVienId());
        }
    }
}
