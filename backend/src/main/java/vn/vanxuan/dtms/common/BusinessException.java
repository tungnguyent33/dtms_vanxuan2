package vn.vanxuan.dtms.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Loi vi pham quy tac nghiep vu (BR-xx). Tra ve HTTP 422 kem ma loi de frontend hien thi.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
