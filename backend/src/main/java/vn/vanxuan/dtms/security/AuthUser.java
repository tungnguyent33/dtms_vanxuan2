package vn.vanxuan.dtms.security;

/**
 * Nguoi dung dang dang nhap, lay tu JWT (khong can truy van CSDL moi request).
 * Dung trong controller: {@code @AuthenticationPrincipal AuthUser user}
 */
public record AuthUser(Long id, String username, String vaiTro) {
    public boolean la(String role) {
        return role.equals(vaiTro);
    }
}
