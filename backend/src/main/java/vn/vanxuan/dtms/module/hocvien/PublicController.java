package vn.vanxuan.dtms.module.hocvien;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTaoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * API cong khai (khong can dang nhap) cho trang dang ky truc tuyen.
 * Khi trien khai that nen them captcha va gioi han tan suat (rate limit) o nginx.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    public record KhoaCongKhai(Long id, String maKhoa, String hang, String tenHang, LocalDate ngayKhaiGiang,
                               LocalDate ngayBeGiang, BigDecimal hocPhi, long conCho) {
    }

    public record KetQuaDangKy(String maHoSo, String thongBao) {
    }

    private final KhoaDaoTaoRepository khoaRepo;
    private final DangKyRepository dangKyRepo;
    private final DangKyService dangKyService;

    public PublicController(KhoaDaoTaoRepository khoaRepo, DangKyRepository dangKyRepo, DangKyService dangKyService) {
        this.khoaRepo = khoaRepo;
        this.dangKyRepo = dangKyRepo;
        this.dangKyService = dangKyService;
    }

    @GetMapping("/khoa-dang-tuyen")
    @Transactional(readOnly = true)
    public List<KhoaCongKhai> khoaDangTuyen() {
        return khoaRepo.timKiem(KhoaDaoTao.TrangThai.DANG_TUYEN).stream().map(k -> {
            long da = dangKyRepo.countByKhoaIdAndTrangThaiNot(k.getId(), DangKy.TrangThai.DA_HUY);
            return new KhoaCongKhai(k.getId(), k.getMaKhoa(), k.getHang().getMa(), k.getHang().getTen(),
                    k.getNgayKhaiGiang(), k.getNgayBeGiang(), k.getHocPhi(), Math.max(0, k.getSiSoToiDa() - da));
        }).toList();
    }

    @PostMapping("/dang-ky")
    @ResponseStatus(HttpStatus.CREATED)
    public KetQuaDangKy dangKy(@Valid @RequestBody DangKyDtos.DangKyTrucTuyenRequest req) {
        String ma = dangKyService.dangKyTrucTuyen(req);
        return new KetQuaDangKy(ma, "Đăng ký thành công. Trung tâm sẽ gọi lại để xác nhận hồ sơ.");
    }
}
