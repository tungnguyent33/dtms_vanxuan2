package vn.vanxuan.dtms.common;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Tinh han theo ngay lam viec (BR-12: gui danh sach hoan thanh ve So trong 02 ngay lam viec).
 * Chi bo qua thu Bay, Chu nhat; ngay le do quan tri vien tu luu y.
 */
public final class NgayLamViec {
    private NgayLamViec() {
    }

    public static LocalDate cong(LocalDate tu, int soNgay) {
        LocalDate d = tu;
        int con = soNgay;
        while (con > 0) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) con--;
        }
        return d;
    }
}
