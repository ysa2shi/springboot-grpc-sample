package com.example.springbootgrpcsample.dto;

public record UserResponse(
        String id,
        String name,
        String profile
) {
}
