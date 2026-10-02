package vn.vanxuan.dtms.module.hocvien;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.PageResponse;
import vn.vanxuan.dtms.module.lichhoc.TienDoService;
import vn.vanxuan.dtms.security.AuthUser;

import java.util.List;

import static vn.vanxuan.dtms.module.hocvien.DangKyDtos.*;

@RestController
@RequestMapping("/api/dang-ky")
public class DangKyController {
    private final DangKyService service;
    private final TienDoService tienDoService;

    public DangKyController(DangKyService service, TienDoService tienDoService) {
        this.service = service;
        this.tienDoService = tienDoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public PageResponse<DangKyResponse> timKiem(@RequestParam(required = false) Long khoaId,
                                                @RequestParam(required = false) DangKy.TrangThai trangThai,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.timKiem(khoaId, trangThai, q, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public DangKyResponse chiTiet(@PathVariable Long id) {
        return service.chiTiet(id);
    }

    @GetMapping("/cua-toi")
    @PreAuthorize("hasAnyRole('GIAO_VIEN', 'HOC_VIEN')")
    public List<DangKyResponse> hoSoCuaToi(@AuthenticationPrincipal AuthUser user) {
        return service.hoSoCuaToi(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public DangKyResponse tao(@Valid @RequestBody TaoDangKyRequest req, @AuthenticationPrincipal AuthUser user) {
        return service.taoDangKy(req, user);
    }

    @PatchMapping("/{id}/duyet")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public DangKyResponse duyet(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.duyet(id, user);
    }

    @PatchMapping("/{id}/huy")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public DangKyResponse huy(@PathVariable Long id, @Valid @RequestBody HuyRequest req,
                              @AuthenticationPrincipal AuthUser user) {
        return service.huy(id, req.lyDo(), user);
    }

    @PatchMapping("/{id}/chuyen-khoa")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public DangKyResponse chuyenKhoa(@PathVariable Long id, @Valid @RequestBody ChuyenKhoaRequest req,
                                     @AuthenticationPrincipal AuthUser user) {
        return service.chuyenKhoa(id, req.khoaMoiId(), user);
    }

    @GetMapping("/{id}/tien-do")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN', 'HOC_VIEN')")
    public TienDoService.TienDo tienDo(@PathVariable Long id) {
        return tienDoService.tinh(id);
    }
}
