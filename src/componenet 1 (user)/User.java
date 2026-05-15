package com.urbanride.component1;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)
    private int userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String mobile;
    private String role;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(int userId, String name, String email, String passwordHash, String mobile, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.mobile = mobile;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public abstract boolean login(String email, String password);
    public abstract void displayProfile();

    /** Simple hash for OOP demo â€” not production-grade */
    public static String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }
}

