package com.example.demo.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.ServiceItem;
import com.example.demo.domain.UserAccount;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.ServiceRequest;
import com.example.demo.dto.ServiceResponse;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.ServiceItemMapper;
import com.example.demo.mapper.UserMapper;

@Service
public class ServiceItemService {

    private static final Set<String> SERVICE_SORT_FIELD_WHITELIST = Set.of(
            "id", "name", "price", "duration_minutes", "created_at"
    );

    private final ServiceItemMapper serviceItemMapper;
    private final AppointmentMapper appointmentMapper;
    private final UserMapper userMapper;

    public ServiceItemService(ServiceItemMapper serviceItemMapper, AppointmentMapper appointmentMapper, UserMapper userMapper) {
        this.serviceItemMapper = serviceItemMapper;
        this.appointmentMapper = appointmentMapper;
        this.userMapper = userMapper;
    }

    public PageResponse<ServiceResponse> list(String keyword, Integer page, Integer size, String sort, Boolean active) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        int offset = (pageNum - 1) * pageSize;

        String sortField = "id";
        String sortDirection = "desc";
        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                sortField = parts[0].trim().toLowerCase();
            }
            if (parts.length >= 2) {
                sortDirection = parts[1].trim().toLowerCase();
            }
        }

        if (!SERVICE_SORT_FIELD_WHITELIST.contains(sortField)) {
            sortField = "id";
        }
        if (!"asc".equals(sortDirection) && !"desc".equals(sortDirection)) {
            sortDirection = "desc";
        }

        // 根据当前登录用户角色决定 therapistId 过滤
        Long therapistId = null;
        UserAccount currentUser = getCurrentUser();
        if (currentUser != null && "STAFF".equals(currentUser.getRole())) {
            therapistId = currentUser.getId();
        }

        List<ServiceItem> services = serviceItemMapper.findAll(keyword, active, therapistId, pageSize, offset, sortField, sortDirection);
        long total = serviceItemMapper.countAll(keyword, active, therapistId);
        List<ServiceResponse> items = services.stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(total, items);
    }

    public ServiceResponse findById(Long id) {
        ServiceItem item = serviceItemMapper.findById(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }
        return toResponse(item);
    }

    public ServiceResponse create(ServiceRequest request) {
        ServiceItem item = new ServiceItem();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setDurationMinutes(request.getDurationMinutes());
        item.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        serviceItemMapper.insert(item);
        return toResponse(serviceItemMapper.findById(item.getId()));
    }

    public ServiceResponse update(Long id, ServiceRequest request) {
        ServiceItem existing = serviceItemMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }

        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目名称不能为空");
        }

        existing.setName(request.getName() != null ? request.getName() : existing.getName());
        existing.setDescription(request.getDescription() != null ? request.getDescription() : existing.getDescription());
        existing.setPrice(request.getPrice() != null ? request.getPrice() : existing.getPrice());
        existing.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : existing.getDurationMinutes());
        existing.setActive(request.getActive() != null ? request.getActive() : existing.getActive());

        serviceItemMapper.update(existing);
        return toResponse(serviceItemMapper.findById(id));
    }

    public void delete(Long id) {
        ServiceItem existing = serviceItemMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }
        long activeAppointments = appointmentMapper.countActiveByServiceId(id);
        if (activeAppointments > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "该服务项目有 " + activeAppointments + " 条未完成预约，无法删除");
        }
        serviceItemMapper.deleteById(id);
    }

    public ServiceResponse toggle(Long id) {
        ServiceItem existing = serviceItemMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }
        existing.setActive(!Boolean.TRUE.equals(existing.getActive()));
        serviceItemMapper.update(existing);
        return toResponse(serviceItemMapper.findById(id));
    }

    private ServiceResponse toResponse(ServiceItem item) {
        ServiceResponse response = new ServiceResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setDurationMinutes(item.getDurationMinutes());
        response.setActive(item.getActive());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }

    /**
     * 获取当前登录用户（未登录或匿名用户返回 null）
     */
    private UserAccount getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String username = auth.getName();
        return userMapper.findByUsername(username);
    }
}