package com.smalltalk.SmallTalkFootball.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.enums.Role;
import com.smalltalk.SmallTalkFootball.services.UserService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
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

    public static final String DELETE = "DELETE";
    public static final String PATCH = "PATCH";
    public static final String POST = "POST";
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isJwtRequired(request)) {

            if (!isAuthHeaderValid(request)) {
                sendUnauthorizedResponse(response, "You are unauthorized to make this action. If you think you should be, please log in again.");
                return;
            }

            String jwt = getAuthorizationHeader(request).substring(7);

            try {

                String userEmail = jwtUtil.extractUserEmail(jwt);
                User user = userService.getUserByEmail(userEmail);

                if (!jwtUtil.isTokenValidated(jwt, user)) {
                    sendUnauthorizedResponse(response, "You are unauthorized to make this action. If you think you should be, please log in again.");
                    return;
                }

                if (isAdminOnlyRequest(request) && user.getRole() != Role.ADMIN) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    return;
                }

            } catch (Exception e) {
                sendUnauthorizedResponse(response, "Unauthorized request");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String getAuthorizationHeader(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    private boolean isAdminOnlyRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return DELETE.equals(method)
                || "articles/pending".equals(uri)
                || (uri.startsWith("/articles/") && PATCH.equals(method))
                || uri.startsWith("/teams")
                || uri.startsWith("/fixtures") && POST.equals(method);
    }

    private static boolean isJwtRequired(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return DELETE.equals(method)
                || isJwtRequiredSmallInfos(uri, method)
                || isJwtRequiredArticles(uri, method)
                || isJwtRequiredTeams(uri)
                || isJwtRequiredFixtures(uri, method);
    }

    private boolean isAuthHeaderValid(HttpServletRequest request) {
        String authHeader = getAuthorizationHeader(request);
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private static boolean isJwtRequiredSmallInfos(String uri, String method) {
        return uri.startsWith("/small-infos") && POST.equals(method);
    }

    private static boolean isJwtRequiredArticles(String uri, String method) {
        return uri.startsWith("/articles") && (POST.equals(method) || PATCH.equals(method));
    }

    private static boolean isJwtRequiredFixtures(String uri, String method) {
        return uri.startsWith("/fixtures") && POST.equals(method);
    }

    private static boolean isJwtRequiredTeams(String uri) {
        return uri.startsWith("/teams");
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        SmallTalkResponse<?> errorResponse = new SmallTalkResponse<>(message, HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
    }
}
