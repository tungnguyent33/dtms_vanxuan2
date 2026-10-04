package vn.vanxuan.dtms.security;

/**
 * Nguoi dung dang dang nhap, lay tu JWT (khong can truy van CSDL moi request).
 * Dung trong controller: {@code @AuthenticationPrincipal AuthUser user}
 * phaiDoiMatKhau = true: dang dung mat khau tam, chi duoc goi /api/auth/** (xem JwtAuthFilter).
 */
public record AuthUser(Long id, String username, String vaiTro, boolean phaiDoiMatKhau) {
    public AuthUser(Long id, String username, String vaiTro) {
        this(id, username, vaiTro, false);
    }

    public boolean la(String role) {
        return role.equals(vaiTro);
    }
}
