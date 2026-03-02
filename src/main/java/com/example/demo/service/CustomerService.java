package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.Customer;
import com.example.demo.dto.CustomerImportRequest;
import com.example.demo.dto.CustomerRequest;
import com.example.demo.dto.CustomerResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.mapper.CustomerMapper;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public PageResponse<CustomerResponse> list(String keyword, Integer page, Integer size) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        int offset = (pageNum - 1) * pageSize;
        List<Customer> customers = customerMapper.findAll(keyword, pageSize, offset);
        long total = customerMapper.countAll(keyword);
        List<CustomerResponse> responses = customers.stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(total, responses);
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = toEntity(new Customer(), request);
        customerMapper.insert(customer);
        return toResponse(customer);
    }

    public void delete(Long id) {
        customerMapper.deleteById(id);
    }

    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = new Customer();
        c.setId(id);
        c.setName(req.getName());
        c.setPhone(req.getPhone());
        c.setEmail(req.getEmail());
        c.setGender(req.getGender());
        c.setTags(req.getTags());
        c.setNote(req.getNote());
        c.setBirthday(req.getBirthday());
        customerMapper.update(c);
        return toResponse(customerMapper.findById(id));
    }

    public CustomerResponse findById(Long id) {
        Customer customer = customerMapper.findById(id);
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在");
        }
        return toResponse(customer);
    }

    public int importBatch(CustomerImportRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getCustomers())) {
            return 0;
        }
        List<Customer> customers = request.getCustomers().stream()
                .map(req -> toEntity(new Customer(), req))
                .collect(Collectors.toList());
        return customerMapper.batchInsert(customers);
    }

    private Customer toEntity(Customer target, CustomerRequest request) {
        target.setName(request.getName());
        target.setPhone(request.getPhone());
        target.setEmail(request.getEmail());
        target.setGender(request.getGender());
        target.setTags(request.getTags());
        target.setNote(request.getNote());
        target.setBirthday(request.getBirthday());
        return target;
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setPhone(customer.getPhone());
        response.setEmail(customer.getEmail());
        response.setGender(customer.getGender());
        response.setTags(customer.getTags());
        response.setNote(customer.getNote());
        response.setBirthday(customer.getBirthday());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());
        return response;
    }
}
