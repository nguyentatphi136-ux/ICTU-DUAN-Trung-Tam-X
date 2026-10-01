package com.ems.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Mục 11: MVC Model - Entity User đại diện dữ liệu người dùng
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userCode;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String status;
    private String primaryRole;
    private List<String> roles;
    private Date lastLoginAt;

    public User() {
    }

    public User(Long id, String userCode, String email, String fullName, String primaryRole) {
        this.id = id;
        this.userCode = userCode;
        this.email = email;
        this.fullName = fullName;
        this.primaryRole = primaryRole;
        this.status = "ACTIVE";
    }

    public User(Long id, String fullName, String email, String status, List<String> roles) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.roles = roles;
        if (roles != null && !roles.isEmpty()) {
            this.primaryRole = roles.get(0);
        }
    }

    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(String primaryRole) {
        this.primaryRole = primaryRole;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
