package com.smalltalk.SmallTalkFootball.config;

import com.smalltalk.SmallTalkFootball.entities.User;
import com.smalltalk.SmallTalkFootball.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isJwtRequired(request)) {

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            String jwt = authHeader.substring(7);
            try {
                String userEmail = jwtUtil.extractUserName(jwt);
                User user = userService.getUserByEmail(userEmail);
                if (!jwtUtil.isTokenValidated(jwt, user)) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    return;
                }
            } catch (Exception e) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isJwtRequired(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return "/small-infos".equals(uri) && "POST".equals(method) || "DELETE".equals(method);
    }
}
