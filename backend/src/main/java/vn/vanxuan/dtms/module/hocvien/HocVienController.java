package vn.vanxuan.dtms.module.hocvien;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService.KetQuaCapMatKhau;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static vn.vanxuan.dtms.module.hocvien.DangKyDtos.DangKyResponse;
import static vn.vanxuan.dtms.module.hocvien.DangKyDtos.HocVienResponse;

@RestController
@RequestMapping("/api/hoc-vien")
@PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
public class HocVienController {
    private final HocVienRepository hocVienRepo;
    private final DangKyRepository dangKyRepo;
    private final AnhHocVienService anhService;
    private final TaiKhoanService taiKhoan;

    public HocVienController(HocVienRepository hocVienRepo, DangKyRepository dangKyRepo, AnhHocVienService anhService,
                             TaiKhoanService taiKhoan) {
        this.hocVienRepo = hocVienRepo;
        this.dangKyRepo = dangKyRepo;
        this.anhService = anhService;
        this.taiKhoan = taiKhoan;
    }

    /**
     * Cap tai khoan cong hoc vien (ten dang nhap = CCCD) hoac dat lai mat khau neu da co.
     * Mat khau tam tra ve DUY NHAT mot lan de le tan dua cho hoc vien; hoc vien phai doi o lan dang nhap dau.
     */
    @PostMapping("/{id}/cap-tai-khoan")
    @Transactional
    public KetQuaCapMatKhau capTaiKhoan(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        HocVien hv = hocVienRepo.findById(id).orElseThrow(() -> new NotFoundException("học viên", id));
        if (hv.getNguoiDung() != null) {
            return taiKhoan.datLaiMatKhau(hv.getNguoiDung(), user.id());
        }
        KetQuaCapMatKhau kq = taiKhoan.tao(hv.getCccd(), hv.getHoTen(), hv.getSoDienThoai(), hv.getEmail(),
                VaiTro.HOC_VIEN, user.id());
        hv.setNguoiDung(taiKhoan.layThamChieu(kq.nguoiDungId()));
        return kq;
    }

    /** Buoc 1-2 cua UC05: tra CCCD de dung lai ho so cu. 404 neu chua co. */
    @GetMapping("/tra-cccd/{cccd}")
    public HocVienResponse traCccd(@PathVariable String cccd) {
        return hocVienRepo.findByCccd(cccd).map(HocVienResponse::of)
                .orElseThrow(() -> new NotFoundException("học viên có CCCD", cccd));
    }

    @GetMapping("/{id}")
    public HocVienResponse chiTiet(@PathVariable Long id) {
        return hocVienRepo.findById(id).map(HocVienResponse::of)
                .orElseThrow(() -> new NotFoundException("học viên", id));
    }

    /** Lich su cac khoa hoc vien da dang ky (A1 roi A, hoc lai...). */
    @GetMapping("/{id}/dang-ky")
    @Transactional(readOnly = true)
    public List<DangKyResponse> lichSu(@PathVariable Long id) {
        return dangKyRepo.findByHocVienIdOrderByNgayDangKyDesc(id).stream().map(DangKyResponse::of).toList();
    }

    @PostMapping(value = "/{id}/anh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void taiAnh(@PathVariable Long id, @RequestParam AnhHocVienService.LoaiAnh loai,
                       @RequestParam("file") MultipartFile file) throws IOException {
        anhService.luu(id, loai, file);
    }

    @GetMapping("/{id}/anh/{loai}")
    public ResponseEntity<Resource> xemAnh(@PathVariable Long id, @PathVariable AnhHocVienService.LoaiAnh loai) {
        Path p = anhService.duongDan(id, loai);
        MediaType type = p.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(p));
    }
}
