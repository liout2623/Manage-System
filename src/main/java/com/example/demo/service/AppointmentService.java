package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.Appointment;
import com.example.demo.domain.Customer;
import com.example.demo.domain.ServiceItem;
import com.example.demo.domain.UserAccount;
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.CustomerMapper;
import com.example.demo.mapper.ServiceItemMapper;
import com.example.demo.mapper.TherapistServiceMapper;
import com.example.demo.mapper.UserMapper;

@Service
public class AppointmentService {

    /** 营业开始时间 */
    private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
    /** 营业结束时间 */
    private static final LocalTime BUSINESS_END = LocalTime.of(21, 0);

    private final AppointmentMapper appointmentMapper;
    private final CustomerMapper customerMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final UserMapper userMapper;
    private final TherapistServiceMapper therapistServiceMapper;

    public AppointmentService(AppointmentMapper appointmentMapper,
                              CustomerMapper customerMapper,
                              ServiceItemMapper serviceItemMapper,
                              UserMapper userMapper,
                              TherapistServiceMapper therapistServiceMapper) {
        this.appointmentMapper = appointmentMapper;
        this.customerMapper = customerMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.userMapper = userMapper;
        this.therapistServiceMapper = therapistServiceMapper;
    }

    // ===================== 查询 =====================

    public PageResponse<AppointmentResponse> list(Long therapistId,
                                                   Long customerId,
                                                   LocalDate date,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   String status,
                                                   Integer page,
                                                   Integer size,
                                                   String sort) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        int offset = (pageNum - 1) * pageSize;

