package vn.vanxuan.dtms.module.khoa;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.security.AuthUser;

import java.util.List;

import static vn.vanxuan.dtms.module.khoa.KhoaDtos.*;

@RestController
@RequestMapping("/api/khoa")
public class KhoaController {
    private final KhoaService khoaService;
    private final XetHoanThanhService xetService;

    public KhoaController(KhoaService khoaService, XetHoanThanhService xetService) {
        this.khoaService = khoaService;
        this.xetService = xetService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public List<KhoaResponse> ds(@RequestParam(required = false) KhoaDaoTao.TrangThai trangThai) {
        return khoaService.ds(trangThai);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
    public KhoaResponse chiTiet(@PathVariable Long id) {
        return khoaService.chiTiet(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public KhoaResponse tao(@Valid @RequestBody TaoKhoaRequest req) {
        return khoaService.tao(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public KhoaResponse sua(@PathVariable Long id, @Valid @RequestBody SuaKhoaRequest req) {
        return khoaService.sua(id, req);
    }

    @PatchMapping("/{id}/trang-thai")
    @PreAuthorize("hasRole('ADMIN')")
    public KhoaResponse doiTrangThai(@PathVariable Long id, @Valid @RequestBody DoiTrangThaiRequest req,
                                     @AuthenticationPrincipal AuthUser user) {
        return khoaService.doiTrangThai(id, req.trangThai(), user);
    }

    /** Xem truoc ket qua xet hoan thanh (khong ghi CSDL). */
    @GetMapping("/{id}/xet-hoan-thanh")
    @PreAuthorize("hasRole('ADMIN')")
    public List<XetHoanThanhService.KetQuaXet> xemTruoc(@PathVariable Long id) {
        return xetService.xemTruoc(id);
    }

    /** Xac nhan: ghi trang thai HOAN_THANH / CHUA_DAT va cap so giay xac nhan. */
    @PostMapping("/{id}/xet-hoan-thanh")
    @PreAuthorize("hasRole('ADMIN')")
    public List<XetHoanThanhService.KetQuaXet> xacNhan(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return xetService.xacNhan(id, user);
    }
}
