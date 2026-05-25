# gRPC PoC — REST JSON / REST GZIP / gRPC 比較検証

同一 Spring Boot プロセス内で **REST JSON・REST GZIP・gRPC** の 3 方式を実装し、  
User データ（10 件 × 500 文字 profile）を題材にペイロードサイズ・レスポンス時間を比較する PoC。

---

## 検証エンドポイント

| 方式 | エンドポイント | ポート | 備考 |
|------|--------------|--------|------|
| REST JSON | `GET /rest/users` | 8080 | 圧縮なし |
| REST GZIP | `GET /rest/users` | 8081 | `--spring.profiles.active=gzip` で起動 |
| gRPC | `GET /grpc/users` | 8080 | REST → gRPC (9090) → REST |

---

## アーキテクチャ

```
【REST JSON / REST GZIP】

curl localhost:8080/rest/users   (または port 8081 with gzip profile)
  ↓
UserController
  ↓
UserDataService  ← 1000件のダミーユーザーを生成
  ↓
JSON レスポンス（gzip プロファイル時は自動圧縮）


【gRPC 経由】

curl localhost:8080/grpc/users
  ↓
UserController
  ↓
UserGrpcClient  (BlockingStub)
  ↓ gRPC/Protobuf (HTTP/2, port 9090)
UserGrpcService (@GrpcService)
  ↓
UserDataService  ← 同じデータ生成ロジックを再利用
  ↓
JSON レスポンス（UserRecord → UserResponse に変換して返却）
```

**すべて同一 Spring Boot プロセスで動く。外部サーバー不要。**

---

## ファイル構成

```
src/
├── main/
│   ├── proto/
│   │   ├── hello.proto                   ★ Hello サービス定義
│   │   └── user.proto                    ★ User サービス定義（比較検証用）
│   ├── java/com/example/springbootgrpcsample/
│   │   ├── SpringbootGrpcSampleApplication.java
│   │   ├── controller/
│   │   │   ├── HelloController.java      GET /hello → gRPC 呼び出し
│   │   │   └── UserController.java       GET /rest/users, GET /grpc/users
│   │   ├── service/
│   │   │   └── UserDataService.java      ★ ダミーユーザーデータ生成（10件 × 500文字 profile）
│   │   ├── dto/
│   │   │   └── UserResponse.java         record { id, name, profile }
│   │   └── grpc/
│   │       ├── client/
│   │       │   ├── GrpcClientConfig.java ★ BlockingStub の Bean 定義
│   │       │   ├── HelloGrpcClient.java  Hello gRPC クライアント
│   │       │   └── UserGrpcClient.java   ★ User gRPC クライアント（比較検証用）
│   │       └── server/
│   │           ├── FakeHelloGrpc.java    Hello gRPC サーバー実装
│   │           └── UserGrpcService.java  ★ User gRPC サーバー実装（比較検証用）
│   └── resources/
│       ├── application.yaml              REST:8080, gRPC:9090, 圧縮なし
│       └── application-gzip.yml         REST:8081, gzip 圧縮有効
└── build/generated/source/proto/        ← gradle build で自動生成
    ├── HelloServiceGrpc.java
    └── UserServiceGrpc.java
```

---

## 起動・検証手順

### 1. ビルド

```bash
./gradlew build
```

### 2. REST JSON / gRPC 比較

```bash
# 起動
./gradlew bootRun

# REST JSON（圧縮なし, port 8080）
curl -w "\nTime: %{time_total}s, Size: %{size_download} bytes\n" \
     http://localhost:8080/rest/users -o /dev/null

# gRPC 経由（REST → gRPC port 9090 → REST, port 8080）
curl -w "\nTime: %{time_total}s, Size: %{size_download} bytes\n" \
     http://localhost:8080/grpc/users -o /dev/null
```

### 3. REST GZIP 比較

```bash
# 別ターミナルで gzip プロファイルを指定して起動
./gradlew bootRun --args='--spring.profiles.active=gzip'

# REST GZIP（圧縮あり, port 8081）
curl -H "Accept-Encoding: gzip" \
     -w "\nTime: %{time_total}s, Size: %{size_download} bytes\n" \
     http://localhost:8081/rest/users -o /dev/null
```

### 4. grpcurl で gRPC サーバーを直接叩く

REST 経由を介さず、gRPC サーバー（port 9090）に直接リクエストする。

```bash
# 登録済みサービス一覧を確認
grpcurl -plaintext localhost:9090 list

# User サービスのメソッド一覧
grpcurl -plaintext localhost:9090 list user.UserService

# UserService/GetUsers を直接呼び出し（Protobuf → JSON で表示）
grpcurl -plaintext localhost:9090 user.UserService/GetUsers

# Hello サービス
grpcurl -plaintext -d '{"name": "world"}' \
  localhost:9090 hello.HelloService/SayHello
```

期待されるレスポンス（先頭 2 件のみ抜粋）:

```json
{
  "users": [
    { "id": "id-0", "name": "user-0", "profile": "aaa..." },
    { "id": "id-1", "name": "user-1", "profile": "aaa..." },
    ...
  ]
}
```

### 5. ログで確認できること

各リクエストでサーバーログにレスポンス時間が出力される。

```
# REST
INFO UserController : REST elapsed = 12 ms

# gRPC（サーバー側 + クライアント側の両方）
INFO UserGrpcService  : gRPC server getUsers elapsed = 8 ms
INFO UserController   : gRPC client elapsed = 45 ms
```

クライアント elapsed とサーバー elapsed の差が **シリアライズ・ネットワークコスト** に相当する。

---

## 技術ポイント

### proto → コード生成の流れ

```
user.proto
  ↓ gradle build（protoc + grpc-java プラグイン）
UserRecord.java          ← メッセージ DTO
GetUsersRequest.java
GetUsersResponse.java
UserServiceGrpc.java     ← Stub クラス群（BlockingStub / FutureStub / AsyncStub）
```

### Stub の種類

| Stub 種別 | 特徴 | 本 PoC |
|-----------|------|--------|
| BlockingStub | 同期。レスポンスまでスレッドブロック | ✅ 使用 |
| FutureStub | 非同期（ListenableFuture） | - |
| AsyncStub | ストリーミング向け非同期 | - |

### 3 方式の比較ポイント

| 項目 | REST JSON | REST GZIP | gRPC |
|------|-----------|-----------|------|
| プロトコル | HTTP/1.1 | HTTP/1.1 | HTTP/2 |
| フォーマット | テキスト（JSON） | テキスト（JSON + 圧縮） | バイナリ（Protobuf） |
| スキーマ定義 | 任意（OpenAPI等） | 任意 | .proto（必須） |
| ペイロードサイズ | 大 | 小（gzip効果） | 小（バイナリ） |
| コード生成 | 任意 | 任意 | 自動生成が前提 |
| エラー型 | HTTP Status Code | HTTP Status Code | StatusRuntimeException |

---

## grpcurl インストール

```bash
# Mac
brew install grpcurl
```
