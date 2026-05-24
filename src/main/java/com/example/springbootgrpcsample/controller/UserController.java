package com.example.springbootgrpcsample.controller;

import com.example.springbootgrpcsample.dto.UserResponse;
import com.example.springbootgrpcsample.grpc.client.UserGrpcClient;
import com.example.springbootgrpcsample.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserDataService userDataService;
    private final UserGrpcClient userGrpcClient;

    @GetMapping("/rest/users")
    public List<UserResponse> restUsers() {
        long start = System.currentTimeMillis();
        List<UserResponse> response = userDataService.getUsers();
        log.info("REST elapsed = {} ms", System.currentTimeMillis() - start);
        return response;
    }

    @GetMapping("/grpc/users")
    public List<UserResponse> grpcUsers() {
        long start = System.currentTimeMillis();
        List<UserResponse> response = userGrpcClient.getUsers();
        log.info("gRPC client elapsed = {} ms", System.currentTimeMillis() - start);
        return response;
    }
}