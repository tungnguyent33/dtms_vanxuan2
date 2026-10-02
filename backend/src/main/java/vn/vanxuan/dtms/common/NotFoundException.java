package vn.vanxuan.dtms.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {
    public NotFoundException(String doiTuong, Object id) {
        super("KHONG_TIM_THAY", "Không tìm thấy " + doiTuong + " (" + id + ")", HttpStatus.NOT_FOUND);
    }
}
