package vn.vanxuan.dtms.module.ctv;

import jakarta.validation.constraints.*;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CtvDtos {
    private CtvDtos() {
    }

    static final String SDT = "^0[0-9]{9}$";
    static final String LOI_SDT = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0";

    // ------------------------------------------------------------------ ho so CTV
    /** Le tan / admin nhap ho so CTV. CCCD bat buoc de doi soat khi tra tien. */
    public record CtvRequest(@NotBlank @Size(max = 100) String hoTen,
                             @NotBlank @Pattern(regexp = SDT, message = LOI_SDT) String soDienThoai,
                             @Pattern(regexp = "^(0[0-9]{9})?$", message = LOI_SDT) String zalo,
                             @NotBlank @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải gồm 12 chữ số") String cccd,
                             @Size(max = 255) String diaChi,
                             @Size(max = 100) String diaBan,
                             @NotNull CongTacVien.Loai loai,
                             @Size(max = 100) String nganHang,
                             @Pattern(regexp = "^[0-9]{0,30}$", message = "Số tài khoản chỉ gồm chữ số") String soTaiKhoan,
                             @Size(max = 100) String chuTaiKhoan,
                             LocalDate ngayBatDau,
                             @Size(max = 255) String ghiChu) {
    }

    public record DuyetCtvRequest(@NotNull CongTacVien.Hang hang, LocalDate ngayBatDau) {
    }

    public record TuChoiRequest(@NotBlank @Size(max = 255) String lyDo) {
    }

    public record DoiTrangThaiCtvRequest(@NotNull CongTacVien.TrangThai trangThai, @Size(max = 255) String lyDo) {
    }

    public record DoiHangRequest(@NotNull CongTacVien.Hang hang) {
    }

    public record CtvResponse(Long id, String hoTen, String soDienThoai, String zalo, String cccd, String diaChi,
                              String diaBan, String loai, String hang, String trangThai, String nganHang,
                              String soTaiKhoan, String chuTaiKhoan, boolean coCamKet, LocalDate ngayBatDau,
                              String ghiChu, Long hocVienId, String tenDangNhap, Long nguoiTaoId, String nguoiTao,
                              LocalDateTime createdAt, String nguoiDuyet, LocalDateTime ngayDuyet,
                              long soLead, long soHocVien) {
    }

    /** Hoc vien xin lam CTV: thong tin ca nhan lay tu ho so hoc vien, chi nhap them tai khoan nhan tien. */
    public record XinLamCtvRequest(@Pattern(regexp = "^(0[0-9]{9})?$", message = LOI_SDT) String zalo,
                                   @Size(max = 100) String nganHang,
                                   @Pattern(regexp = "^[0-9]{0,30}$", message = "Số tài khoản chỉ gồm chữ số") String soTaiKhoan,
                                   @Size(max = 100) String chuTaiKhoan,
                                   @Size(max = 255) String ghiChu) {
    }

    // ------------------------------------------------------------------ lead
    public record LeadRequest(@NotBlank @Size(max = 100) String hoTen,
                              @NotBlank @Pattern(regexp = SDT, message = LOI_SDT) String soDienThoai,
                              @Size(max = 255) String diaChi,
                              String hangMuonHoc,
                              @Size(max = 255) String ghiChu,
                              LeadKhach.Nguon nguon,
                              Long ctvId,
                              Long hocVienGioiThieuId) {
    }

    public record DoiTrangThaiLeadRequest(@NotNull LeadKhach.TrangThai trangThai) {
    }

    public record DoiCtvLeadRequest(@NotNull Long ctvId) {
    }

    public record LeadResponse(Long id, String hoTen, String soDienThoai, String diaChi, String hangMuonHoc,
                               String ghiChu, String nguon, Long ctvId, String tenCtv, Long hocVienGioiThieuId,
                               String trangThai, Long dangKyId, String maHoSo, String nguoiNhap,
                               LocalDateTime createdAt) {
    }

    // ------------------------------------------------------------------ hoa hong
    public record HoaHongResponse(Long id, Long dangKyId, String maHoSo, String hocVien, String maKhoa, String hang,
                                  Long ctvId, String tenCtv, String kieu, BigDecimal giaTri, BigDecimal coSo,
                                  BigDecimal soTien, String trangThai, LocalDate ngayDuDieuKien, String ky,
                                  LocalDateTime ngayDuyet, String soPhieuChi, String ghiChu) {
    }

    public record DuyetHoaHongRequest(@NotEmpty List<Long> ids) {
    }

    public record ChiHoaHongRequest(@NotNull Long ctvId,
                                    @NotBlank @Pattern(regexp = "^[0-9]{4}-[0-9]{2}$", message = "Kỳ dạng yyyy-MM") String ky,
                                    @NotNull LocalDate ngayChi,
                                    @NotNull ChiHoaHong.HinhThuc hinhThuc,
                                    @Size(max = 255) String ghiChu) {
    }

    public record ChiHoaHongResponse(Long id, String soPhieu, Long ctvId, String tenCtv, String ky, BigDecimal soTien,
                                     String hinhThuc, LocalDate ngayChi, String nguoiChi, String ghiChu, int soDon) {
    }

    /** Mot dong tong hop ky chi: moi CTV mot dong. */
    public record TongHopKy(Long ctvId, String tenCtv, String soDienThoai, String nganHang, String soTaiKhoan,
                            String chuTaiKhoan, long soDon, BigDecimal duDieuKien, BigDecimal daDuyet,
                            BigDecimal daChi) {
    }

    public record ChinhSachRequest(@NotNull CongTacVien.Hang hangCtv,
                                   @NotBlank String hangGplx,
                                   @NotNull ChinhSachHoaHong.Kieu kieu,
                                   @NotNull @DecimalMin("0") BigDecimal giaTri,
                                   @NotNull LocalDate hieuLucTu) {
    }

    public record ChinhSachResponse(Long id, String hangCtv, String hangGplx, String kieu, BigDecimal giaTri,
                                    LocalDate hieuLucTu, boolean dangApDung, long soDonDaDung) {
    }

    public record CauHinhHoaHong(@NotNull @Min(1) @Max(100) Integer phanTramDongToiThieu,
                                 @NotNull @Min(1) @Max(28) Integer ngayChi) {
    }

    public record BaoCaoCtv(Long ctvId, String tenCtv, String hang, String trangThai, long soLead, long soChot,
                            double tyLeChot, long soHocVien, BigDecimal doanhThu, BigDecimal hoaHongPhatSinh,
                            BigDecimal daChi, BigDecimal conPhaiTra) {
    }

    /** CTV / hoc vien-CTV xem tong quan cua minh. */
    public record CtvCuaToi(Long ctvId, String hoTen, String trangThai, String hang, long soLead, long soChot,
                            BigDecimal choDuyet, BigDecimal choChi, BigDecimal daNhan) {
    }
}
