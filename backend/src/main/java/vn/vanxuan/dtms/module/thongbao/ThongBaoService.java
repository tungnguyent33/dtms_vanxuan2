package vn.vanxuan.dtms.module.thongbao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;

import java.util.List;

/**
 * Gui thong bao trong he thong (FR-16). Goi ben trong transaction cua nghiep vu: nghiep vu rollback thi
 * thong bao cung khong duoc tao. maSuKien (neu co) chong gui trung cho cung mot nguoi nhan.
 */
@Service
public class ThongBaoService {

    /** Loai thong bao - frontend dung de chon bieu tuong. */
    public static final String HO_SO = "HO_SO";
    public static final String HOC_PHI = "HOC_PHI";
    public static final String LICH_HOC = "LICH_HOC";
    public static final String NHAC_NO = "NHAC_NO";
    public static final String BAO_CAO_SO = "BAO_CAO_SO";
    public static final String KET_QUA = "KET_QUA";
    public static final String XE = "XE";

    private final ThongBaoRepository repo;
    private final NguoiDungRepository nguoiDungRepo;

    public ThongBaoService(ThongBaoRepository repo, NguoiDungRepository nguoiDungRepo) {
        this.repo = repo;
        this.nguoiDungRepo = nguoiDungRepo;
    }

    /** @return true neu da tao (false: nguoi nhan rong hoac su kien da gui roi). */
    @Transactional
    public boolean gui(Long nguoiNhanId, String loai, String tieuDe, String noiDung, String duongDan, String maSuKien) {
        if (nguoiNhanId == null) return false;
        if (maSuKien != null && repo.existsByNguoiNhanIdAndMaSuKien(nguoiNhanId, maSuKien)) return false;
        ThongBao t = new ThongBao();
        t.setNguoiNhanId(nguoiNhanId);
        t.setLoai(loai);
        t.setTieuDe(catNgan(tieuDe, 150));
        t.setNoiDung(noiDung);
        t.setDuongDan(duongDan);
        t.setMaSuKien(maSuKien);
        repo.save(t);
        return true;
    }

    /** Gui cho moi nguoi dung dang hoat dong cua cac vai tro. @return so thong bao da tao. */
    @Transactional
    public int guiTheoVaiTro(List<String> vaiTro, String loai, String tieuDe, String noiDung, String duongDan,
                             String maSuKien) {
        int n = 0;
        for (Long id : nguoiDungRepo.idTheoVaiTro(vaiTro)) {
            if (gui(id, loai, tieuDe, noiDung, duongDan, maSuKien)) n++;
        }
        return n;
    }

    private static String catNgan(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
