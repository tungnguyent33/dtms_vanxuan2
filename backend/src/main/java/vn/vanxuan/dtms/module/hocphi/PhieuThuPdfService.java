package vn.vanxuan.dtms.module.hocphi;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vanxuan.dtms.common.NotFoundException;
import vn.vanxuan.dtms.common.SoThanhChu;
import vn.vanxuan.dtms.module.hocvien.DangKy;
import vn.vanxuan.dtms.module.hocvien.HocVien;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** In phieu thu kho A5 bang OpenPDF, font DejaVu de hien thi tieng Viet. */
@Service
public class PhieuThuPdfService {
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final PhieuThuRepository repo;
    private final BaseFont fontThuong;
    private final BaseFont fontDam;

    public PhieuThuPdfService(PhieuThuRepository repo) throws IOException {
        this.repo = repo;
        this.fontThuong = napFont("fonts/DejaVuSans.ttf");
        this.fontDam = napFont("fonts/DejaVuSans-Bold.ttf");
    }

    private static BaseFont napFont(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(path.substring(path.lastIndexOf('/') + 1), BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, bytes, null);
        }
    }

    @Transactional(readOnly = true)
    public byte[] taoPdf(Long phieuId) {
        PhieuThu p = repo.findChiTiet(phieuId).orElseThrow(() -> new NotFoundException("phiếu thu", phieuId));
        DangKy dk = p.getDangKy();
        HocVien hv = dk.getHocVien();
        NumberFormat vnd = NumberFormat.getInstance(Locale.of("vi", "VN"));

        Font tieuDe = new Font(fontDam, 16);
        Font dam = new Font(fontDam, 10);
        Font thuong = new Font(fontThuong, 10);
        Font nghieng = new Font(fontThuong, 9, Font.ITALIC);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 36, 36, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        PdfPTable dau = new PdfPTable(new float[]{3, 2});
        dau.setWidthPercentage(100);
        dau.addCell(o(new Phrase("TRUNG TÂM ĐÀO TẠO LÁI XE PHÚ THỌ\nChi nhánh Vạn Xuân – Tam Nông", dam), Element.ALIGN_LEFT));
        dau.addCell(o(new Phrase("Số: " + p.getSoPhieu() + "\nNgày: " + p.getNgayThu().format(NGAY), thuong), Element.ALIGN_RIGHT));
        doc.add(dau);

        Paragraph td = new Paragraph("PHIẾU THU HỌC PHÍ", tieuDe);
        td.setAlignment(Element.ALIGN_CENTER);
        td.setSpacingBefore(10);
        td.setSpacingAfter(10);
        doc.add(td);
        if (p.getTrangThai() == PhieuThu.TrangThai.DA_HUY) {
            Paragraph huy = new Paragraph("ĐÃ HỦY – " + p.getLyDoHuy(), new Font(fontDam, 12, Font.NORMAL, java.awt.Color.RED));
            huy.setAlignment(Element.ALIGN_CENTER);
            doc.add(huy);
        }

        PdfPTable t = new PdfPTable(new float[]{1.4f, 3.6f});
        t.setWidthPercentage(100);
        dong(t, "Họ tên học viên:", hv.getHoTen() + "  (" + hv.getMaHocVien() + ")", dam, thuong);
        dong(t, "Số CCCD:", hv.getCccd(), dam, thuong);
        dong(t, "Khóa / hạng:", dk.getKhoa().getMaKhoa() + " – hạng " + dk.getKhoa().getHang().getMa()
                + "   (hồ sơ " + dk.getMaHoSo() + ")", dam, thuong);
        dong(t, "Nội dung:", p.getNoiDung(), dam, thuong);
        dong(t, "Số tiền:", vnd.format(p.getSoTien()) + " đồng", dam, dam);
        dong(t, "Bằng chữ:", SoThanhChu.doc(p.getSoTien().longValue()), dam, nghieng);
        dong(t, "Hình thức:", p.getHinhThuc() == PhieuThu.HinhThuc.TIEN_MAT ? "Tiền mặt"
                : p.getHinhThuc() == PhieuThu.HinhThuc.CHUYEN_KHOAN ? "Chuyển khoản" : "VNPay", dam, thuong);
        doc.add(t);

        PdfPTable ky = new PdfPTable(2);
        ky.setWidthPercentage(100);
        ky.setSpacingBefore(18);
        ky.addCell(o(new Phrase("Người nộp tiền\n(Ký, ghi rõ họ tên)", dam), Element.ALIGN_CENTER));
        ky.addCell(o(new Phrase("Người thu tiền\n(Ký, ghi rõ họ tên)\n\n\n\n" + p.getNguoiThu().getHoTen(), dam), Element.ALIGN_CENTER));
        doc.add(ky);

        doc.close();
        return out.toByteArray();
    }

    private static PdfPCell o(Phrase ph, int canLe) {
        PdfPCell c = new PdfPCell(ph);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(canLe);
        return c;
    }

    private static void dong(PdfPTable t, String nhan, String giaTri, Font fNhan, Font fGiaTri) {
        PdfPCell a = o(new Phrase(nhan, fNhan), Element.ALIGN_LEFT);
        PdfPCell b = o(new Phrase(giaTri == null ? "" : giaTri, fGiaTri), Element.ALIGN_LEFT);
        a.setPaddingBottom(5);
        b.setPaddingBottom(5);
        t.addCell(a);
        t.addCell(b);
    }
}
