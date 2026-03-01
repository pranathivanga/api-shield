package com.api_shield.api_shield.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")   // ✅ FIX HERE
public class User {

    @Id
    private String id;

    private String tier;

    public User() {}

    public User(String id, String tier) {
        this.id = id;
        this.tier = tier;
    }

    public String getId() {
        return id;
    }

    public String getTier() {
        return tier;
    }
}