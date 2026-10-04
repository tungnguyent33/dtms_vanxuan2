package vn.vanxuan.dtms.module.hocvien;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.module.danhmuc.HangGplx;
import vn.vanxuan.dtms.module.danhmuc.HangGplxRepository;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTaoRepository;
import vn.vanxuan.dtms.security.CaptchaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * API cong khai (khong can dang nhap) cho tac nhan Khach: xem khoa dang tuyen, thong tin hang,
 * dang ky truc tuyen va tra cuu tinh trang ho so.
 * Khi trien khai that nen them captcha va gioi han tan suat (rate limit) o nginx.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    public record KhoaCongKhai(Long id, String maKhoa, String hang, String tenHang, LocalDate ngayKhaiGiang,
                               LocalDate ngayBeGiang, BigDecimal hocPhi, long conCho) {
    }

    public record HangCongKhai(String ma, String ten, BigDecimal gioLyThuyet, BigDecimal gioThucHanh,
                               Integer tuoiToiThieu, Integer soNgayKhoaToiDa, String canCuPhapLy) {
        static HangCongKhai of(HangGplx h) {
            return new HangCongKhai(h.getMa(), h.getTen(), h.getGioLyThuyet(), h.getGioThucHanh(),
                    h.getTuoiToiThieu(), h.getSoNgayKhoaToiDa(), h.getCanCuPhapLy());
        }
    }

    public record KetQuaDangKy(String maHoSo, String thongBao) {
    }

    public record TraCuuRequest(@NotBlank String maHoSo,
                                @NotBlank @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải gồm 12 chữ số") String cccd,
                                String captchaId,
                                String captcha) {
    }

    /** Chi tra thong tin du de khach biet tinh trang, khong lo CCCD, SDT, dia chi, hoc phi. */
    public record KetQuaTraCuu(String maHoSo, String hoTen, String hang, String maKhoa, LocalDate ngayKhaiGiang,
                               LocalDate ngayBeGiang, String trangThai, LocalDateTime ngayDangKy) {
    }

    private final KhoaDaoTaoRepository khoaRepo;
    private final DangKyRepository dangKyRepo;
    private final HangGplxRepository hangRepo;
    private final DangKyService dangKyService;
    private final CaptchaService captcha;

    public PublicController(KhoaDaoTaoRepository khoaRepo, DangKyRepository dangKyRepo, HangGplxRepository hangRepo,
                            DangKyService dangKyService, CaptchaService captcha) {
        this.khoaRepo = khoaRepo;
        this.dangKyRepo = dangKyRepo;
        this.hangRepo = hangRepo;
        this.dangKyService = dangKyService;
        this.captcha = captcha;
    }

    /** Captcha anh cho form dang ky / tra cuu (dung mot lan, het han sau 5 phut). batBuoc=false khi tat o cau hinh. */
    public record CaptchaResponse(String captchaId, String anh, boolean batBuoc) {
    }

    @GetMapping("/captcha")
    public CaptchaResponse captcha() {
        if (!captcha.dangBat()) return new CaptchaResponse(null, null, false);
        var c = captcha.tao();
        return new CaptchaResponse(c.captchaId(), c.anh(), true);
    }

    @GetMapping("/khoa-dang-tuyen")
    @Transactional(readOnly = true)
    public List<KhoaCongKhai> khoaDangTuyen() {
        return khoaRepo.timKiem(KhoaDaoTao.TrangThai.DANG_TUYEN).stream()
                .sorted(Comparator.comparing(KhoaDaoTao::getNgayKhaiGiang))
                .map(k -> {
                    long da = dangKyRepo.countByKhoaIdAndTrangThaiNot(k.getId(), DangKy.TrangThai.DA_HUY);
                    return new KhoaCongKhai(k.getId(), k.getMaKhoa(), k.getHang().getMa(), k.getHang().getTen(),
                            k.getNgayKhaiGiang(), k.getNgayBeGiang(), k.getHocPhi(), Math.max(0, k.getSiSoToiDa() - da));
                }).toList();
    }

    @GetMapping("/hang-gplx")
    public List<HangCongKhai> hangDaoTao() {
        return hangRepo.findAll().stream()
                .filter(h -> Boolean.TRUE.equals(h.getDangApDung()))
                .map(HangCongKhai::of).toList();
    }

    @PostMapping("/dang-ky")
    @ResponseStatus(HttpStatus.CREATED)
    public KetQuaDangKy dangKy(@Valid @RequestBody DangKyDtos.DangKyTrucTuyenRequest req) {
        captcha.kiemTra(req.captchaId(), req.captcha());
        String ma = dangKyService.dangKyTrucTuyen(req);
        return new KetQuaDangKy(ma, "Đăng ký thành công. Trung tâm sẽ gọi lại để xác nhận hồ sơ.");
    }

    /** Dung POST de CCCD khong nam tren URL (log truy cap, lich su trinh duyet). */
    @PostMapping("/tra-cuu")
    @Transactional(readOnly = true)
    public KetQuaTraCuu traCuu(@Valid @RequestBody TraCuuRequest req) {
        captcha.kiemTra(req.captchaId(), req.captcha());
        // Cung mot thong bao cho "sai ma" va "sai CCCD" de khong do duoc ma ho so nao ton tai
        DangKy d = dangKyRepo.traCuu(req.maHoSo().trim().toUpperCase(), req.cccd())
                .orElseThrow(() -> new BusinessException("KHONG_TIM_THAY_HO_SO",
                        "Không tìm thấy hồ sơ khớp mã hồ sơ và số CCCD", HttpStatus.NOT_FOUND));
        KhoaDaoTao k = d.getKhoa();
        return new KetQuaTraCuu(d.getMaHoSo(), d.getHocVien().getHoTen(), k.getHang().getMa(), k.getMaKhoa(),
                k.getNgayKhaiGiang(), k.getNgayBeGiang(), d.getTrangThai().name(), d.getNgayDangKy());
    }
}
