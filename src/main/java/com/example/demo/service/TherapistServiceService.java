package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.ServiceItem;
import com.example.demo.domain.TherapistService;
import com.example.demo.domain.UserAccount;
import com.example.demo.dto.TherapistServiceRequest;
import com.example.demo.dto.ServiceResponse;
import com.example.demo.mapper.ServiceItemMapper;
import com.example.demo.mapper.TherapistServiceMapper;
import com.example.demo.mapper.UserMapper;

@Service
public class TherapistServiceService {

    private final TherapistServiceMapper therapistServiceMapper;
    private final UserMapper userMapper;
    private final ServiceItemMapper serviceItemMapper;

    public TherapistServiceService(TherapistServiceMapper therapistServiceMapper,
                                   UserMapper userMapper,
                                   ServiceItemMapper serviceItemMapper) {
        this.therapistServiceMapper = therapistServiceMapper;
        this.userMapper = userMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    /**
     * 查询某理疗师负责的服务项目列表
     */
    public List<ServiceResponse> findByTherapistId(Long therapistId) {
        List<ServiceItem> services = therapistServiceMapper.findServicesByTherapistId(therapistId);
        return services.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 管理员为理疗师批量分配服务项目（先删后插）
     */
    @Transactional
    public List<ServiceResponse> assignServices(TherapistServiceRequest request) {
        Long therapistId = request.getTherapistId();

        // 校验理疗师角色必须为 STAFF
        UserAccount therapist = userMapper.findById(therapistId);
        if (therapist == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        if (!"STAFF".equals(therapist.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "理疗师必须是 STAFF 角色");
        }

        // 校验所有服务项目必须存在且 active=1
        for (Long serviceId : request.getServiceIds()) {
            ServiceItem item = serviceItemMapper.findById(serviceId);
            if (item == null || !Boolean.TRUE.equals(item.getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "服务项目不存在或已停用");
            }
        }

        // 先删除该理疗师的所有旧关联
        therapistServiceMapper.deleteByTherapistId(therapistId);

        // 批量插入新关联
        for (Long serviceId : request.getServiceIds()) {
            TherapistService ts = new TherapistService();
            ts.setTherapistId(therapistId);
            ts.setServiceId(serviceId);
            therapistServiceMapper.insert(ts);
        }

        // 返回最新的关联项目列表
        return findByTherapistId(therapistId);
    }

    /**
     * 移除某条关联
     */
    public void deleteById(Long id) {
        TherapistService existing = therapistServiceMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关联记录不存在");
        }
        therapistServiceMapper.deleteById(id);
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
}
