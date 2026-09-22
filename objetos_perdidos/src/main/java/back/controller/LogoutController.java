package back.controller;

import back.api.SessionManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@RestController
public class LogoutController {

    @PostMapping("/api/logout")
    public Map<String, Object> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = leerCookie(request, "fyb_session");

        if (token != null) {
            SessionManager.cerrar(token);
        }

        ResponseCookie cookie = ResponseCookie
                .from("fyb_session", "")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.setHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return Map.of("ok", true);
    }

    private String leerCookie(
            HttpServletRequest request,
            String nombre) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}