package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.domain.Customer;

public interface CustomerMapper {

    List<Customer> findAll(@Param("keyword") String keyword,
                           @Param("limit") int limit,
                           @Param("offset") int offset);

    long countAll(@Param("keyword") String keyword);

    Customer findById(Long id);

    void insert(Customer customer);

    int batchInsert(@Param("list") List<Customer> customers);

    int update(Customer customer);

    int deleteById(Long id);
}
