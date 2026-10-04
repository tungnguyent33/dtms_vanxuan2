package vn.vanxuan.dtms.module.ctv;

import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.nguoidung.TaiKhoanService.KetQuaCapMatKhau;
import vn.vanxuan.dtms.security.AuthUser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/**
 * Ho so CTV. Ma tran quyen:
 * LE_TAN: xem, tao (cho duyet), sua khi cho duyet, tai cam ket khi cho duyet.
 * ADMIN: tat ca + duyet / tu choi, khoa / mo, doi hang, cap tai khoan. Khong ai xoa CTV.
 */
@RestController
@RequestMapping("/api/ctv")
@PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
public class CtvController {

    private final CtvService service;

    public CtvController(CtvService service) {
        this.service = service;
    }

    /** Mac dinh tra CTV dang hoat dong (dung cho o chon CTV khi tiep nhan ho so). */
    @GetMapping
    public List<CtvResponse> ds(@RequestParam(required = false) CongTacVien.TrangThai trangThai,
                                @RequestParam(defaultValue = "false") boolean tatCa,
                                @RequestParam(required = false) String q,
                                @AuthenticationPrincipal AuthUser user) {
        return service.ds(tatCa ? trangThai : (trangThai == null ? CongTacVien.TrangThai.HOAT_DONG : trangThai), q, user);
    }

    @GetMapping("/{id}")
    public CtvResponse chiTiet(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.chiTiet(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CtvResponse tao(@Valid @RequestBody CtvRequest req, @AuthenticationPrincipal AuthUser user) {
        return service.tao(req, user);
    }

    @PutMapping("/{id}")
    public CtvResponse sua(@PathVariable Long id, @Valid @RequestBody CtvRequest req, @AuthenticationPrincipal AuthUser user) {
        return service.sua(id, req, user);
    }

    @PostMapping(value = "/{id}/cam-ket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CtvResponse taiCamKet(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                 @AuthenticationPrincipal AuthUser user) throws IOException {
        return service.luuCamKet(id, file, user);
    }

    @GetMapping("/{id}/cam-ket")
    public ResponseEntity<Resource> xemCamKet(@PathVariable Long id) {
        Path p = service.duongDanCamKet(id);
        String ten = p.toString().toLowerCase();
        MediaType type = ten.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : ten.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(p));
    }

    // ------------------------------------------------------------------ chi ADMIN
    @PostMapping("/{id}/duyet")
    @PreAuthorize("hasRole('ADMIN')")
    public CtvResponse duyet(@PathVariable Long id, @Valid @RequestBody DuyetCtvRequest req,
                             @AuthenticationPrincipal AuthUser admin) {
        return service.duyet(id, req, admin);
    }

    @PostMapping("/{id}/tu-choi")
    @PreAuthorize("hasRole('ADMIN')")
    public CtvResponse tuChoi(@PathVariable Long id, @Valid @RequestBody TuChoiRequest req,
                              @AuthenticationPrincipal AuthUser admin) {
        return service.tuChoi(id, req.lyDo(), admin);
    }

    @PatchMapping("/{id}/trang-thai")
    @PreAuthorize("hasRole('ADMIN')")
    public CtvResponse doiTrangThai(@PathVariable Long id, @Valid @RequestBody DoiTrangThaiCtvRequest req,
                                    @AuthenticationPrincipal AuthUser admin) {
        return service.doiTrangThai(id, req.trangThai(), req.lyDo(), admin);
    }

    @PatchMapping("/{id}/hang")
    @PreAuthorize("hasRole('ADMIN')")
    public CtvResponse doiHang(@PathVariable Long id, @Valid @RequestBody DoiHangRequest req,
                               @AuthenticationPrincipal AuthUser admin) {
        return service.doiHang(id, req.hang(), admin);
    }

    @PostMapping("/{id}/tai-khoan")
    @PreAuthorize("hasRole('ADMIN')")
    public KetQuaCapMatKhau capTaiKhoan(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return service.capTaiKhoan(id, admin);
    }
}
