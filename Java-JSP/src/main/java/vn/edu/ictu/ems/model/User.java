package vn.edu.ictu.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String status; // active, inactive, locked
    private List<String> roles = new ArrayList<>();

    public User() {}

    public User(int id, String email, String name, String status, List<String> roles) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.status = status;
        if (roles != null) {
            this.roles = new ArrayList<>(roles);
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
