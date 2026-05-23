package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.ServiceItem;

public interface ServiceItemMapper {
    List<ServiceItem> findAll(@Param("keyword") String keyword,
                              @Param("active") Boolean active,
                              @Param("therapistId") Long therapistId,
                              @Param("limit") int limit,
                              @Param("offset") int offset,
                              @Param("sortField") String sortField,
                              @Param("sortDirection") String sortDirection);

    long countAll(@Param("keyword") String keyword,
                  @Param("active") Boolean active,
                  @Param("therapistId") Long therapistId);

    ServiceItem findById(Long id);

    void insert(ServiceItem serviceItem);

    int update(ServiceItem serviceItem);

    int deleteById(Long id);
}