package com.example.springbootgrpcsample.service;

import com.example.springbootgrpcsample.dto.UserResponse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class UserDataService {

    public List<UserResponse> getUsers() {

        String largeProfile =
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        .repeat(10);

        return IntStream.range(0, 10)
                .mapToObj(i -> new UserResponse(
                        "id-" + i,
                        "user-" + i,
                        largeProfile
                ))
                .toList();
    }
}