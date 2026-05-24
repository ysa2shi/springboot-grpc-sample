package com.example.springbootgrpcsample.grpc.client;

import com.example.grpcsample.proto.GetUsersRequest;
import com.example.grpcsample.proto.GetUsersResponse;
import com.example.grpcsample.proto.UserServiceGrpc;
import com.example.springbootgrpcsample.dto.UserResponse;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserGrpcClient {

    private final UserServiceGrpc.UserServiceBlockingStub stub;

    public List<UserResponse> getUsers() {
        GetUsersRequest request = GetUsersRequest.newBuilder().build();

        try {
            GetUsersResponse response = stub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .getUsers(request);

            return response.getUsersList().stream()
                    .map(r -> new UserResponse(r.getId(), r.getName(), r.getProfile()))
                    .toList();

        } catch (StatusRuntimeException ex) {
            log.error("gRPC getUsers failed. code={}, description={}",
                    ex.getStatus().getCode(),
                    ex.getStatus().getDescription(),
                    ex);
            throw switch (ex.getStatus().getCode()) {
                case DEADLINE_EXCEEDED -> new RuntimeException("gRPC timeout", ex);
                case UNAVAILABLE      -> new RuntimeException("gRPC unavailable", ex);
                default               -> new RuntimeException("gRPC error", ex);
            };
        }
    }
}
