package vn.vanxuan.dtms.common;

import java.time.LocalDateTime;
import java.util.Map;

/** Dinh dang loi thong nhat: { code, message, details, timestamp } */
public record ApiError(String code, String message, Map<String, String> details, LocalDateTime timestamp) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, LocalDateTime.now());
    }

    public static ApiError of(String code, String message, Map<String, String> details) {
        return new ApiError(code, message, details, LocalDateTime.now());
    }
}
