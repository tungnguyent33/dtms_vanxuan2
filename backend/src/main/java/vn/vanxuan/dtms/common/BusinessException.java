package vn.vanxuan.dtms.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Loi vi pham quy tac nghiep vu (BR-xx). Tra ve HTTP 422 kem ma loi de frontend hien thi.
 * details (tuy chon): du lieu kem theo de frontend xu ly tiep, VD goi y khoa khac khi khoa da du si so.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final Map<String, String> details;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public BusinessException(String code, String message, HttpStatus status, Map<String, String> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }
}
