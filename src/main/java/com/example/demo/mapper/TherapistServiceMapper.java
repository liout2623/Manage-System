package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.ServiceItem;
import com.example.demo.domain.TherapistService;

public interface TherapistServiceMapper {

    List<ServiceItem> findServicesByTherapistId(@Param("therapistId") Long therapistId);

    List<TherapistService> findByTherapistId(@Param("therapistId") Long therapistId);

    TherapistService findById(@Param("id") Long id);

    void insert(TherapistService therapistService);

    void deleteByTherapistId(@Param("therapistId") Long therapistId);

    int deleteById(@Param("id") Long id);

    boolean existsByTherapistIdAndServiceId(@Param("therapistId") Long therapistId,
                                            @Param("serviceId") Long serviceId);
}
