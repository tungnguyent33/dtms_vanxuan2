package vn.vanxuan.dtms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gioi han tan suat theo dia chi IP cho cac API de bi lam dung (yeu cau phi chuc nang, muc 4.4):
 * dang nhap, lam moi phien, dang ky truc tuyen, tra cuu ho so, lay captcha.
 * Cua so co dinh trong bo nho - du cho trien khai mot may chu. Nginx co the chan them mot lop (nginx.conf).
 * KHONG danh dau @Component (ly do giong JwtAuthFilter): dang ky trong SecurityConfig.
 */
public class GioiHanTanSuatFilter extends OncePerRequestFilter {

    private record Luat(String method, String path, int soLan, long giayCuaSo) {
    }

    private static final List<Luat> LUAT = List.of(
            new Luat("POST", "/api/auth/login", 10, 60),
            new Luat("POST", "/api/auth/refresh", 30, 60),
            new Luat("POST", "/api/public/dang-ky", 5, 600),
            new Luat("POST", "/api/public/tra-cuu", 10, 60),
            new Luat("GET", "/api/public/captcha", 30, 60));

    private final Map<String, AtomicInteger> dem = new ConcurrentHashMap<>();
    private final boolean bat;
    private volatile long lanDonCuoi = 0;

    public GioiHanTanSuatFilter(boolean bat) {
        this.bat = bat;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (bat) {
            Luat luat = timLuat(request);
            if (luat != null) {
                long cuaSo = System.currentTimeMillis() / 1000 / luat.giayCuaSo();
                String khoa = luat.path() + "|" + diaChi(request) + "|" + cuaSo;
                int lan = dem.computeIfAbsent(khoa, k -> new AtomicInteger()).incrementAndGet();
                donDep();
                if (lan > luat.soLan()) {
                    long conLai = luat.giayCuaSo() - (System.currentTimeMillis() / 1000) % luat.giayCuaSo();
                    response.setStatus(429);
                    response.setHeader("Retry-After", Long.toString(conLai));
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"code\":\"QUA_NHIEU_YEU_CAU\",\"message\":\"Bạn thao tác quá nhanh. "
                            + "Vui lòng thử lại sau " + conLai + " giây\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static Luat timLuat(HttpServletRequest req) {
        String uri = req.getRequestURI();
        for (Luat l : LUAT) {
            if (l.method().equals(req.getMethod()) && l.path().equals(uri)) return l;
        }
        return null;
    }

    /**
     * Chi tin header X-Real-IP / X-Forwarded-For khi request den tu proxy noi bo (nginx trong Docker, Vite khi dev);
     * neu backend bi goi truc tiep tu Internet thi dung dia chi ket noi that de khong bi gia mao IP.
     */
    static String diaChi(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        if (laNoiBo(remote)) {
            String real = req.getHeader("X-Real-IP");
            if (real != null && !real.isBlank()) return real.trim();
            String fwd = req.getHeader("X-Forwarded-For");
            if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        }
        return remote;
    }

    private static boolean laNoiBo(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            return a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /** Xoa bo dem cua cac cua so cu, toi da moi phut mot lan. */
    private void donDep() {
        long now = System.currentTimeMillis();
        if (now - lanDonCuoi < 60_000) return;
        lanDonCuoi = now;
        long giay = now / 1000;
        dem.keySet().removeIf(k -> {
            String[] p = k.split("\\|");
            Luat l = LUAT.stream().filter(x -> x.path().equals(p[0])).findFirst().orElse(null);
            return l == null || Long.parseLong(p[2]) < giay / l.giayCuaSo();
        });
    }
}
