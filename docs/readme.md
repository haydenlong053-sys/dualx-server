# dualx-server

`dualx-server` 是一个基于 Spring Boot 3 的后端接口项目，当前保留用户、资产、配置、文件上传、Redis、接口文档、Sa-Token 鉴权和安全请求能力。

## 技术栈

- JDK 21
- Spring Boot 3.3.13
- Maven 多模块
- MyBatis Plus 3.5.9
- MySQL 8 Connector
- Redis / Spring Data Redis
- Sa-Token 1.45.0
- Sentinel / Spring Cloud Alibaba Sentinel 2023.0.3.3
- Knife4j 4.5.0
- Lombok

## 模块说明

- `dualx-api`：启动模块，包含 Controller、Service、任务示例、接口文档配置和环境配置。
- `dualx-common`：公共模块，包含统一返回、异常处理、Redis 工具、Sa-Token 鉴权、安全请求、日志配置等。
- `dualx-db`：数据库模块，包含实体、Mapper 和 XML。

## 启动方式

先确认本机使用 JDK 21：

```bash
java -version
mvn -version
```

编译：

```bash
mvn clean install -DskipTests
```

启动 dev 环境：

```bash
mvn -pl dualx-api spring-boot:run -Dspring-boot.run.profiles=dev
```

默认端口：

```text
8201
```

## 配置文件

主配置：

```text
dualx-api/src/main/resources/config/application.yml
```

环境配置：

```text
dualx-api/src/main/resources/config/application-dev.yml
dualx-api/src/main/resources/config/application-test.yml
dualx-api/src/main/resources/config/application-prod.yml
```

常用配置项：

- `spring.datasource`：MySQL 连接配置。
- `spring.data.redis`：Redis 连接配置。
- `logging.level`：日志级别。
- `sa-token`：登录态、Token、并发登录等配置。
- `spring.cloud.sentinel`：Sentinel 控制台、客户端通信端口、Web资源统计和限流配置。
- `com.app.upload`：文件上传路径和访问地址。
- `com.app.task`：定时任务开关和 cron 配置。
- `com.app.secure-request`：安全请求签名、防重放和 RSA 解密配置。

## 接口文档

只使用 Knife4j 页面：

```text
http://127.0.0.1:8201/doc.html
```

OpenAPI JSON：

```text
http://127.0.0.1:8201/v3/api-docs
```

旧 Swagger UI 入口已重定向到 `doc.html`。

## Sentinel

控制台地址：

```text
http://127.0.0.1:8858/
```

默认登录账号密码：

```text
账号：sentinel
密码：sentinel
```

这个账号密码是 Sentinel Dashboard 自己的登录配置，业务应用连接控制台时只需要配置 `spring.cloud.sentinel.transport.dashboard`，不需要在项目里配置控制台账号密码。

如果需要修改 Dashboard 登录账号密码，启动 Sentinel Dashboard 时增加 JVM 参数：

```bash
java -Dserver.port=8858 \
  -Dsentinel.dashboard.auth.username=sentinel \
  -Dsentinel.dashboard.auth.password=sentinel \
  -jar sentinel-dashboard.jar
```

应用配置按环境放在：

```text
dualx-api/src/main/resources/config/application-dev.yml
dualx-api/src/main/resources/config/application-test.yml
dualx-api/src/main/resources/config/application-prod.yml
```

每个环境都有独立的 Sentinel 配置：

```yaml
spring:
  cloud:
    sentinel:
      enabled: true
      eager: true
      http-method-specify: true
      web-context-unify: false
      transport:
        dashboard: '${SENTINEL_DASHBOARD:127.0.0.1:8858}'
        port: '${SENTINEL_TRANSPORT_PORT:8719}'
        client-ip: '${SENTINEL_CLIENT_IP:}'
      filter:
        enabled: true
        order: 10
```

测试接口：

```text
GET /api/test/sentinel/ping
```

启动应用后先请求一次测试接口，再到 Sentinel 控制台查看 `dualx-api` 应用和对应资源。限流触发时接口会返回统一 JSON，HTTP 状态码为 `429`。

如果 Sentinel Dashboard 运行在 Docker 里，`SENTINEL_CLIENT_IP` 不能配置成 `127.0.0.1`，否则 Dashboard 会访问到容器自己。Mac本地开发默认使用 `host.docker.internal` 指向宿主机，也可以显式指定 Dashboard 能访问到的应用机器IP，例如：

```bash
SENTINEL_CLIENT_IP=192.168.0.116
```

## 鉴权

项目已废弃原 JWT，统一使用 Sa-Token。

登录成功后返回：

```json
{
  "token": "..."
}
```

前端后续请求放到请求头：

```http
Authorization: 登录返回的token
```

需要登录的接口使用：

```java
@Login
```

修改登录密码后会调用 `StpUtil.logout(userId)`，使旧登录态失效。

## 安全请求

前端接入文档：

```text
docs/frontend-secure-request.md
```

敏感接口使用：

```java
@SecureRequest
```

敏感字段使用：

```java
@SecureField
```

处理流程：

1. 前端调用 `/api/security/publicKey` 获取 RSA 公钥。
2. 前端用 RSA 公钥加密带 `@SecureField` 的字段。
3. 前端用原始请求 body 计算签名。
4. 后端校验时间戳、随机数、签名，并用 RSA 私钥自动解密字段。

请求头：

```http
X-Timestamp: 当前时间戳，秒或毫秒
X-Nonce: 每次请求唯一随机字符串
X-Sign: HMAC-SHA256签名
```

签名文本：

```text
METHOD + "\n" +
PATH + "\n" +
X-Timestamp + "\n" +
X-Nonce + "\n" +
SHA256(原始请求body)
```

签名密钥配置：

```yaml
com.app:
  secure-request:
    sign-secret: '${APP_SIGN_SECRET}'
    rsa-public-key: '${APP_RSA_PUBLIC_KEY}'
    rsa-private-key: '${APP_RSA_PRIVATE_KEY}'
```

RSA 密钥读取顺序：

- 优先读取环境变量 `APP_RSA_PUBLIC_KEY`、`APP_RSA_PRIVATE_KEY`。
- 环境变量不存在时，使用 `application.yml` 里的默认值。

生产环境必须修改默认 `sign-secret`，RSA 私钥建议只通过环境变量或外部配置注入。

## 文件上传

文件上传控制器：

```text
FileController
```

配置项：

```yaml
com.app:
  upload:
    filePath: /data/upload
    getHttpImg: https://example.com/upload/
```

## 定时任务

当前保留一个示例任务：

```text
SampleCronTask
```

配置项：

```yaml
com.app:
  task:
    enable: false
    cron:
      sampleCronTask: '0 */1 * * * ?'
```

`enable=false` 时不会启用定时任务。

## 日志

日志配置：

```text
dualx-common/src/main/resources/logback-spring.xml
```

日志文件：

```text
logs/dualx-api.log
logs/dualx-api-error.log
```

SQL 默认不打印。需要临时查看 SQL 时，可在环境配置里调整 mapper 包日志级别。

## 代码生成

代码生成测试类：

```text
dualx-api/src/test/java/com/app/web/MysqlTest.java
```

生成器会动态识别模块目录，不需要写死 `dualx-api`、`dualx-db` 路径。

## 当前主要接口分组

- 账号相关：`AccountController`
- 安全中心：`SafetyCenterController`
- 文件管理：`FileController`
- Banner：`BannerController`
- 安全配置：`SecurityController`
