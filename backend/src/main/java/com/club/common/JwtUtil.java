package com.club.common;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * JWT工具类
 * 生成token、解析token、校验token，用于无状态用户认证
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /** 生成JWT token，payload包含userId和role供后续权限校验 */
    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 从token中解析用户ID */
    public Long getUserId(String token) {
        try {
            return Long.parseLong(
                Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject()
            );
        } catch (Exception e) {
            return null;
        }
    }

    /** 从token中解析用户角色 */
    public String getRole(String token) {
        try {
            return (String) Jwts.parser().setSigningKey(secret)
                .parseClaimsJws(token).getBody().get("role");
        } catch (Exception e) {
            return null;
        }
    }

    /** 校验token是否有效（未过期+签名正确） */
    public boolean validate(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
