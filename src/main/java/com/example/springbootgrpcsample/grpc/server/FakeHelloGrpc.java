package com.example.springbootgrpcsample.grpc.server;

import com.example.grpcsample.proto.HelloRequest;
import com.example.grpcsample.proto.HelloResponse;
import com.example.grpcsample.proto.HelloServiceGrpc;

import io.grpc.stub.StreamObserver;

import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class FakeHelloGrpc extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(
            HelloRequest request,
            StreamObserver<HelloResponse> responseObserver
    ) {

        String name = request.getName();

        HelloResponse response = HelloResponse.newBuilder()
                .setMessage("Hello " + name)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}