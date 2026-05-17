package com.urbanride.component4;

import jakarta.persistence.*;
import com.urbanride.component1.User;

@Entity
@Table(name = "admins")
public class Admin extends User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_seq")
    @SequenceGenerator(name = "admin_seq", sequenceName = "admin_sequence", allocationSize = 1)
    private int userId;

    @Override
    public int getUserId() { return userId; }
    @Override
    public void setUserId(int id) { this.userId = id; }

    private String adminRole;
    private String lastLogin;
    private boolean isActive = true;

    public Admin() {}

    public Admin(int userId, String name, String email, String password, String mobile, String adminRole) {
        super(userId, name, email, password, mobile, "admin");
        this.adminRole = adminRole;
        this.isActive = true;
    }

    public String getAdminRole() { return adminRole; }
    public void setAdminRole(String role) { this.adminRole = role; }
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String date) { this.lastLogin = date; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    @Override
    public boolean login(String email, String password) {
        return getEmail().equals(email) && getPasswordHash().equals(hashPassword(password)) && isActive;
    }

    @Override
    public void displayProfile() {
        System.out.println("Admin: " + getName() + " | Role: " + adminRole + " | Active: " + isActive);
    }
}

