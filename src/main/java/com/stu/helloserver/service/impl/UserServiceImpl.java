package com.stu.helloserver.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stu.helloserver.common.Result;
import com.stu.helloserver.common.ResultCode;
import com.stu.helloserver.dto.UserDTO;
import com.stu.helloserver.entity.User;
import com.stu.helloserver.entity.UserInfo;
import com.stu.helloserver.mapper.UserInfoMapper;
import com.stu.helloserver.mapper.UserMapper;
import com.stu.helloserver.service.UserService;
import com.stu.helloserver.vo.UserDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_KEY_PREFIX = "user:detail:";

    // ==================== 注册 ====================
    @Override
    public Result<String> register(UserDTO userDTO) {
        // 1. 查询用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userDTO.getUsername());
        User dbUser = userMapper.selectOne(queryWrapper);
        if (dbUser != null) {
            return Result.error(ResultCode.USER_HAS_EXISTED);
        }
        // 2. 创建新用户
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        // 3. 插入数据库
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    // ==================== 登录 ====================
    @Override
    public Result<String> login(UserDTO userDTO) {
        // 1. 根据用户名查询
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userDTO.getUsername());
        User dbUser = userMapper.selectOne(queryWrapper);
        // 2. 用户不存在
        if (dbUser == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        // 3. 密码错误
        if (!dbUser.getPassword().equals(userDTO.getPassword())) {
            return Result.error(ResultCode.PASSWORD_ERROR);
        }
        return Result.success("登录成功");
    }

    // ==================== 根据ID查询用户（基础信息） ====================
    @Override
    public Result<String> getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        String data = "查询成功，用户ID：" + user.getId() + "，用户名：" + user.getUsername();
        return Result.success(data);
    }

    // ==================== 分页查询用户列表 ====================
    @Override
    public Result<Object> getUserPage(Integer pageNum, Integer pageSize) {
        Page<User> pageParam = new Page<>(pageNum, pageSize);
        Page<User> resultPage = userMapper.selectPage(pageParam, null);
        return Result.success(resultPage);
    }

    // ==================== 查询用户详情（多表联查 + Redis缓存） ====================
    @Override
    public Result<UserDetailVO> getUserDetail(Long userId) {
        String key = CACHE_KEY_PREFIX + userId;
        // 1. 先从缓存获取
        String json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            try {
                UserDetailVO vo = JSONUtil.toBean(json, UserDetailVO.class);
                return Result.success(vo);
            } catch (Exception e) {
                // 缓存数据异常，删除脏数据
                redisTemplate.delete(key);
            }
        }
        // 2. 缓存未命中，查询数据库
        UserDetailVO detail = userInfoMapper.getUserDetail(userId);
        if (detail == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        // 3. 写入缓存（10分钟过期）
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(detail), 10, TimeUnit.MINUTES);
        return Result.success(detail);
    }

    // ==================== 更新用户扩展信息（同时删除缓存） ====================
    @Override
    @Transactional
    public Result<String> updateUserInfo(Long userId, UserInfo userInfo) {
        // 1. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        // 2. 设置关联ID
        userInfo.setUserId(userId.intValue());
        // 3. 根据 user_id 查询扩展信息是否存在
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        UserInfo existing = userInfoMapper.selectOne(wrapper);
        if (existing != null) {
            // 更新已有记录
            existing.setRealName(userInfo.getRealName());
            existing.setPhone(userInfo.getPhone());
            existing.setAddress(userInfo.getAddress());
            userInfoMapper.updateById(existing);
        } else {
            // 插入新记录
            userInfoMapper.insert(userInfo);
        }
        // 4. 删除缓存
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
        return Result.success("更新用户扩展信息成功");
    }

    // ==================== 删除用户（同时删除扩展信息和缓存） ====================
    @Override
    @Transactional
    public Result<String> deleteUser(Long userId) {
        // 1. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        // 2. 删除扩展信息（如果存在）
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        userInfoMapper.delete(wrapper);
        // 3. 删除主表用户
        userMapper.deleteById(userId);
        // 4. 删除缓存
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
        return Result.success("删除用户成功");
    }
}