package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;
import com.example.demo.security.JwtService;

import org.springframework.dao.DuplicateKeyException;
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
    private final JwtService jwtService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        UserAccount user = buildNewUser(
                request.getUsername(),
                request.getDisplayName(),
                request.getPassword(),
                "STAFF",
                request.getPhone(),
                request.getOccupation(),
                true
        );
            insertNewUser(user, "注册失败，该用户名已存在");
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
        String token = jwtService.generateToken(account.getId(), account.getUsername(), account.getRole());
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
        UserAccount user = buildNewUser(
                request.getUsername(),
                request.getDisplayName(),
                request.getPassword(),
                request.getRole(),
                request.getPhone(),
                request.getOccupation(),
                request.getActive() != null ? request.getActive() : true
        );
        insertNewUser(user, "用户名已存在");
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
        ua.setOccupation(req.getOccupation());
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

    private void ensureUsernameAvailable(String username, String duplicateMessage) {
        UserAccount existing = userMapper.findByUsername(username);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, duplicateMessage);
        }
    }

    private UserAccount buildNewUser(String username,
                                     String displayName,
                                     String rawPassword,
                                     String role,
                                     String phone,
                                     String occupation,
                                     boolean active) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setPhone(phone);
        user.setOccupation(occupation);
        user.setActive(active);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    private void insertNewUser(UserAccount user, String duplicateMessage) {
        ensureUsernameAvailable(user.getUsername(), duplicateMessage);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, duplicateMessage);
        }
    }

    private UserResponse toResponse(UserAccount user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setDisplayName(user.getDisplayName());
        response.setPhone(user.getPhone());
        response.setOccupation(user.getOccupation());
        response.setActive(user.getActive());
        return response;
    }
}
