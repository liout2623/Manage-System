package com.example.demo.dto;

import com.alibaba.excel.annotation.ExcelProperty;

public class CustomerExportRow {

    @ExcelProperty("客户ID")
    private Long id;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("标签")
    private String tags;

    @ExcelProperty("备注")
    private String note;

    @ExcelProperty("生日")
    private String birthday;

    @ExcelProperty("创建时间")
    private String createdAt;

    @ExcelProperty("更新时间")
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}