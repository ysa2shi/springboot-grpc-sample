# gRPC PoC — REST API → gRPC 呼び出し検証

## 構成

```
curl localhost:8080/hello?name=world
  ↓ HTTP/JSON
HelloController                  （REST API サーバー: port 8080）
  ↓
HelloGrpcClientService           （gRPC クライアント層）
  ↓ gRPC/Protobuf (HTTP/2)
FakeHelloGrpcService             （Fake gRPC サーバー: port 9090）
  ↓
"Hello world"
```

**すべて同一 Spring Boot プロセスで動く。外部サーバー不要。**

---

## ファイル構成

```
src/
├── main/
│   ├── proto/
│   │   └── hello.proto                        ★ API スキーマ定義
│   ├── java/com/example/grpcpoc/
│   │   ├── GrpcPocApplication.java
│   │   ├── config/
│   │   │   └── GrpcClientConfig.java          ★ gRPC Channel / Stub の Bean 定義
│   │   ├── controller/
│   │   │   └── HelloController.java           ★ REST エンドポイント
│   │   ├── service/
│   │   │   └── HelloGrpcClientService.java    ★ gRPC クライアント呼び出し
│   │   └── grpc/
│   │       └── FakeHelloGrpcService.java      ★ Fake gRPC サーバー実装
│   └── resources/
│       └── application.yml
└── build/generated/source/proto/              ← gradle build で自動生成される
    ├── HelloRequest.java
    ├── HelloResponse.java
    └── HelloServiceGrpc.java                  ← Stub クラス（BlockingStub など）
```

---

## 起動手順

```bash
# 1. ビルド（Protobuf → Java コード生成も行われる）
./gradlew build

# 2. 起動
./gradlew bootRun

# 3. REST API を叩く
curl "localhost:8080/hello?name=world"
```

### 期待されるレスポンス

```json
{
  "message": "Hello world",
  "source": "via gRPC → FakeGrpcServer"
}
```

### ログで確認できること

```
[REST]       GET /hello?name=world
[gRPC Client] Calling SayHello: name=world
[FakeGrpcServer] SayHello called: name=world
[FakeGrpcServer] Response sent: Hello world
[gRPC Client] Response received: message=Hello world, serverId=FakeGrpcServer-001
[REST]       Response: {message=Hello world, source=via gRPC → FakeGrpcServer}
```

---

## 学習ポイント

### 1. proto → コード生成の流れ

```
hello.proto
  ↓ gradle build（protoc + grpc-java プラグイン）
HelloRequest.java        ← リクエスト DTO
HelloResponse.java       ← レスポンス DTO
HelloServiceGrpc.java    ← Stub クラス群（BlockingStub / FutureStub / AsyncStub）
```

### 2. Stub の種類

| Stub 種別 | 特徴 | 今回 |
|-----------|------|------|
| BlockingStub | 同期。レスポンスまでスレッドブロック | ✅ 使用 |
| FutureStub | 非同期（ListenableFuture） | - |
| AsyncStub | ストリーミング向け非同期 | - |

### 3. REST との違い

| 項目 | REST/JSON | gRPC/Protobuf |
|------|-----------|---------------|
| スキーマ定義 | OpenAPI（任意） | .proto（必須） |
| 通信フォーマット | テキスト（JSON） | バイナリ（Protobuf） |
| プロトコル | HTTP/1.1 | HTTP/2 |
| コード生成 | 任意 | 自動生成が前提 |
| エラー型 | HTTP Status Code | StatusRuntimeException |

---

## 次の拡張候補

### grpcurl で直接 gRPC サーバーを叩く
```bash
# インストール（Mac）
brew install grpcurl

# Fake サーバーに直接リクエスト（REST 経由なし）
grpcurl -plaintext \
  -d '{"name": "world"}' \
  localhost:9090 \
  hello.HelloService/SayHello
```

### Stream RPC
```proto
// server streaming: 1リクエスト → 複数レスポンス
rpc Subscribe (HelloRequest) returns (stream HelloResponse);
```

### Interceptor（ログ・認証）
```java
// クライアント側
stub.withInterceptors(new LoggingClientInterceptor())

// サーバー側
@GrpcService(interceptors = AuthServerInterceptor.class)
```

### gzip 比較（参照記事より）
- JSON ペイロードサイズ vs Protobuf バイナリサイズ
- さらに gzip 圧縮した場合の比較
