package com.example.springbootgrpcsample.grpc.server;

import com.example.grpcsample.proto.GetUsersRequest;
import com.example.grpcsample.proto.GetUsersResponse;
import com.example.grpcsample.proto.UserRecord;
import com.example.grpcsample.proto.UserServiceGrpc;
import com.example.springbootgrpcsample.service.UserDataService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserDataService userDataService;

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
        long start = System.currentTimeMillis();

        List<UserRecord> records = userDataService.getUsers().stream()
                .map(u -> UserRecord.newBuilder()
                        .setId(u.id())
                        .setName(u.name())
                        .setProfile(u.profile())
                        .build())
                .toList();

        GetUsersResponse response = GetUsersResponse.newBuilder()
                .addAllUsers(records)
                .build();

        log.info("gRPC server getUsers elapsed = {} ms", System.currentTimeMillis() - start);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
