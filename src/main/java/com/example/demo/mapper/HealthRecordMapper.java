package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.HealthRecord;

public interface HealthRecordMapper {
    List<HealthRecord> findByCustomerId(@Param("customerId") Long customerId,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    long countByCustomerId(@Param("customerId") Long customerId);

    HealthRecord findByIdAndCustomerId(@Param("recordId") Long recordId,
                                       @Param("customerId") Long customerId);

    void insert(HealthRecord record);

    int update(HealthRecord record);

    int deleteById(@Param("recordId") Long recordId);
}
