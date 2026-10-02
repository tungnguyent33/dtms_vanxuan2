package vn.vanxuan.dtms.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cap so thu tu khong trung lap, an toan khi nhieu nguoi thao tac dong thoi.
 * Phai duoc goi ben trong mot transaction dang chay (MANDATORY) de khoa giu den luc commit.
 */
@Service
public class SoThuTuService {
    private final BoDemRepository repo;

    public SoThuTuService(BoDemRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long capSo(String ten) {
        repo.taoNeuChuaCo(ten);
        BoDem dem = repo.khoaDeCapSo(ten).orElseThrow();
        long next = dem.getGiaTri() + 1;
        dem.setGiaTri(next);
        return next;
    }

    /** VD: capMa("PT-202610", "PT-202610-", 4) -> "PT-202610-0007" */
    @Transactional(propagation = Propagation.MANDATORY)
    public String capMa(String tenBoDem, String tienTo, int doDai) {
        return tienTo + String.format("%0" + doDai + "d", capSo(tenBoDem));
    }
}
