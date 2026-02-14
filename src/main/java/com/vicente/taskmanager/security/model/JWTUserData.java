package com.vicente.taskmanager.security.model;

import com.vicente.taskmanager.model.entity.UserRole;

import java.util.Set;

public record JWTUserData(Long id, String email, Set<UserRole> roles){
}
