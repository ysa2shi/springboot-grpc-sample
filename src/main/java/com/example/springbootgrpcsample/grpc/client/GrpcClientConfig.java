package com.example.springbootgrpcsample.grpc.client;

import com.example.grpcsample.proto.HelloServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    HelloServiceGrpc.HelloServiceBlockingStub helloStub(
            GrpcChannelFactory channelFactory
    ) {
        return HelloServiceGrpc.newBlockingStub(
                channelFactory.createChannel("hello-service")
        );
    }
}
