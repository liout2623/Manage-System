package com.example.demo.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.Appointment;

public interface AppointmentMapper {

    List<Appointment> findAll(@Param("therapistId") Long therapistId,
                              @Param("customerId") Long customerId,
                              @Param("date") LocalDate date,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("status") String status,
                              @Param("limit") int limit,
                              @Param("offset") int offset,
                              @Param("sortField") String sortField,
                              @Param("sortDirection") String sortDirection);

    long countAll(@Param("therapistId") Long therapistId,
                  @Param("customerId") Long customerId,
                  @Param("date") LocalDate date,
                  @Param("startDate") LocalDate startDate,
                  @Param("endDate") LocalDate endDate,
                  @Param("status") String status);

    Appointment findById(@Param("id") Long id);

    void insert(Appointment appointment);

    int update(Appointment appointment);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    /**
     * 冲突检测：查询指定理疗师在指定时间段内状态为 BOOKED/COMPLETED 的预约数量
     */
    long countConflict(@Param("therapistId") Long therapistId,
                       @Param("appointmentTime") LocalDateTime appointmentTime,
                       @Param("endTime") LocalDateTime endTime,
                       @Param("excludeId") Long excludeId);

    /**
     * 统计指定理疗师的未完成预约数量（用于删除前关联检查）
     */
    long countActiveByTherapistId(@Param("therapistId") Long therapistId);

    /**
     * 统计指定客户的未完成预约数量（用于删除前关联检查）
     */
    long countActiveByCustomerId(@Param("customerId") Long customerId);

    /**
     * 统计指定服务项目的未完成预约数量（用于删除前关联检查）
     */
    long countActiveByServiceId(@Param("serviceId") Long serviceId);
}
