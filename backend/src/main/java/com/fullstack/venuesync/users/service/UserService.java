package com.fullstack.venuesync.users.service;

public interface UserService {
    void upgradeUserToOrganizer(String userId, String email);
}
