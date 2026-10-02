package vn.vanxuan.dtms.module.lichhoc;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.module.danhmuc.GiaoVien;
import vn.vanxuan.dtms.module.danhmuc.GiaoVienRepository;
import vn.vanxuan.dtms.module.danhmuc.XeTapLai;
import vn.vanxuan.dtms.module.danhmuc.XeTapLaiRepository;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.DangKyRepository;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTao;
import vn.vanxuan.dtms.module.khoa.KhoaDaoTaoRepository;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;
import vn.vanxuan.dtms.module.nguoidung.VaiTro;
import vn.vanxuan.dtms.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vn.vanxuan.dtms.module.lichhoc.LichHocDtos.*;

/** Lap lich buoi hoc (BR-06) va diem danh (UC09; BR-05, BR-07). */
@Service
public class LichHocService {

    /** Trang thai dang ky duoc dua vao danh sach diem danh. */
    static final Set<DangKy.TrangThai> DANG_THEO_HOC = EnumSet.of(
            DangKy.TrangThai.DA_TIEP_NHAN, DangKy.TrangThai.DANG_HOC, DangKy.TrangThai.CHUA_DAT);

    private final BuoiHocRepository buoiRepo;
    private final DiemDanhRepository diemDanhRepo;
    private final KhoaDaoTaoRepository khoaRepo;
    private final GiaoVienRepository gvRepo;
    private final XeTapLaiRepository xeRepo;
    private final DangKyRepository dangKyRepo;
    private final NguoiDungRepository nguoiDungRepo;

    public LichHocService(BuoiHocRepository buoiRepo, DiemDanhRepository diemDanhRepo, KhoaDaoTaoRepository khoaRepo,
                          GiaoVienRepository gvRepo, XeTapLaiRepository xeRepo, DangKyRepository dangKyRepo,
                          NguoiDungRepository nguoiDungRepo) {
        this.buoiRepo = buoiRepo;
        this.diemDanhRepo = diemDanhRepo;
        this.khoaRepo = khoaRepo;
        this.gvRepo = gvRepo;
        this.xeRepo = xeRepo;
        this.dangKyRepo = dangKyRepo;
        this.nguoiDungRepo = nguoiDungRepo;
    }

    // ------------------------------------------------------------------ lap lich
    @Transactional
    public BuoiHocResponse taoBuoi(TaoBuoiRequest req) {
        KhoaDaoTao khoa = khoaRepo.findWithHang(req.khoaId())
                .orElseThrow(() -> new NotFoundException("khóa đào tạo", req.khoaId()));
        if (khoa.getTrangThai() == KhoaDaoTao.TrangThai.DA_KET_THUC || khoa.getTrangThai() == KhoaDaoTao.TrangThai.HUY) {
            throw new BusinessException("KHOA_DA_DONG", "Khóa đã kết thúc hoặc đã hủy");
        }
        if (req.ngay().isBefore(khoa.getNgayKhaiGiang()) || req.ngay().isAfter(khoa.getNgayBeGiang())) {
            throw new BusinessException("NGOAI_THOI_GIAN_KHOA",
                    "Ngày học phải nằm trong thời gian khóa (" + khoa.getNgayKhaiGiang() + " – " + khoa.getNgayBeGiang() + ")");
        }
        if (!req.gioKetThuc().isAfter(req.gioBatDau())) {
            throw new BusinessException("GIO_KHONG_HOP_LE", "Giờ kết thúc phải sau giờ bắt đầu");
        }
        GiaoVien gv = gvRepo.findById(req.giaoVienId())
                .orElseThrow(() -> new NotFoundException("giáo viên", req.giaoVienId()));
        if (buoiRepo.demTrungLichGiaoVien(gv.getId(), req.ngay(), req.gioBatDau(), req.gioKetThuc(), null,
                BuoiHoc.TrangThai.HUY) > 0) {
            throw new BusinessException("TRUNG_LICH_GIAO_VIEN", "Giáo viên " + gv.getHoTen() + " đã có buổi học trùng giờ");
        }
        XeTapLai xe = null;
        if (req.xeId() != null) {
            xe = xeRepo.findById(req.xeId()).orElseThrow(() -> new NotFoundException("xe tập lái", req.xeId()));
            if (xe.getTrangThai() != XeTapLai.TrangThaiXe.SAN_SANG) {
                throw new BusinessException("XE_KHONG_SAN_SANG", "Xe " + xe.getBienSo() + " đang " + xe.getTrangThai());
            }
            if (buoiRepo.demTrungLichXe(xe.getId(), req.ngay(), req.gioBatDau(), req.gioKetThuc(), null,
                    BuoiHoc.TrangThai.HUY) > 0) {
                throw new BusinessException("TRUNG_LICH_XE", "Xe " + xe.getBienSo() + " đã được xếp trùng giờ");
            }
        }
        BuoiHoc b = new BuoiHoc();
        b.setKhoa(khoa);
        b.setLoai(req.loai());
        b.setNgay(req.ngay());
        b.setGioBatDau(req.gioBatDau());
        b.setGioKetThuc(req.gioKetThuc());
        b.setDiaDiem(req.diaDiem());
        b.setGiaoVien(gv);
        b.setXe(xe);
        return BuoiHocResponse.of(buoiRepo.save(b));
    }

