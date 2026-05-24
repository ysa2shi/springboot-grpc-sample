package com.example.springbootgrpcsample.controller;


import com.example.springbootgrpcsample.grpc.client.HelloGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HelloController {

    private final HelloGrpcClient helloGrpcClient;

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "world") String name) {
        return helloGrpcClient.callHello(name);
    }
}
