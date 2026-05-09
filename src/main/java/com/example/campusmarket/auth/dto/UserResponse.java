package com.example.campusmarket.auth.dto;

public record UserResponse(
    String uid,
    String email,
    String name,
    String university,
    Long createdAt
) {}
