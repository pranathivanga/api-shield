package com.api_shield.api_shield.user;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    private final UserRepository repository;

    public DataLoader(UserRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void loadData() {
        repository.save(new User("101", "FREE"));
        repository.save(new User("202", "PREMIUM"));
    }
}