package vn.vanxuan.dtms.module.thongbao;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.PageResponse;
import vn.vanxuan.dtms.security.AuthUser;

import java.time.LocalDateTime;
import java.util.Map;

/** Thong bao cua nguoi dang dang nhap (FR-15, FR-16). Moi vai tro deu dung; chi thay thong bao cua minh. */
@RestController
@RequestMapping("/api/thong-bao")
public class ThongBaoController {

    public record ThongBaoResponse(Long id, String loai, String tieuDe, String noiDung, String duongDan,
                                   boolean daDoc, LocalDateTime thoiGian) {
        static ThongBaoResponse of(ThongBao t) {
            return new ThongBaoResponse(t.getId(), t.getLoai(), t.getTieuDe(), t.getNoiDung(), t.getDuongDan(),
                    Boolean.TRUE.equals(t.getDaDoc()), t.getCreatedAt());
        }
    }

    private final ThongBaoRepository repo;
    private final NhacViecService nhacViec;

    public ThongBaoController(ThongBaoRepository repo, NhacViecService nhacViec) {
        this.repo = repo;
        this.nhacViec = nhacViec;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<ThongBaoResponse> ds(@AuthenticationPrincipal AuthUser user,
                                            @RequestParam(defaultValue = "false") boolean chuaDoc,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50));
        var kq = chuaDoc ? repo.findByNguoiNhanIdAndDaDocFalseOrderByIdDesc(user.id(), pageable)
                : repo.findByNguoiNhanIdOrderByIdDesc(user.id(), pageable);
        return PageResponse.of(kq, ThongBaoResponse::of);
    }

    /** Frontend goi dinh ky de hien so tren chuong - truy van nhe (co index). */
    @GetMapping("/so-chua-doc")
    public Map<String, Long> soChuaDoc(@AuthenticationPrincipal AuthUser user) {
        return Map.of("soChuaDoc", repo.countByNguoiNhanIdAndDaDocFalse(user.id()));
    }

    @PatchMapping("/{id}/da-doc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void daDoc(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        // Tim theo ca id va nguoi nhan: khong danh dau duoc thong bao cua nguoi khac
        repo.findByIdAndNguoiNhanId(id, user.id()).orElseThrow(() -> new NotFoundException("thông báo", id)).setDaDoc(true);
    }

    @PatchMapping("/da-doc-tat-ca")
    @Transactional
    public Map<String, Integer> daDocTatCa(@AuthenticationPrincipal AuthUser user) {
        return Map.of("soDaDanhDau", repo.danhDauDaDocTatCa(user.id()));
    }

    /** Chay ngay tac vu nhac viec (binh thuong chay tu dong luc 7h sang) - de quan tri vien kiem tra / demo. */
    @PostMapping("/chay-nhac-viec")
    @PreAuthorize("hasRole('ADMIN')")
    public NhacViecService.KetQuaNhacViec chayNhacViec() {
        return nhacViec.chay();
    }
}
