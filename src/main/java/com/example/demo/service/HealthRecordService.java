package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.HealthRecord;
import com.example.demo.domain.UserAccount;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.RecordRequest;
import com.example.demo.dto.RecordResponse;
import com.example.demo.mapper.CustomerMapper;
import com.example.demo.mapper.HealthRecordMapper;
import com.example.demo.mapper.UserMapper;

@Service
public class HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;

    public HealthRecordService(HealthRecordMapper healthRecordMapper,
                               CustomerMapper customerMapper,
                               UserMapper userMapper) {
        this.healthRecordMapper = healthRecordMapper;
        this.customerMapper = customerMapper;
        this.userMapper = userMapper;
    }

    public PageResponse<RecordResponse> listByCustomer(Long customerId, Integer page, Integer size) {
        ensureCustomerExists(customerId);

        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 10;
        int offset = (pageNum - 1) * pageSize;

        List<HealthRecord> records = healthRecordMapper.findByCustomerId(customerId, pageSize, offset);
        long total = healthRecordMapper.countByCustomerId(customerId);
        List<RecordResponse> items = records.stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(total, items);
    }

    public RecordResponse create(Long customerId, RecordRequest request, String username) {
        ensureCustomerExists(customerId);
        UserAccount currentUser = findCurrentUser(username);
        ensureUserActive(currentUser);
        ensureCanCreate(currentUser);

        HealthRecord record = new HealthRecord();
        record.setCustomerId(customerId);
        record.setAssessment(request.getAssessment());
        record.setRecommendation(request.getRecommendation());
        record.setRecordDate(request.getRecordDate() != null ? request.getRecordDate() : LocalDate.now());
        record.setCreatedBy(currentUser.getId());

        healthRecordMapper.insert(record);

        HealthRecord saved = healthRecordMapper.findByIdAndCustomerId(record.getId(), customerId);
        return toResponse(saved);
    }

    public RecordResponse update(Long customerId, Long recordId, RecordRequest request, String username) {
        ensureCustomerExists(customerId);
        HealthRecord existing = findRecord(customerId, recordId);
        UserAccount currentUser = findCurrentUser(username);
        ensureUserActive(currentUser);
        ensureCanOperate(existing, currentUser);

        existing.setAssessment(request.getAssessment());
        existing.setRecommendation(request.getRecommendation());
        existing.setRecordDate(request.getRecordDate() != null ? request.getRecordDate() : LocalDate.now());

        healthRecordMapper.update(existing);
        HealthRecord updated = healthRecordMapper.findByIdAndCustomerId(recordId, customerId);
        return toResponse(updated);
    }

    public void delete(Long customerId, Long recordId, String username) {
        ensureCustomerExists(customerId);
        HealthRecord existing = findRecord(customerId, recordId);
        UserAccount currentUser = findCurrentUser(username);
        ensureUserActive(currentUser);
        ensureCanOperate(existing, currentUser);
        healthRecordMapper.deleteById(recordId);
    }

    private void ensureCustomerExists(Long customerId) {
        if (customerMapper.findById(customerId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在");
        }
    }

    private HealthRecord findRecord(Long customerId, Long recordId) {
        HealthRecord record = healthRecordMapper.findByIdAndCustomerId(recordId, customerId);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "健康记录不存在");
        }
        return record;
    }

    private UserAccount findCurrentUser(String username) {
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前登录用户不存在");
        }
        return account;
    }

    private void ensureCanOperate(HealthRecord record, UserAccount currentUser) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isCreator = currentUser.getId() != null && currentUser.getId().equals(record.getCreatedBy());
        if (!isAdmin && !isCreator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限操作该健康记录");
        }
    }

    private void ensureCanCreate(UserAccount currentUser) {
        String role = currentUser.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"STAFF".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员或员工可创建健康记录");
        }
    }

    private void ensureUserActive(UserAccount currentUser) {
        if (currentUser.getActive() == null || !currentUser.getActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账户已被停用，无法操作");
        }
    }

    private RecordResponse toResponse(HealthRecord record) {
        RecordResponse response = new RecordResponse();
        response.setId(record.getId());
        response.setCustomerId(record.getCustomerId());
        response.setAssessment(record.getAssessment());
        response.setRecommendation(record.getRecommendation());
        response.setRecordDate(record.getRecordDate());
        response.setCreatedBy(record.getCreatedBy());
        response.setCreatedByName(record.getCreatedByName());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}
