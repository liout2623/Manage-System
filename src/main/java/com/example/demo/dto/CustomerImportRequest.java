package com.example.demo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public class CustomerImportRequest {
    @NotEmpty
    @Valid
    private List<CustomerRequest> customers;

    public List<CustomerRequest> getCustomers() {
        return customers;
    }

    public void setCustomers(List<CustomerRequest> customers) {
        this.customers = customers;
    }
}
