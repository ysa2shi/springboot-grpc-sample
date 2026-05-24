package com.example.springbootgrpcsample.grpc.client;

import com.example.grpcsample.proto.HelloRequest;
import com.example.grpcsample.proto.HelloResponse;
import com.example.grpcsample.proto.HelloServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelloGrpcClient {

    private final GrpcChannelFactory channelFactory;

    public String callHello(String name) {

        HelloServiceGrpc.HelloServiceBlockingStub stub =
                HelloServiceGrpc.newBlockingStub(
                        channelFactory.createChannel("local")
                );

        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .build();

        try {
            HelloResponse response = stub.sayHello(request);
            return response.getMessage();
        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed. status={}", ex.getStatus(), ex);
            throw ex;
        }
    }
}