    @Transactional(readOnly = true)
    public List<BuoiHocResponse> timKiem(Long khoaId, LocalDate tu, LocalDate den) {
        return buoiRepo.timKiem(khoaId, tu, den).stream().map(BuoiHocResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<BuoiHocResponse> lichCuaToi(AuthUser user, LocalDate tu, LocalDate den) {
        if (user.la(VaiTro.GIAO_VIEN)) {
            return buoiRepo.lichCuaGiaoVien(user.id(), tu, den, BuoiHoc.TrangThai.HUY).stream()
                    .map(BuoiHocResponse::of).toList();
        } else if (user.la(VaiTro.HOC_VIEN)) {
            return buoiRepo.lichCuaHocVien(user.id(), tu, den, BuoiHoc.TrangThai.HUY, DangKy.TrangThai.DA_HUY).stream()
                    .map(BuoiHocResponse::of).toList();
        }
        return List.of();
    }

    // ------------------------------------------------------------------ diem danh
    @Transactional(readOnly = true)
    public DanhSachBuoi danhSach(Long buoiId, AuthUser user) {
        BuoiHoc b = layBuoiVaKiemTraQuyen(buoiId, user);
        Map<Long, DiemDanh> daCo = diemDanhRepo.findByBuoiHocId(buoiId).stream()
                .collect(Collectors.toMap(d -> d.getDangKy().getId(), Function.identity()));
        List<DongDiemDanh> dong = hocVienCuaBuoi(b).stream().map(dk -> {
            DiemDanh d = daCo.get(dk.getId());
            return new DongDiemDanh(dk.getId(), dk.getMaHoSo(), dk.getHocVien().getHoTen(),
                    dk.getHocVien().getSoDienThoai(), dk.getHinhThucLyThuyet().name(),
                    d == null ? null : d.getCoMat(), d == null ? null : d.getSoGio(), d == null ? null : d.getGhiChu());
        }).toList();
        return new DanhSachBuoi(BuoiHocResponse.of(b), dong);
    }

    @Transactional
    public DanhSachBuoi luuDiemDanh(Long buoiId, LuuDiemDanhRequest req, AuthUser user) {
        BuoiHoc b = layBuoiVaKiemTraQuyen(buoiId, user);
        if (b.getTrangThai() == BuoiHoc.TrangThai.HUY) {
            throw new BusinessException("BUOI_DA_HUY", "Buổi học đã hủy");
        }
        if (b.getNgay().isAfter(LocalDate.now())) {
            throw new BusinessException("CHUA_DEN_NGAY", "Chưa đến ngày học, không thể điểm danh trước");
        }
        BigDecimal thoiLuong = b.thoiLuongGio();
        Map<Long, DangKy> hopLe = hocVienCuaBuoi(b).stream()
                .collect(Collectors.toMap(DangKy::getId, Function.identity()));

        for (DiemDanhItem item : req.danhSach()) {
            DangKy dk = hopLe.get(item.dangKyId());
            if (dk == null) {
                throw new BusinessException("HOC_VIEN_KHONG_THUOC_BUOI",
                        "Hồ sơ " + item.dangKyId() + " không thuộc danh sách buổi học này");
            }
            BigDecimal soGio = Boolean.TRUE.equals(item.coMat()) ? item.soGio() : BigDecimal.ZERO;   // BR-07
            if (soGio.compareTo(thoiLuong) > 0) {
                throw new BusinessException("VUOT_THOI_LUONG",
                        dk.getHocVien().getHoTen() + ": số giờ vượt thời lượng buổi (" + thoiLuong + " giờ)");
            }
            DiemDanh d = diemDanhRepo.findByBuoiHocIdAndDangKyId(buoiId, dk.getId()).orElseGet(() -> {
                DiemDanh moi = new DiemDanh();
                moi.setBuoiHoc(b);
                moi.setDangKy(dk);
                return moi;
            });
            d.setCoMat(item.coMat());
            d.setSoGio(soGio);
            d.setGhiChu(item.ghiChu());
            d.setNguoiGhi(nguoiDungRepo.getReferenceById(user.id()));
            d.setGhiLuc(LocalDateTime.now());
            diemDanhRepo.save(d);
        }
        b.setTrangThai(BuoiHoc.TrangThai.DA_DAY);
        diemDanhRepo.flush();
        return danhSach(buoiId, user);
    }

    // ------------------------------------------------------------------ ho tro
    private BuoiHoc layBuoiVaKiemTraQuyen(Long buoiId, AuthUser user) {
        BuoiHoc b = buoiRepo.findChiTiet(buoiId).orElseThrow(() -> new NotFoundException("buổi học", buoiId));
        if (user.la(VaiTro.GIAO_VIEN)) {
            var nd = b.getGiaoVien().getNguoiDung();
            if (nd == null || !nd.getId().equals(user.id())) {
                throw new BusinessException("KHONG_PHU_TRACH", "Bạn không được phân công buổi học này",
                        HttpStatus.FORBIDDEN);
            }
        }
        return b;
    }

    /** BR-05: buoi ly thuyet chi gom hoc vien hoc TAP_TRUNG; buoi thuc hanh gom tat ca. */
    private List<DangKy> hocVienCuaBuoi(BuoiHoc b) {
        return dangKyRepo.findByKhoaIdAndTrangThaiInOrderByHocVienHoTen(b.getKhoa().getId(), DANG_THEO_HOC).stream()
                .filter(dk -> b.getLoai() == BuoiHoc.Loai.THUC_HANH
                        || dk.getHinhThucLyThuyet() == DangKy.HinhThucLyThuyet.TAP_TRUNG)
                .toList();
    }
}
