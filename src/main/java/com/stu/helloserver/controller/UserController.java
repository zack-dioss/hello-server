package com.stu.helloserver.controller;

import com.stu.helloserver.common.Result;
import com.stu.helloserver.dto.UserDTO;
import com.stu.helloserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 注册
    @PostMapping
    public Result<String> register(@RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    // 登录
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO);
    }

    // 根据 id 查询用户
    @GetMapping("/{id}")
    public Result<String> getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // 以下方法（PUT / DELETE）如果之前有且使用了旧 User 实体，可暂时注释，或者保持原样但需要调整
    // 为了不影响任务5的测试，建议注释
    /*
    @PutMapping("/{id}")
    public Result<String> updateUser(...) { ... }

    @DeleteMapping("/{id}")
    public Result<String> deleteUser(...) { ... }
    */
}