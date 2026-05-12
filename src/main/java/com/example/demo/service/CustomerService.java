package com.example.demo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.domain.Customer;
import com.example.demo.dto.CustomerExportRow;
import com.example.demo.dto.CustomerImportRequest;
import com.example.demo.dto.CustomerRequest;
import com.example.demo.dto.CustomerResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.mapper.CustomerMapper;

@Service
public class CustomerService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_EXPORT_LIMIT = 10_000;
    private static final Set<String> CUSTOMER_SORT_FIELD_WHITELIST = Set.of(
            "id", "name", "phone", "email", "gender", "tags", "birthday", "created_at", "updated_at"
    );

    private final CustomerMapper customerMapper;
    private final AppointmentMapper appointmentMapper;

    public CustomerService(CustomerMapper customerMapper, AppointmentMapper appointmentMapper) {
        this.customerMapper = customerMapper;
        this.appointmentMapper = appointmentMapper;
    }

    public PageResponse<CustomerResponse> list(String keyword, Integer page, Integer size, String sort) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        int offset = (pageNum - 1) * pageSize;

        // 解析排序参数，格式: "field,asc" 或 "field,desc"
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

        if (!CUSTOMER_SORT_FIELD_WHITELIST.contains(sortField)) {
            sortField = "id";
        }
        if (!"asc".equals(sortDirection) && !"desc".equals(sortDirection)) {
            sortDirection = "desc";
        }

        List<Customer> customers = customerMapper.findAll(keyword, pageSize, offset, sortField, sortDirection);
        long total = customerMapper.countAll(keyword);
        List<CustomerResponse> responses = customers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(total, responses);
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = toEntity(new Customer(), request);
        customerMapper.insert(customer);
        return toResponse(customer);
    }

    public void delete(Long id) {
        Customer customer = customerMapper.findById(id);
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在");
        }
        long activeAppointments = appointmentMapper.countActiveByCustomerId(id);
        if (activeAppointments > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "该客户有 " + activeAppointments + " 条未完成预约，无法删除");
        }
        customerMapper.deleteById(id);
    }

    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer existing = customerMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在");
        }

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

        Customer updated = customerMapper.findById(id);
        return toResponse(updated);
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

    public List<CustomerExportRow> listForExport(String keyword) {
        List<Customer> customers = customerMapper.findAll(keyword, MAX_EXPORT_LIMIT, 0, "id", "asc");
        return customers.stream()
                .map(this::toExportRow)
                .collect(Collectors.toList());
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

    private CustomerExportRow toExportRow(Customer customer) {
        CustomerExportRow row = new CustomerExportRow();
        row.setId(customer.getId());
        row.setName(customer.getName());
        row.setPhone(customer.getPhone());
        row.setEmail(customer.getEmail());
        row.setGender(customer.getGender());
        row.setTags(customer.getTags());
        row.setNote(customer.getNote());
        row.setBirthday(customer.getBirthday() == null ? null : customer.getBirthday().format(DATE_FORMATTER));
        row.setCreatedAt(customer.getCreatedAt() == null ? null : customer.getCreatedAt().format(DATETIME_FORMATTER));
        row.setUpdatedAt(customer.getUpdatedAt() == null ? null : customer.getUpdatedAt().format(DATETIME_FORMATTER));
        return row;
    }
}