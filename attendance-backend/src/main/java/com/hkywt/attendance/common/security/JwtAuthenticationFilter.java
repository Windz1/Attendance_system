package com.hkywt.attendance.common.security;

import com.hkywt.attendance.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = Long.valueOf(String.valueOf(claims.get("userId")));
                @SuppressWarnings("unchecked")
                Set<String> roles = claims.get("roles") == null ? Collections.emptySet() : ((java.util.List<String>) claims.get("roles")).stream().collect(Collectors.toSet());
                Integer userType = claims.get("userType") == null ? null : Integer.valueOf(String.valueOf(claims.get("userType")));
                SecurityUser user = SecurityUser.builder()
                        .userId(userId)
                        .username(claims.getSubject())
                        .realName((String) claims.get("realName"))
                        .userType(userType)
                        .roles(roles)
                        .build();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
