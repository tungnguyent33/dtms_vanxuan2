package vn.vanxuan.dtms.module.hocphi;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.hocvien.QuyenHoSoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.util.List;

import static vn.vanxuan.dtms.module.hocphi.HocPhiDtos.*;

/** Hoc vien chi xem / in duoc phieu thu va cong no cua chinh minh (QuyenHoSoService). */
@RestController
@RequestMapping("/api/phieu-thu")
public class PhieuThuController {
    private final HocPhiService service;
    private final PhieuThuPdfService pdfService;
    private final PhieuThuRepository phieuRepo;
    private final QuyenHoSoService quyen;

    public PhieuThuController(HocPhiService service, PhieuThuPdfService pdfService, PhieuThuRepository phieuRepo,
                              QuyenHoSoService quyen) {
        this.service = service;
        this.pdfService = pdfService;
        this.phieuRepo = phieuRepo;
        this.quyen = quyen;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public LapPhieuResponse lap(@Valid @RequestBody LapPhieuRequest req, @AuthenticationPrincipal AuthUser user) {
        return service.lapPhieu(req, user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN','HOC_VIEN')")
    public List<PhieuThuResponse> dsTheoDangKy(@RequestParam Long dangKyId, @AuthenticationPrincipal AuthUser user) {
        quyen.kiemTraXem(user, dangKyId);
        return service.dsPhieuCuaDangKy(dangKyId);
    }

    @GetMapping("/cong-no/{dangKyId}")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN','HOC_VIEN')")
    public CongNo congNo(@PathVariable Long dangKyId, @AuthenticationPrincipal AuthUser user) {
        quyen.kiemTraXem(user, dangKyId);
        return service.congNo(dangKyId);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN','HOC_VIEN')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        Long dangKyId = phieuRepo.dangKyIdCuaPhieu(id).orElseThrow(() -> new NotFoundException("phiếu thu", id));
        quyen.kiemTraXem(user, dangKyId);
        byte[] pdf = pdfService.taoPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("phieu-thu-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    /** BR-11: chi ADMIN duoc huy, bat buoc ly do. */
    @PatchMapping("/{id}/huy")
    @PreAuthorize("hasRole('ADMIN')")
    public PhieuThuResponse huy(@PathVariable Long id, @Valid @RequestBody HuyPhieuRequest req,
                                @AuthenticationPrincipal AuthUser user) {
        return service.huyPhieu(id, req.lyDo(), user);
    }
}
