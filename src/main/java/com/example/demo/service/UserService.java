package com.example.demo.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.UserAccount;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UserUpsertRequest;
import com.example.demo.mapper.UserMapper;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        ensureUsernameAvailable(request.getUsername());
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setRole("STAFF");
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);
        return toResponse(user);
    }

    public LoginResponse login(AuthRequest request) {
        UserAccount account = userMapper.findByUsername(request.getUsername());
        if (account == null || Boolean.FALSE.equals(account.getActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或已停用");
        }
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString();
        return new LoginResponse(token, toResponse(account));
    }

    public PageResponse<UserResponse> list(String keyword, String role, Boolean active, Integer page, Integer size) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        int offset = (pageNum - 1) * pageSize;
        List<UserAccount> users = userMapper.findAll(keyword, role, active, pageSize, offset);
        long total = userMapper.countAll(keyword, role, active);
        List<UserResponse> responses = users.stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(total, responses);
    }

    public UserResponse create(UserUpsertRequest request) {
        ensureUsernameAvailable(request.getUsername());
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setActive(request.getActive() != null ? request.getActive() : true);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }
        userMapper.insert(user);
        return toResponse(user);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    public UserResponse update(Long id, UserUpsertRequest req) {
        UserAccount ua = new UserAccount();
        ua.setId(id);
        ua.setUsername(req.getUsername());
        ua.setDisplayName(req.getDisplayName());
        ua.setRole(req.getRole());
        ua.setPhone(req.getPhone());
        ua.setActive(Boolean.TRUE.equals(req.getActive()));
        if (StringUtils.hasText(req.getPassword())) {
            ua.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        userMapper.update(ua);
        return toResponse(userMapper.findById(id));
    }

    public UserResponse findById(Long id) {
        UserAccount account = userMapper.findById(id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return toResponse(account);
    }

    private void ensureUsernameAvailable(String username) {
        UserAccount existing = userMapper.findByUsername(username);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
    }

    private UserResponse toResponse(UserAccount user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setDisplayName(user.getDisplayName());
        response.setPhone(user.getPhone());
        response.setActive(user.getActive());
        return response;
    }
}
