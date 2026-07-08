package com.club.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.JwtUtil;
import com.club.common.Result;
import com.club.entity.User;
import com.club.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 用户注册 */
    public Result<?> register(User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            return Result.fail("用户名和密码不能为空");
        }
        if (lambdaQuery().eq(User::getUsername, user.getUsername()).count() > 0) {
            return Result.fail("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("STUDENT");
        user.setStatus(1);
        save(user);
        return Result.ok("注册成功");
    }

    /** 用户登录：校验密码 + 签发JWT */
    public Result<?> login(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null || user.getStatus() == 0) {
            return Result.fail("用户不存在或已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail("密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("college", user.getCollege());
        data.put("phone", user.getPhone());
        data.put("createTime", user.getCreateTime());
        return Result.ok(data);
    }

    /** 获取用户列表（管理员功能） */
    public Result<?> listUsers(String keyword, Integer page, Integer size) {
        var wrapper = new LambdaQueryWrapper<User>();
        if (!StrUtil.isBlank(keyword)) {
            wrapper.like(User::getRealName, keyword).or().like(User::getUsername, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        var result = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        return Result.ok(result);
    }
}
