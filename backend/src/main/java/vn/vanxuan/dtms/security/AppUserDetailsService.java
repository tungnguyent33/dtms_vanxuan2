package vn.vanxuan.dtms.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.vanxuan.dtms.module.nguoidung.NguoiDung;
import vn.vanxuan.dtms.module.nguoidung.NguoiDungRepository;

/** Chi dung khi dang nhap: nap tai khoan tu CSDL de kiem tra mat khau BCrypt. */
@Service
public class AppUserDetailsService implements UserDetailsService {
    private final NguoiDungRepository repo;

    public AppUserDetailsService(NguoiDungRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        NguoiDung nd = repo.findByTenDangNhap(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(nd.getTenDangNhap())
                .password(nd.getMatKhauHash())
                .roles(nd.getVaiTro().getMa())
                .disabled(!Boolean.TRUE.equals(nd.getTrangThai()))
                .build();
    }
}
