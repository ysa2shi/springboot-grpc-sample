package com.example.springbootgrpcsample.grpc.client;

import com.example.grpcsample.proto.HelloRequest;
import com.example.grpcsample.proto.HelloResponse;
import com.example.grpcsample.proto.HelloServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

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

        HelloResponse response = stub.sayHello(request);

        return response.getMessage();
    }
}
