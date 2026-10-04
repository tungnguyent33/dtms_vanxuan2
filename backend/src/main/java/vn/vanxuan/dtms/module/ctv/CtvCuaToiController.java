package vn.vanxuan.dtms.module.ctv;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.module.danhmuc.CongTacVien;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.util.List;

import static vn.vanxuan.dtms.module.ctv.CtvDtos.*;

/**
 * Cong CTV - chi du lieu cua chinh minh: gui lead, xem lead, xem hoa hong.
 * Dung cho tai khoan vai tro CTV va hoc vien (hoc vien gui SDT nguoi quen / xin lam CTV; neu da la CTV thi xem hoa hong).
 * Khong co API sua hoc phi, sua trang thai lead hay xem CTV khac.
 */
@RestController
@RequestMapping("/api/ctv-cua-toi")
@PreAuthorize("hasAnyRole('CTV','HOC_VIEN')")
public class CtvCuaToiController {

    private final CtvService ctvService;
    private final LeadService leadService;
    private final HoaHongService hoaHongService;
    private final LeadKhachRepository leadRepo;

    public CtvCuaToiController(CtvService ctvService, LeadService leadService, HoaHongService hoaHongService,
                               LeadKhachRepository leadRepo) {
        this.ctvService = ctvService;
        this.leadService = leadService;
        this.hoaHongService = hoaHongService;
        this.leadRepo = leadRepo;
    }

    /** Ho so CTV cua toi (null neu hoc vien chua xin lam CTV). */
    @GetMapping
    @Transactional(readOnly = true)
    public CtvCuaToi thongTin(@AuthenticationPrincipal AuthUser user) {
        CongTacVien c = ctvService.cuaNguoiDung(user).orElse(null);
        if (c == null) return null;
        var hh = hoaHongService.cuaCtv(c.getId());
        return new CtvCuaToi(c.getId(), c.getHoTen(), c.getTrangThai().name(), c.getHang().name(),
                leadRepo.countByCtvId(c.getId()), leadRepo.countByCtvIdAndTrangThai(c.getId(), LeadKhach.TrangThai.DA_CHOT),
                tong(hh, "DU_DIEU_KIEN"), tong(hh, "DA_DUYET"), tong(hh, "DA_CHI"));
    }

    @GetMapping("/lead")
    public List<LeadResponse> lead(@AuthenticationPrincipal AuthUser user) {
        return leadService.cuaToi(user);
    }

    @PostMapping("/lead")
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse guiLead(@Valid @RequestBody LeadRequest req, @AuthenticationPrincipal AuthUser user) {
        return leadService.taoBoiCtvHoacHocVien(req, user);
    }

    /** Chi xem hoa hong cua chinh minh; khong thay ty le / co so tinh cua CTV khac. */
    @GetMapping("/hoa-hong")
    public List<HoaHongResponse> hoaHong(@AuthenticationPrincipal AuthUser user) {
        CongTacVien c = ctvService.cuaNguoiDung(user)
                .orElseThrow(() -> new BusinessException("CHUA_LA_CTV", "Bạn chưa là cộng tác viên", HttpStatus.NOT_FOUND));
        return hoaHongService.cuaCtv(c.getId());
    }

    @PostMapping("/xin-lam-ctv")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HOC_VIEN')")
    public CtvResponse xinLamCtv(@Valid @RequestBody XinLamCtvRequest req, @AuthenticationPrincipal AuthUser user) {
        if (!user.la(VaiTro.HOC_VIEN)) throw new BusinessException("KHONG_PHAI_HOC_VIEN", "Chỉ học viên gửi yêu cầu này");
        return ctvService.xinLamCtv(req, user);
    }

    private static BigDecimal tong(List<HoaHongResponse> ds, String trangThai) {
        return ds.stream().filter(h -> trangThai.equals(h.trangThai())).map(HoaHongResponse::soTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