        String sortField = "appointment_time";
        String sortDirection = "ASC";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String field = parts[0].trim().toLowerCase();
            // 只允许排序的字段
            if ("appointment_time".equals(field) || "created_at".equals(field) || "id".equals(field)) {
                sortField = field;
            }
            if (parts.length > 1) {
                String dir = parts[1].trim().toUpperCase();
                if ("ASC".equals(dir) || "DESC".equals(dir)) {
                    sortDirection = dir;
                }
            }
        }

        List<Appointment> appointments = appointmentMapper.findAll(
                therapistId, customerId, date, startDate, endDate, status, pageSize, offset, sortField, sortDirection);
        long total = appointmentMapper.countAll(therapistId, customerId, date, startDate, endDate, status);

        List<AppointmentResponse> items = appointments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(total, items);
    }

    public AppointmentResponse findById(Long id) {
        Appointment appointment = appointmentMapper.findById(id);
        if (appointment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在");
        }
        return toResponse(appointment);
    }

    // ===================== 创建 =====================

    public AppointmentResponse create(AppointmentRequest request) {
        UserAccount currentUser = getCurrentUser();

        // 校验客户、服务项目是否存在
        Customer customer = customerMapper.findById(request.getCustomerId());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在");
        }
        ServiceItem serviceItem = serviceItemMapper.findById(request.getServiceId());
        if (serviceItem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }

        // 校验理疗师角色和状态
        UserAccount therapist = userMapper.findById(request.getTherapistId());
        validateTherapist(therapist);

        // 校验理疗师是否负责所选服务项目
        validateTherapistService(request.getTherapistId(), request.getServiceId());

        // STAFF 用户只能为自己创建预约
        ensureTherapistOwnership(currentUser, request.getTherapistId());

        // 计算结束时间
        int duration = serviceItem.getDurationMinutes() != null ? serviceItem.getDurationMinutes() : 60;
        LocalDateTime endTime = request.getAppointmentTime().plusMinutes(duration);

        // 校验营业时间
        validateBusinessHours(request.getAppointmentTime(), endTime);

        // 校验日期范围（今天至未来14天）
        validateAppointmentDateRange(request.getAppointmentTime());

        // 冲突检测
        ensureNoConflict(request.getTherapistId(), request.getAppointmentTime(), endTime, null);

        // 插入
        Appointment appointment = new Appointment();
        appointment.setCustomerId(request.getCustomerId());
        appointment.setServiceId(request.getServiceId());
        appointment.setTherapistId(request.getTherapistId());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setEndTime(endTime);
        appointment.setStatus("BOOKED");
        appointment.setNote(request.getNote());

        appointmentMapper.insert(appointment);

        // 重新查询以获取关联姓名
        Appointment saved = appointmentMapper.findById(appointment.getId());
        return toResponse(saved);
    }

    // ===================== 更新 =====================

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        UserAccount currentUser = getCurrentUser();
        Appointment existing = findOrThrow(id);

        // 已取消或已完成的预约不可再修改
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已取消的预约不可修改");
        }
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已完成的预约不可修改");
        }

        // STAFF 用户只能修改自己的预约
        ensureAppointmentOwnership(currentUser, existing);

        // 校验关联实体
        Customer customer = customerMapper.findById(request.getCustomerId());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在");
        }
        ServiceItem serviceItem = serviceItemMapper.findById(request.getServiceId());
        if (serviceItem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }
        UserAccount therapist = userMapper.findById(request.getTherapistId());
        validateTherapist(therapist);

        // 校验理疗师是否负责所选服务项目
        validateTherapistService(request.getTherapistId(), request.getServiceId());

        // STAFF 用户不能将预约转给其他理疗师
        ensureTherapistOwnership(currentUser, request.getTherapistId());

        // 重新计算结束时间
        int duration = serviceItem.getDurationMinutes() != null ? serviceItem.getDurationMinutes() : 60;
        LocalDateTime endTime = request.getAppointmentTime().plusMinutes(duration);

        // 校验营业时间
        validateBusinessHours(request.getAppointmentTime(), endTime);

        // 校验日期范围（今天至未来14天）
        validateAppointmentDateRange(request.getAppointmentTime());

        // 冲突检测（排除自身）
        ensureNoConflict(request.getTherapistId(), request.getAppointmentTime(), endTime, id);

        // 更新（显式保留状态不变）
        existing.setCustomerId(request.getCustomerId());
        existing.setServiceId(request.getServiceId());
        existing.setTherapistId(request.getTherapistId());
        existing.setAppointmentTime(request.getAppointmentTime());
        existing.setEndTime(endTime);
        existing.setNote(request.getNote());
        existing.setStatus(existing.getStatus());

        appointmentMapper.update(existing);

        Appointment updated = appointmentMapper.findById(id);
        return toResponse(updated);
    }

    // ===================== 状态变更 =====================

    public AppointmentResponse changeStatus(Long id, String newStatus) {
        UserAccount currentUser = getCurrentUser();
        Appointment existing = findOrThrow(id);

        // 只能从 BOOKED 变更为 COMPLETED 或 CANCELLED
        if (!"BOOKED".equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能变更已预约(BOOKED)状态的预约");
        }
        if (!"COMPLETED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能变更为COMPLETED或CANCELLED状态");
        }

        // STAFF 用户只能变更自己的预约
        ensureAppointmentOwnership(currentUser, existing);

        appointmentMapper.updateStatus(id, newStatus);

        Appointment updated = appointmentMapper.findById(id);
        return toResponse(updated);
    }

    // ===================== 删除 =====================

    public void delete(Long id) {
        findOrThrow(id);
        appointmentMapper.deleteById(id);
    }

    // ===================== 内部方法 =====================

    private Appointment findOrThrow(Long id) {
        Appointment appointment = appointmentMapper.findById(id);
        if (appointment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在");
        }
        return appointment;
    }

    private void ensureNoConflict(Long therapistId, LocalDateTime appointmentTime, LocalDateTime endTime, Long excludeId) {
        long count = appointmentMapper.countConflict(therapistId, appointmentTime, endTime, excludeId);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该理疗师此时间段已被占用");
        }
    }

    /**
     * 校验营业时间：预约时间必须在 08:00~21:00 之间，结束时间不能超过 21:00，不能预约过去的时间
     */
    private void validateBusinessHours(LocalDateTime appointmentTime, LocalDateTime endTime) {
        // 检查预约时间是否在过去
        if (appointmentTime.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约时间不能早于当前时间");
        }
        // 检查开始时间是否在营业时间内
        if (appointmentTime.toLocalTime().isBefore(BUSINESS_START)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约时间不能早于08:00");
        }
        // 检查结束时间是否超过营业时间
        if (endTime.toLocalTime().isAfter(BUSINESS_END)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约结束时间不能晚于21:00");
        }
    }

    /**
     * 校验预约日期范围：仅允许今天至未来14天内的日期
     */
    private void validateAppointmentDateRange(LocalDateTime appointmentTime) {
        LocalDate appointmentDate = appointmentTime.toLocalDate();
        LocalDate today = LocalDate.now();
        if (appointmentDate.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约日期不能早于今天");
        }
        if (appointmentDate.isAfter(today.plusDays(14))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约日期仅限今天起至未来14天内");
        }
    }

    /**
     * 校验理疗师：必须是 STAFF 角色且处于活跃状态
     */
    private void validateTherapist(UserAccount therapist) {
        if (therapist == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "理疗师不存在");
        }
        if (!Boolean.TRUE.equals(therapist.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定的理疗师已停用");
        }
        String role = therapist.getRole();
        if (!"STAFF".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "理疗师必须是 STAFF 角色");
        }
    }

    /**
     * 校验理疗师是否负责所选服务项目
     */
    private void validateTherapistService(Long therapistId, Long serviceId) {
        boolean exists = therapistServiceMapper.existsByTherapistIdAndServiceId(therapistId, serviceId);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该理疗师不负责所选服务项目");
        }
    }

    /**
     * STAFF 用户只能为指定为自己创建/修改预约，ADMIN 不受限
     */
    private void ensureTherapistOwnership(UserAccount currentUser, Long therapistId) {
        if (!"ADMIN".equals(currentUser.getRole()) && !currentUser.getId().equals(therapistId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您只能为自己创建预约");
        }
    }

    /**
     * STAFF 用户只能操作属于自己的预约，ADMIN 不受限
     */
    private void ensureAppointmentOwnership(UserAccount currentUser, Appointment appointment) {
        if (!"ADMIN".equals(currentUser.getRole()) && !currentUser.getId().equals(appointment.getTherapistId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您只能操作自己的预约");
        }
    }

    /**
     * 获取当前登录用户
     */
    private UserAccount getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        String username = auth.getName();
        UserAccount user = userMapper.findByUsername(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    private AppointmentResponse toResponse(Appointment a) {
        AppointmentResponse r = new AppointmentResponse();
        r.setId(a.getId());
        r.setCustomerId(a.getCustomerId());
        r.setServiceId(a.getServiceId());
        r.setTherapistId(a.getTherapistId());
        r.setAppointmentTime(a.getAppointmentTime());
        r.setEndTime(a.getEndTime());
        r.setStatus(a.getStatus());
        r.setNote(a.getNote());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        r.setCustomerName(a.getCustomerName());
        r.setServiceName(a.getServiceName());
        r.setTherapistName(a.getTherapistName());
        return r;
    }
}
