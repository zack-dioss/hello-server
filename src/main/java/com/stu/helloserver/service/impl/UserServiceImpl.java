package com.stu.helloserver.service.impl;

import com.stu.helloserver.common.Result;
import com.stu.helloserver.common.ResultCode;
import com.stu.helloserver.dto.UserDTO;
import com.stu.helloserver.service.UserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    // 模拟数据库：key=用户名, value=密码
    private static final Map<String, String> userDb = new HashMap<>();

    @Override
    public Result<String> register(UserDTO userDTO) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();

        // 校验用户名是否已存在
        if (userDb.containsKey(username)) {
            return Result.error(ResultCode.USER_HAS_EXISTED);
        }
        // 存入模拟数据库
        userDb.put(username, password);
        return Result.success("注册成功");
    }

    @Override
    public Result<String> login(UserDTO userDTO) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();

        // 校验用户是否存在
        if (!userDb.containsKey(username)) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        // 校验密码是否正确
        String dbPassword = userDb.get(username);
        if (!dbPassword.equals(password)) {
            return Result.error(ResultCode.PASSWORD_ERROR);
        }
        // 登录成功（任务4未要求生成Token，只返回成功消息）
        return Result.success("登录成功");
    }
}