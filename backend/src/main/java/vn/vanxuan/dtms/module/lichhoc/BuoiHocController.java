package vn.vanxuan.dtms.module.lichhoc;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.security.AuthUser;

import java.time.LocalDate;
import java.util.List;

import static vn.vanxuan.dtms.module.lichhoc.LichHocDtos.*;

@RestController
@RequestMapping("/api/buoi-hoc")
public class BuoiHocController {
    private final LichHocService service;

    public BuoiHocController(LichHocService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public BuoiHocResponse tao(@Valid @RequestBody TaoBuoiRequest req) {
        return service.taoBuoi(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public List<BuoiHocResponse> timKiem(@RequestParam(required = false) Long khoaId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den) {
        return service.timKiem(khoaId, tu, den);
    }

    /** Lich day cua giao vien / lich hoc cua hoc vien dang dang nhap. */
    @GetMapping("/cua-toi")
    @PreAuthorize("hasAnyRole('GIAO_VIEN', 'HOC_VIEN')")
    public List<BuoiHocResponse> cuaToi(@AuthenticationPrincipal AuthUser user,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        if (ngay != null) {
            tu = ngay;
            den = ngay;
        } else {
            if (tu == null) tu = LocalDate.now();
            if (den == null) den = LocalDate.now().plusDays(7);
        }
        return service.lichCuaToi(user, tu, den);
    }

    @GetMapping("/{id}/danh-sach")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN','GIAO_VIEN')")
    public DanhSachBuoi danhSach(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.danhSach(id, user);
    }

    /** Le tan khong diem danh (ma tran phan quyen) - chi giao vien phu trach hoac ADMIN. */
    @PutMapping("/{id}/diem-danh")
    @PreAuthorize("hasAnyRole('ADMIN','GIAO_VIEN')")
    public DanhSachBuoi diemDanh(@PathVariable Long id, @Valid @RequestBody LuuDiemDanhRequest req,
                                 @AuthenticationPrincipal AuthUser user) {
        return service.luuDiemDanh(id, req, user);
    }
}
