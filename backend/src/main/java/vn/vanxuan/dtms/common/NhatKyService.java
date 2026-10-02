package vn.vanxuan.dtms.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Ghi nhat ky cac thao tac nhay cam (huy phieu thu, sua hoc phi, xet hoan thanh...). */
@Service
public class NhatKyService {

    private final NhatKyRepository repo;
    private final ObjectMapper objectMapper;

    public NhatKyService(NhatKyRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public void ghi(Long nguoiDungId, String hanhDong, String doiTuong, Long doiTuongId, Map<String, ?> duLieu) {
        NhatKy nk = new NhatKy();
        nk.setNguoiDungId(nguoiDungId);
        nk.setHanhDong(hanhDong);
        nk.setDoiTuong(doiTuong);
        nk.setDoiTuongId(doiTuongId);
        try {
            nk.setDuLieu(duLieu == null ? null : objectMapper.writeValueAsString(duLieu));
        } catch (JsonProcessingException e) {
            nk.setDuLieu(null);
        }
        repo.save(nk);
    }
}
