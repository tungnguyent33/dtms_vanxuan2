package vn.vanxuan.dtms.module.ctv;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.PageResponse;
import vn.vanxuan.dtms.security.AuthUser;

import java.util.Map;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/** Lead do van phong quan ly (le tan nhap thay CTV / khach tu den). Giao lai lead cho CTV khac: chi ADMIN. */
@RestController
@RequestMapping("/api/lead")
@PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
public class LeadController {

    private final LeadService service;

    public LeadController(LeadService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<LeadResponse> ds(@RequestParam(required = false) Long ctvId,
                                        @RequestParam(required = false) LeadKhach.TrangThai trangThai,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.timKiem(ctvId, trangThai, q, page, size);
    }

    /** Kiem tra truoc khi nhap: SDT da la lead / hoc vien chua. */
    @GetMapping("/kiem-tra-trung")
    public Map<String, Object> kiemTraTrung(@RequestParam String sdt) {
        return service.kiemTraTrung(sdt.trim()).<Map<String, Object>>map(m -> Map.of("trung", true, "moTa", m))
                .orElse(Map.of("trung", false));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse tao(@Valid @RequestBody LeadRequest req, @AuthenticationPrincipal AuthUser user) {
        return service.taoBoiNhanVien(req, user);
    }

    @PatchMapping("/{id}/trang-thai")
    public LeadResponse doiTrangThai(@PathVariable Long id, @Valid @RequestBody DoiTrangThaiLeadRequest req,
                                     @AuthenticationPrincipal AuthUser user) {
        return service.doiTrangThai(id, req.trangThai(), user);
    }

    @PatchMapping("/{id}/ctv")
    @PreAuthorize("hasRole('ADMIN')")
    public LeadResponse doiCtv(@PathVariable Long id, @Valid @RequestBody DoiCtvLeadRequest req,
                               @AuthenticationPrincipal AuthUser admin) {
        return service.doiCtv(id, req.ctvId(), admin);
    }
}
