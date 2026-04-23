package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.Customer;

public interface CustomerMapper {

    List<Customer> findAll(@Param("keyword") String keyword,
                           @Param("limit") int limit,
                           @Param("offset") int offset,
                           @Param("sortField") String sortField,
                           @Param("sortDirection") String sortDirection);

    long countAll(@Param("keyword") String keyword);

    Customer findById(Long id);

    void insert(Customer customer);

    // 关键修复：@Param 名称必须与 XML 的 collection 一致（customers）
    int batchInsert(@Param("customers") List<Customer> customers);

    int update(Customer customer);

    int deleteById(Long id);
}