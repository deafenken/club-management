package com.club.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.club.common.Result;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 — 登录注册（无需JWT拦截，SecurityConfig中已放行）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClubMemberMapper clubMemberMapper;
    private final ActivityEnrollMapper enrollMapper;
    private final ResourceBorrowMapper borrowMapper;
    private final VenueBookingMapper bookingMapper;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        return userService.login(body.get("username"), body.get("password"));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        return userService.register(user);
    }

    /** 获取当前登录用户信息（从JWT中提取身份，防止越权查询） */
    @GetMapping("/info")
    public Result<?> getUserInfo() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long)) return Result.fail(401, "未登录");
        User user = userService.getById((Long) principal);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(null); // 脱敏
        return Result.ok(user);
    }

    /** 获取当前用户统计概览（社团数/活动数/物资借用数/场地预约数） */
    @GetMapping("/stats")
    public Result<?> getUserStats() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long)) return Result.fail(401, "未登录");
        Long userId = (Long) principal;

        Map<String, Object> stats = new HashMap<>();
        stats.put("clubCount", clubMemberMapper.selectCount(
            new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getUserId, userId)
                .eq(ClubMember::getStatus, 1)));
        stats.put("activityCount", enrollMapper.selectCount(
            new LambdaQueryWrapper<ActivityEnroll>()
                .eq(ActivityEnroll::getUserId, userId)));
        stats.put("borrowCount", borrowMapper.selectCount(
            new LambdaQueryWrapper<ResourceBorrow>()
                .eq(ResourceBorrow::getUserId, userId)));
        stats.put("bookingCount", bookingMapper.selectCount(
            new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getBookingUserId, userId)));
        return Result.ok(stats);
    }
}
