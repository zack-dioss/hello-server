package com.stu.helloserver.dto;

public class UserDTO {
    private String username;
    private String password;

    // 无参构造
    public UserDTO() {}

    // 全参构造（可选）
    public UserDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter 和 Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}