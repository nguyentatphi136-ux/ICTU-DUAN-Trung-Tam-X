package vn.edu.ictu.ems.model;

import java.util.List;

/**
 * Model User theo package ICTU (vn.edu.ictu.ems)
 * Tương thích và kế thừa toàn bộ từ com.ems.model.User
 */
public class User extends com.ems.model.User {
    private static final long serialVersionUID = 1L;

    public User() {
        super();
    }

    public User(Long id, String userCode, String email, String fullName, String primaryRole) {
        super(id, userCode, email, fullName, primaryRole);
    }

    public User(Long id, String fullName, String email, String status, List<String> roles) {
        super(id, fullName, email, status, roles);
    }

    public User(int id, String email, String fullName, String status, List<String> roles) {
        super((long) id, fullName, email, status, roles);
    }
}
