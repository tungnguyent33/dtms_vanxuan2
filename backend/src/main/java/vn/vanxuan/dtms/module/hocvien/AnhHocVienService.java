package vn.vanxuan.dtms.module.hocvien;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.vanxuan.dtms.common.BusinessException;
import vn.vanxuan.dtms.common.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Luu anh chan dung / CCCD vao thu muc rieng tren may chu (KHONG phai thu muc public).
 * Anh chi duoc tai ve qua API co xac thuc - day la du lieu ca nhan.
 */
@Service
public class AnhHocVienService {
    public enum LoaiAnh { CHAN_DUNG, CCCD_TRUOC, CCCD_SAU }

    private static final Set<String> KIEU_CHO_PHEP = Set.of("image/jpeg", "image/png");

    private final HocVienRepository repo;
    private final Path thuMuc;

    public AnhHocVienService(HocVienRepository repo, @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.repo = repo;
        this.thuMuc = Path.of(uploadDir, "hoc-vien").toAbsolutePath().normalize();
        Files.createDirectories(thuMuc);
    }

    @Transactional
    public void luu(Long hocVienId, LoaiAnh loai, MultipartFile file) throws IOException {
        HocVien hv = repo.findById(hocVienId).orElseThrow(() -> new NotFoundException("học viên", hocVienId));
        if (file.isEmpty() || !KIEU_CHO_PHEP.contains(file.getContentType())) {
            throw new BusinessException("TEP_KHONG_HOP_LE", "Chỉ nhận ảnh JPG hoặc PNG");
        }
        String duoi = "image/png".equals(file.getContentType()) ? ".png" : ".jpg";
        String ten = hocVienId + "_" + loai.name().toLowerCase() + "_" + UUID.randomUUID() + duoi;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, thuMuc.resolve(ten), StandardCopyOption.REPLACE_EXISTING);
        }
        String cu = switch (loai) {
            case CHAN_DUNG -> hv.getAnhChanDungUrl();
            case CCCD_TRUOC -> hv.getAnhCccdTruocUrl();
            case CCCD_SAU -> hv.getAnhCccdSauUrl();
        };
        switch (loai) {
            case CHAN_DUNG -> hv.setAnhChanDungUrl(ten);
            case CCCD_TRUOC -> hv.setAnhCccdTruocUrl(ten);
            case CCCD_SAU -> hv.setAnhCccdSauUrl(ten);
        }
        if (cu != null) {
            Files.deleteIfExists(thuMuc.resolve(cu).normalize());
        }
    }

    @Transactional(readOnly = true)
    public Path duongDan(Long hocVienId, LoaiAnh loai) {
        HocVien hv = repo.findById(hocVienId).orElseThrow(() -> new NotFoundException("học viên", hocVienId));
        String ten = switch (loai) {
            case CHAN_DUNG -> hv.getAnhChanDungUrl();
            case CCCD_TRUOC -> hv.getAnhCccdTruocUrl();
            case CCCD_SAU -> hv.getAnhCccdSauUrl();
        };
        if (ten == null) throw new NotFoundException("ảnh", loai);
        Path p = thuMuc.resolve(ten).normalize();
        if (!p.startsWith(thuMuc) || !Files.exists(p)) throw new NotFoundException("ảnh", loai);
        return p;
    }
}
