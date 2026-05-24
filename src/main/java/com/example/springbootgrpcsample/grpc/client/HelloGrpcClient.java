package com.example.springbootgrpcsample.grpc.client;

import com.example.grpcsample.proto.HelloRequest;
import com.example.grpcsample.proto.HelloResponse;
import com.example.grpcsample.proto.HelloServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HelloGrpcClient {

    private final HelloServiceGrpc.HelloServiceBlockingStub stub;

    public String callHello(String name) {

        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .build();

        try {
            HelloResponse response = stub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .sayHello(request);

            return response.getMessage();
        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed. code={}, description={}",
                    ex.getStatus().getCode(),
                    ex.getStatus().getDescription(),
                    ex);
            switch (ex.getStatus().getCode()) {
                case DEADLINE_EXCEEDED ->
                        throw new RuntimeException("gRPC timeout", ex);
                case UNAVAILABLE ->
                        throw new RuntimeException("gRPC unavailable", ex);
                default ->
                        throw new RuntimeException("gRPC error", ex);
            }
        }
    }
}
