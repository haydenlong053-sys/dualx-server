# 前端安全请求接入说明

本文档用于前端接入后端 `@SecureRequest` 接口。

## 一、整体流程

调用带 `@SecureRequest` 的接口时，前端需要做两件事：

1. 对敏感字段做 RSA 公钥加密。
2. 对整个请求生成签名，并把签名信息放到请求头。

后端收到请求后会：

1. 校验时间戳是否过期。
2. 校验 `nonce` 是否重复。
3. 重新计算签名，判断请求内容是否被篡改。
4. 使用 RSA 私钥自动解密敏感字段。

## 二、需要后端提供的信息

前端需要拿到：

```text
baseUrl
signSecret
```

`baseUrl` 是接口域名，例如：

```text
http://127.0.0.1:8201
```

`signSecret` 是请求签名密钥，对应后端配置：

```yaml
com.app:
  secure-request:
    sign-secret: ${APP_SIGN_SECRET}
```

注意：当前方案要求前端参与签名，所以前端必须知道 `signSecret`。Web/H5 场景里这个值无法绝对保密，生产环境要配合 HTTPS、短时间戳、nonce 防重放一起使用。

## 三、获取 RSA 公钥

先调用：

```http
GET /api/security/publicKey
```

返回示例：

```json
{
  "code": 0,
  "msg": "Success",
  "data": {
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A..."
  }
}
```

前端拿 `data.publicKey` 加密敏感字段。

## 四、哪些字段需要 RSA 加密

只加密后端标了 `@SecureField` 的字段。

当前主要字段：

| 请求对象 | 需要加密的字段 |
| --- | --- |
| `AccountSignInReq` | `account`, `password` |
| `AccountSignUpReq` | `account`, `password` |
| `PasswordPayReq` | `fromPassword`, `toPassword`, `verification` |
| `MailReq` | `mail` |
| `NewMailReq` | `verified`, `newMail`, `newVerified` |
| `PasswordFindReq` | `account`, `verification` |
| `GoogleBindingReq` | `verified` |
| `AccountModifyReq` | `account`, `verification` |

没有标 `@SecureField` 的字段不要加密，正常传原值。

## 五、请求头

调用 `@SecureRequest` 接口时，必须带这 3 个请求头：

```http
X-Timestamp: 当前时间戳，秒或毫秒都可以
X-Nonce: 每次请求唯一随机字符串
X-Sign: HMAC-SHA256签名
```

说明：

- `X-Timestamp`：请求时间，后端默认允许 300 秒误差。
- `X-Nonce`：随机字符串，5 分钟内不能重复。
- `X-Sign`：请求签名，后端会重新计算并比对。

## 六、签名规则

后端实际签名文本是： POST/api/test/secure/echo1779787806hwyhn7281jjjsjqq

```text
METHOD + "\n" +
PATH + "\n" +
X-Timestamp + "\n" +
X-Nonce + "\n" +
SHA256(原始请求body)
```

字段说明：

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| `METHOD` | `POST` | 请求方法，必须大写 |
| `PATH` | `/api/account/signIn` | 请求路径，不带域名，不带 query |
| `X-Timestamp` | `1779783828032` | 请求头里的时间戳 |
| `X-Nonce` | `q7x8c9a1b2` | 请求头里的随机字符串 |
| `SHA256(body)` | `9b7c...e12f` | 原始 JSON 字符串的 SHA256 |

然后用 `signSecret` 对上面的签名文本做：

```text
HMAC-SHA256
```

得到的十六进制字符串就是 `X-Sign`。

重要：计算签名用的 body 字符串，必须和真正发出去的 HTTP body 完全一致。建议先生成 `body = JSON.stringify(data)`，签名和请求都使用这个 `body`。

## 七、前端 JS 示例

安装依赖：

```bash
npm install jsencrypt
```

示例代码：

