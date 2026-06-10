package com.stu.helloserver.vo;

import lombok.Data;

@Data  // 使用 Lombok 简化 getter/setter，如果不用可以手动生成
public class UserDetailVO {
    private Long userId;      // 对应 sys_user.id
    private String username;  // 用户名
    private String realName;  // 真实姓名
    private String phone;     // 手机号
    private String address;   // 地址
}