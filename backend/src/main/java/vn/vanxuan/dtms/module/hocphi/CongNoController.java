package vn.vanxuan.dtms.module.hocphi;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/** Danh sach cong no doc tu view v_cong_no; xuat Excel bang Apache POI. */
@RestController
@RequestMapping("/api/cong-no")
@PreAuthorize("hasAnyRole('ADMIN','LE_TAN')")
public class CongNoController {

    public record DongCongNo(Long dangKyId, String maHoSo, String trangThai, String hoTen, String soDienThoai,
                             Long khoaId, String maKhoa, BigDecimal phaiDong, BigDecimal daDong, BigDecimal conNo) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public CongNoController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<DongCongNo> ds(@RequestParam(required = false) Long khoaId,
                               @RequestParam(defaultValue = "true") boolean chiConNo) {
        String sql = "SELECT * FROM v_cong_no WHERE (:khoaId IS NULL OR khoa_id = :khoaId)"
                + (chiConNo ? " AND con_no > 0" : "") + " ORDER BY ma_khoa DESC, ho_ten";
        var params = new MapSqlParameterSource("khoaId", khoaId);
        return jdbc.query(sql, params, (rs, i) -> new DongCongNo(
                rs.getLong("dang_ky_id"), rs.getString("ma_ho_so"), rs.getString("trang_thai"),
                rs.getString("ho_ten"), rs.getString("so_dien_thoai"), rs.getLong("khoa_id"),
                rs.getString("ma_khoa"), rs.getBigDecimal("phai_dong"), rs.getBigDecimal("da_dong"),
                rs.getBigDecimal("con_no")));
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) Long khoaId,
                                        @RequestParam(defaultValue = "true") boolean chiConNo) throws IOException {
        List<DongCongNo> rows = ds(khoaId, chiConNo);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Công nợ");
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            String[] cols = {"STT", "Mã hồ sơ", "Họ tên", "Số điện thoại", "Khóa", "Phải đóng", "Đã đóng", "Còn nợ"};
            Row h = sh.createRow(0);
            for (int c = 0; c < cols.length; c++) {
                Cell cell = h.createCell(c);
                cell.setCellValue(cols[c]);
                cell.setCellStyle(head);
            }
            int r = 1;
            for (DongCongNo d : rows) {
                Row row = sh.createRow(r);
                row.createCell(0).setCellValue(r);
                row.createCell(1).setCellValue(d.maHoSo());
                row.createCell(2).setCellValue(d.hoTen());
                row.createCell(3).setCellValue(d.soDienThoai());
                row.createCell(4).setCellValue(d.maKhoa());
                tien(row, 5, d.phaiDong(), money);
                tien(row, 6, d.daDong(), money);
                tien(row, 7, d.conNo(), money);
                r++;
            }
            if (r > 1) {
                Row tong = sh.createRow(r);
                Cell nhan = tong.createCell(4);
                nhan.setCellValue("Tổng");
                nhan.setCellStyle(head);
                for (int c = 5; c <= 7; c++) {
                    Cell cell = tong.createCell(c);
                    String col = String.valueOf((char) ('A' + c));
                    cell.setCellFormula("SUM(" + col + "2:" + col + r + ")");
                    cell.setCellStyle(money);
                }
            }
            // Dat do rong co dinh (don vi 1/256 ky tu); autoSizeColumn can font he thong, de loi trong Docker
            int[] rong = {6, 16, 28, 14, 16, 14, 14, 14};
            for (int c = 0; c < rong.length; c++) sh.setColumnWidth(c, rong[c] * 256);
            wb.write(out);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("cong-no.xlsx").build().toString())
                    .body(out.toByteArray());
        }
    }

    private static void tien(Row row, int col, BigDecimal v, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(v == null ? 0 : v.doubleValue());
        c.setCellStyle(style);
    }
}
