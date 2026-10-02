package vn.vanxuan.dtms.module.hocphi;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NhatKyService;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.SoThuTuService;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static vn.vanxuan.dtms.module.hocphi.HocPhiDtos.*;

/** Lap phieu thu, huy phieu, tinh cong no (UC11; BR-10, BR-11). */
@Service
public class HocPhiService {
    private static final DateTimeFormatter THANG = DateTimeFormatter.ofPattern("yyyyMM");

    private final PhieuThuRepository phieuRepo;
    private final DangKyRepository dangKyRepo;
    private final NguoiDungRepository nguoiDungRepo;
    private final SoThuTuService soThuTu;
    private final NhatKyService nhatKy;

    public HocPhiService(PhieuThuRepository phieuRepo, DangKyRepository dangKyRepo, NguoiDungRepository nguoiDungRepo,
                         SoThuTuService soThuTu, NhatKyService nhatKy) {
        this.phieuRepo = phieuRepo;
        this.dangKyRepo = dangKyRepo;
        this.nguoiDungRepo = nguoiDungRepo;
        this.soThuTu = soThuTu;
        this.nhatKy = nhatKy;
    }

    @Transactional
    public LapPhieuResponse lapPhieu(LapPhieuRequest req, AuthUser leTan) {
        // Khoa dong dang ky: 2 le tan thu tien cung luc se xep hang, khong vuot so con no
        DangKy dk = dangKyRepo.khoaDeThuTien(req.dangKyId())
                .orElseThrow(() -> new NotFoundException("hồ sơ đăng ký", req.dangKyId()));
        if (dk.getTrangThai() == DangKy.TrangThai.DA_HUY) {
            throw new BusinessException("HO_SO_DA_HUY", "Hồ sơ đã hủy, không thể thu tiền");
        }
        CongNo truoc = tinhCongNo(dk);
        if (req.soTien().compareTo(truoc.conNo()) > 0) {
            throw new BusinessException("SO_TIEN_KHONG_HOP_LE",
                    "Số tiền thu lớn hơn số còn nợ (" + truoc.conNo().toPlainString() + "đ)");
        }
        String thang = LocalDate.now().format(THANG);
        PhieuThu p = new PhieuThu();
        p.setSoPhieu(soThuTu.capMa("PT-" + thang, "PT-" + thang + "-", 4));
        p.setDangKy(dk);
        p.setSoTien(req.soTien());
        p.setHinhThuc(req.hinhThuc());
        p.setNoiDung(req.noiDung() == null || req.noiDung().isBlank()
                ? "Học phí khóa " + dk.getKhoa().getMaKhoa() : req.noiDung());
        p.setNguoiThu(nguoiDungRepo.getReferenceById(leTan.id()));
        phieuRepo.saveAndFlush(p);
        return new LapPhieuResponse(PhieuThuResponse.of(p), tinhCongNo(dk));
    }

    /** BR-11: khong xoa phieu, chi huy mem va bat buoc ly do; chi ADMIN (kiem tra o controller). */
    @Transactional
    public PhieuThuResponse huyPhieu(Long id, String lyDo, AuthUser admin) {
        PhieuThu p = phieuRepo.findChiTiet(id).orElseThrow(() -> new NotFoundException("phiếu thu", id));
        if (p.getTrangThai() == PhieuThu.TrangThai.DA_HUY) {
            throw new BusinessException("PHIEU_DA_HUY", "Phiếu đã được hủy trước đó");
        }
        p.setTrangThai(PhieuThu.TrangThai.DA_HUY);
        p.setLyDoHuy(lyDo);
        p.setNguoiHuy(nguoiDungRepo.getReferenceById(admin.id()));
        nhatKy.ghi(admin.id(), "HUY_PHIEU_THU", "phieu_thu", p.getId(),
                Map.of("soPhieu", p.getSoPhieu(), "soTien", p.getSoTien(), "lyDo", lyDo));
        return PhieuThuResponse.of(p);
    }

    @Transactional(readOnly = true)
    public List<PhieuThuResponse> dsPhieuCuaDangKy(Long dangKyId) {
        return phieuRepo.findByDangKyIdOrderByNgayThuDesc(dangKyId).stream()
                .map(PhieuThuResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CongNo congNo(Long dangKyId) {
        DangKy dk = dangKyRepo.findById(dangKyId).orElseThrow(() -> new NotFoundException("hồ sơ đăng ký", dangKyId));
        return tinhCongNo(dk);
    }

    public CongNo tinhCongNo(DangKy dk) {
        BigDecimal phaiDong = dk.phaiDong();
        BigDecimal daDong = phieuRepo.tongDaThu(dk.getId());
        return new CongNo(phaiDong, daDong, phaiDong.subtract(daDong));
    }
}
