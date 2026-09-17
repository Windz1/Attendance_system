package com.hkywt.attendance.common.security;

import com.hkywt.attendance.common.util.JwtUtil;
import com.hkywt.attendance.module.system.entity.SysUser;
import com.hkywt.attendance.module.system.mapper.SysRoleMapper;
import com.hkywt.attendance.module.system.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, SysUserMapper userMapper, SysRoleMapper roleMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = Long.valueOf(String.valueOf(claims.get("userId")));
                SysUser dbUser = userMapper.selectById(userId);
                if (dbUser == null || Integer.valueOf(1).equals(dbUser.getDeleted()) || !Integer.valueOf(1).equals(dbUser.getStatus())) {
                    throw new IllegalArgumentException("inactive user");
                }
                if (!dbUser.getUsername().equals(claims.getSubject())) {
                    throw new IllegalArgumentException("token subject mismatch");
                }
                List<String> roleList = roleMapper.findRoleCodesByUserId(userId);
                Set<String> roles = roleList.stream().collect(Collectors.toSet());
                SecurityUser user = SecurityUser.builder()
                        .userId(userId)
                        .username(dbUser.getUsername())
                        .realName(dbUser.getRealName())
                        .userType(dbUser.getUserType())
                        .roles(roles)
                        .build();
                List<SimpleGrantedAuthority> authorities = roleList.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
