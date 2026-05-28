package com.huang.zhixing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.zhixing.common.JwtUtil;
import com.huang.zhixing.common.Result;
import com.huang.zhixing.mapper.BiUserMapper;
import com.huang.zhixing.model.entity.BiUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final BiUserMapper userMapper;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<?> register(@RequestParam String username, @RequestParam String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.error(400, "用户名和密码不能为空");
        }
        if (password.length() < 4) {
            return Result.error(400, "密码至少4位");
        }

        BiUser exist = userMapper.selectOne(
                new LambdaQueryWrapper<BiUser>().eq(BiUser::getUsername, username.trim()));
        if (exist != null) {
            return Result.error(400, "账号已存在");
        }

        BiUser user = new BiUser();
        user.setUsername(username.trim());
        user.setPassword(password);
        userMapper.insert(user);
        String token = jwtUtil.generateToken(user.getUsername());
        return Result.success(Map.of("username", user.getUsername(), "token", token));
    }

    @PostMapping("/login")
    public Result<?> login(@RequestParam String username, @RequestParam String password) {
        BiUser user = userMapper.selectOne(
                new LambdaQueryWrapper<BiUser>().eq(BiUser::getUsername, username.trim()));
        if (user == null) {
            return Result.error(400, "账号不存在");
        }
        if (!user.getPassword().equals(password)) {
            return Result.error(400, "密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return Result.success(Map.of("username", user.getUsername(), "token", token));
    }
}