```js
import JSEncrypt from 'jsencrypt'

const baseUrl = 'http://127.0.0.1:8201'
const signSecret = '<APP_SIGN_SECRET>'

async function sha256Hex(text) {
  const encoder = new TextEncoder()
  const buffer = await crypto.subtle.digest('SHA-256', encoder.encode(text))
  return [...new Uint8Array(buffer)]
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('')
}

async function hmacSha256Hex(text, secret) {
  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const buffer = await crypto.subtle.sign('HMAC', key, encoder.encode(text))
  return [...new Uint8Array(buffer)]
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('')
}

function randomNonce() {
  const array = new Uint8Array(16)
  crypto.getRandomValues(array)
  return [...array].map((item) => item.toString(16).padStart(2, '0')).join('')
}

function encryptByRsa(value, publicKey) {
  if (value === undefined || value === null || value === '') {
    return value
  }
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(
    `-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`
  )
  const encrypted = encryptor.encrypt(String(value))
  if (!encrypted) {
    throw new Error('RSA加密失败')
  }
  return encrypted
}

async function getPublicKey() {
  const response = await fetch(`${baseUrl}/api/security/publicKey`)
  const result = await response.json()
  return result.data.publicKey
}

async function securePost(path, data, secureFields = [], token) {
  const publicKey = await getPublicKey()

  const requestData = { ...data }
  secureFields.forEach((field) => {
    requestData[field] = encryptByRsa(requestData[field], publicKey)
  })

  const body = JSON.stringify(requestData)
  const timestamp = String(Date.now())
  const nonce = randomNonce()
  const bodyHash = await sha256Hex(body)
  const signText = [
    'POST',
    path,
    timestamp,
    nonce,
    bodyHash
  ].join('\n')
  const sign = await hmacSha256Hex(signText, signSecret)

  const headers = {
    'Content-Type': 'application/json',
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Sign': sign
  }
  if (token) {
    headers.Authorization = token
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers,
    body
  })
  return response.json()
}
```

## 八、登录接口示例

登录接口：

```http
POST /api/account/signIn
```

前端调用：

```js
const result = await securePost(
  '/api/account/signIn',
  {
    account: 'test1234',
    password: '12345678'
  },
  ['account', 'password']
)

console.log(result)
```

实际发送到后端的 body 类似：

```json
{
  "account": "RSA加密后的密文",
  "password": "RSA加密后的密文"
}
```

后端会自动解密成：

```json
{
  "account": "test1234",
  "password": "12345678"
}
```

## 九、注册接口示例

注册接口：

```http
POST /api/account/signUp
```

前端调用：

```js
const result = await securePost(
  '/api/account/signUp',
  {
    account: 'test1234',
    password: '12345678',
    shareCode: 'ABC123'
  },
  ['account', 'password']
)

console.log(result)
```

其中 `shareCode` 没有 `@SecureField`，不要加密。

## 十、验证步骤

前端可以按下面步骤验证：

1. 调用 `/api/security/publicKey`，确认能拿到 `publicKey`。
2. 调用 `/api/account/signIn`，先不传 `X-Sign`，后端应该返回参数错误。
3. 按文档生成 `X-Timestamp`、`X-Nonce`、`X-Sign` 后再次请求，签名应通过。
4. 重复使用同一个 `X-Nonce` 请求一次，后端应该返回参数错误。
5. 修改 body 里的任意一个字符但不重新签名，后端应该返回参数错误。

## 十一、后端测试接口

dev、test、uat、test02、test03 环境提供了两个测试接口。

先调用加密测试接口：

```http
POST /api/test/secure/encrypt
```

请求：

```json
{
  "account": "test1234",
  "password": "12345678",
  "remark": "hello"
}
```

返回里的 `account`、`password` 是 RSA 密文。

然后调用签名和解密测试接口：

```http
POST /api/test/secure/echo
```

请求 body 使用上一步返回的密文：

```json
{
  "account": "上一步返回的account密文",
  "password": "上一步返回的password密文",
  "remark": "hello"
}
```

同时按签名规则生成并传入：

```http
X-Timestamp
X-Nonce
X-Sign
```

如果成功，接口会返回解密后的 `account`、`password`。

## 十二、常见错误

### 签名一直失败

重点检查：

- `METHOD` 是否大写。
- `PATH` 是否只传 `/api/xxx`，不要带域名。
- `PATH` 不要带 query 参数。
- 签名用的 body 和实际发送的 body 是否完全一致。
- `X-Sign` 是否是小写或大写十六进制都可以，后端忽略大小写。
- `signSecret` 是否和后端当前环境一致。

### RSA 解密失败

重点检查：

- 公钥是否来自 `/api/security/publicKey`。
- 是否只加密了 `@SecureField` 字段。
- 加密结果是否是 Base64 字符串。
- 单个 RSA 字段原文不要太长。当前后端使用 2048 位 RSA，适合密码、验证码、账号这类短文本。

### nonce 重复

每次请求都重新生成 `X-Nonce`，不要复用。

### 时间戳过期

确认客户端时间和服务器时间不要差太多，默认允许误差 300 秒。
