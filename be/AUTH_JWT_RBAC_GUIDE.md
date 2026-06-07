
# Huong Dan Auth, JWT va RBAC

Tai lieu nay mo ta base auth hien tai cua du an `sneak_shop`: dang ky, dang nhap, JWT, SecurityContext, entity user/user_role, va RBAC theo role/permission.

## 1. Tong quan cau truc

Code auth/security dang duoc chia theo cac package chinh:

```text
src/main/java/sneak_shop
+-- controller
|   +-- AuthController.java
+-- dto
|   +-- request
|   |   +-- RegisterRequest.java
|   |   +-- LoginRequest.java
|   +-- response
|       +-- AuthResponse.java
+-- entity
|   +-- UserEntity.java
|   +-- UserRoleEntity.java
|   +-- RoleEntity.java
|   +-- PermissionEntity.java
|   +-- RolePermissionEntity.java
+-- repository
|   +-- UserRepository.java
|   +-- RoleRepository.java
|   +-- PermissionRepository.java
+-- security
|   +-- SecurityConfig.java
|   +-- JwtService.java
|   +-- JwtAuthenticationFilter.java
|   +-- UserContext.java
|   +-- RestAuthenticationEntryPoint.java
|   +-- RestAccessDeniedHandler.java
+-- service
    +-- AuthService.java
    +-- impl
        +-- AuthServiceImpl.java
```

Trach nhiem tung phan:

- `AuthController`: khai bao API dang ky/dang nhap.
- `AuthServiceImpl`: xu ly nghiep vu dang ky/dang nhap.
- `UserEntity`: bang `users`.
- `UserRoleEntity`: bang `user_roles`, gan user voi role.
- `RoleEntity`: bang `roles`.
- `PermissionEntity`: bang `permissions`.
- `RolePermissionEntity`: bang `role_permissions`, gan role voi permission.
- `JwtService`: tao token va doc thong tin tu token.
- `JwtAuthenticationFilter`: doc header `Authorization`, validate token, set user vao `SecurityContextHolder`.
- `UserContext`: object principal duoc luu trong `SecurityContextHolder`.

## 2. Entity va database

### 2.1. UserEntity

`UserEntity` dai dien bang `users`.

Cac field chinh:

```java
private String id;
private String fullName;
private String phoneNumber;
private String password;
private Set<UserRoleEntity> userRoles;
private boolean enabled;
private Instant createdAt;
```

Luu y:

- `id` dang dung `GenerationType.UUID`, kieu `String`.
- `phoneNumber` la duy nhat, dung de dang ky va dang nhap.
- `password` duoc hash bang `BCryptPasswordEncoder`.
- `enabled = false` thi user khong dang nhap/request bang token duoc.
- `userRoles` quan he `OneToMany` voi `UserRoleEntity`.

### 2.2. UserRoleEntity

`UserRoleEntity` dai dien bang `user_roles`.

Moi record gan 1 role cho 1 user:

```java
private String id;
private UserEntity user;
private RoleEntity role;
```

Quan he:

```text
users 1 - n user_roles
roles 1 - n user_roles
```

Vi du:

```text
users
id = u1, phoneNumber = 0900000001

user_roles
id = ur1, user_id = u1, role_id = admin_role_id
```

### 2.3. RoleEntity

`RoleEntity` dai dien bang `roles`.

Field chinh:

```java
private String id;
private String name;
private Set<RolePermissionEntity> rolePermissions;
```

Vi du data:

```text
roles
id = role_admin_id, name = ADMIN
id = role_staff_id, name = STAFF
id = role_customer_id, name = CUSTOMER
```

### 2.4. PermissionEntity

`PermissionEntity` dai dien bang `permissions`.

Field chinh:

```java
private String id;
private String name;
```

Vi du data:

```text
permissions
id = p1, name = PRODUCT_CREATE
id = p2, name = PRODUCT_UPDATE
id = p3, name = PRODUCT_DELETE
```

### 2.5. RolePermissionEntity

`RolePermissionEntity` dai dien bang `role_permissions`.

Moi record gan 1 permission cho 1 role:

```java
private String id;
private RoleEntity role;
private PermissionEntity permission;
```

Vi du data:

```text
role_permissions
id = rp1, role_id = role_admin_id, permission_id = p1
id = rp2, role_id = role_admin_id, permission_id = p2
id = rp3, role_id = role_admin_id, permission_id = p3
```

## 3. RBAC: Role va Permission

He thong dang dung mo hinh:

```text
User -> Role -> Permission
```

`roles` la bang vai tro:

```text
ADMIN
STAFF
CUSTOMER
```

`permissions` la bang quyen hanh dong:

```text
ROLE_VIEW
ROLE_CREATE
ROLE_UPDATE
ROLE_DELETE
USER_VIEW
USER_CREATE
USER_UPDATE
USER_DELETE
PRODUCT_VIEW
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE
ORDER_VIEW
ORDER_CREATE
ORDER_UPDATE
ORDER_VIEW_OWN
```

Moi role co danh sach permission thong qua bang `role_permissions`. Vi du `ADMIN` co cac permission quan tri role/user/product/order.

Khi Spring Security can kiem tra quyen, `UserEntity.getAuthorities()` se gom:

- Role theo format `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_CUSTOMER`
- Permission theo cot `permissions.name`, vi du `USER_CREATE`, `PRODUCT_UPDATE`

Vi du dung trong controller:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Hoac check theo permission:

```java
@PreAuthorize("hasAuthority('PRODUCT_CREATE')")
```

Nen uu tien check theo permission voi cac API hanh dong quan trong, vi no linh hoat hon check role.

Neu muon them quyen cho `ADMIN`, can them data vao DB:

```text
permissions
name = PRODUCT_EXPORT

role_permissions
role_id = id cua role ADMIN
permission_id = id cua permission PRODUCT_EXPORT
```

Neu permission da ton tai, chi can insert them vao `role_permissions`.

## 4. API dang ky

Endpoint:

```http
POST /api/auth/register
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Nguyen Van A",
  "phoneNumber": "0912345678",
  "password": "123456"
}
```

Validate:

- `fullName`: bat buoc, tu 2 den 100 ky tu.
- `phoneNumber`: bat buoc, dung dinh dang so dien thoai di dong Viet Nam.
- `password`: bat buoc, tu 6 den 100 ky tu.

Regex so dien thoai hien tai:

```java
^(0|\+84)(3|5|7|8|9)\d{8}$
```

Chap nhan:

```text
0912345678
+84912345678
```

Khong chap nhan:

```text
123456
0212345678
84912345678
```

Luon tao user moi voi role mac dinh lay tu DB:

```text
roles.name = CUSTOMER
```

Neu so dien thoai da ton tai, service nem:

```java
new AppException(ErrorCode.CONFLICT, "So dien thoai da ton tai")
```

Response dang ky hien tra ve `AuthResponse`. Neu khong truyen token vao `AuthResponse.from(user)`, cac field `tokenType` va `accessToken` se la `null`.

## 5. API dang nhap

Endpoint:

```http
POST /api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "phoneNumber": "0900000001",
  "password": "admin123"
}
```

Flow trong `AuthServiceImpl.login()`:

1. Tim user theo `phoneNumber`.
2. Neu khong thay, tra loi `UNAUTHORIZED`.
3. So sanh password bang `passwordEncoder.matches(...)`.
4. Neu password sai, tra loi `UNAUTHORIZED`.
5. Neu user bi khoa `enabled = false`, tra loi `UNAUTHORIZED`.
6. Tao JWT bang `jwtService.generateAccessToken(user)`.
7. Tra ve `AuthResponse` co token.

Response mau:

```json
{
  "id": "f4cc6e5d-3a66-4b8f-9a6b-2c55f67a1111",
  "fullName": "Admin",
  "phoneNumber": "0900000001",
  "roles": ["ADMIN"],
  "permissions": ["ROLE_VIEW", "ROLE_CREATE", "USER_VIEW"],
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## 6. JWT config

Trong `application.yaml`:

```yaml
app:
  jwt:
    secret: "sneak-shop-local-development-secret-key-change-me"
    expiration-minutes: 1440
```

Y nghia:

- `secret`: khoa ky JWT. Nen doi khi len moi truong that.
- `expiration-minutes`: thoi gian song cua access token, hien tai la 1440 phut, tuc 1 ngay.

Luu y:

- Secret phai du dai cho HMAC key.
- Khong commit secret production len source code.
- Nen dua secret production vao bien moi truong sau nay.

## 7. JWT token gom thong tin gi

`JwtService.generateAccessToken(user)` tao token voi cac claim:

```java
subject = user.getId()
phoneNumber = user.getPhoneNumber()
roles = user.getRoles()
issuedAt = now
expiration = now + expirationMinutes
```

Trong filter, he thong chu yeu dung `subject` de lay `userId`, sau do query DB lai:

```java
String userId = jwtService.getUserId(token);
UserEntity user = userRepository.findById(userId)
    .filter(UserEntity::isEnabled)
    .orElseThrow(...);
```

Ly do van query DB:

- Dam bao user con ton tai.
- Dam bao user chua bi khoa.
- Lay role/permission moi nhat tu database.
- Neu role user thay doi, request sau se dung role moi, khong phu thuoc hoan toan vao token cu.

## 8. Goi API protected bang token

Sau khi login thanh cong, client lay `accessToken` va gui trong header:

```http
Authorization: Bearer <accessToken>
```

Vi du:

```bash
curl -X GET http://localhost:8080/api/some-protected-api \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Neu token hop le:

- Filter set authentication vao `SecurityContextHolder`.
- Request duoc di tiep vao controller.

Neu token sai/het han/user bi khoa:

- Filter tra ve `401`.
- Response theo format `ErrorResponse`.

Vi du:

```json
{
  "timestamp": "2026-05-22T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "UNAUTHORIZED",
  "message": "Token khong hop le hoac da het han",
  "path": "/api/products",
  "details": null
}
```

## 9. SecurityContextHolder dang luu gi

Trong `JwtAuthenticationFilter`, sau khi validate token:

```java
UserContext principal = UserContext.from(user);

SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(
        principal,
        null,
        principal.authorities()
    )
);
```

Nghia la:

```java
authentication.getPrincipal()
```

se la object `UserContext`, gom:

```java
String id
String fullName
String phoneNumber
Set<String> roles
Set<String> permissions
Collection<? extends GrantedAuthority> authorities
```

Cach lay user hien tai trong controller/service:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
UserContext user = (UserContext) authentication.getPrincipal();

String userId = user.id();
String phoneNumber = user.phoneNumber();
Set<String> roles = user.roles();
Set<String> permissions = user.permissions();
```

Co the viet helper sau nay neu muon goi ngan hon.

## 10. SecurityConfig

`SecurityConfig` dang cau hinh:

```java
csrf disabled
httpBasic disabled
session STATELESS
/actuator/health permitAll
/api/auth/** permitAll
anyRequest authenticated
JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
```

Y nghia:

- API dang ky/dang nhap public.
- Tat session, server khong luu login state.
- Tat Basic Auth.
- Cac API khac can JWT Bearer token.

## 11. Exception handling

Project co `GlobalExceptionHandler` xu ly loi theo format thong nhat:

- `AppException`
- validation error
- `AccessDeniedException`
- exception khong mong muon

Loi auth/JWT trong filter duoc tra truc tiep bang JSON vi loi xay ra truoc controller.

## 12. Du lieu seed mac dinh

Trong `SecurityConfig`, he thong seed:

- Role mac dinh: `ADMIN`, `STAFF`, `CUSTOMER`
- Permission mac dinh
- Mapping `role_permissions`
- 1 user admin mac dinh neu database chua co

```text
Admin - 0900000001 / admin123
```

User admin duoc gan role:

```text
Admin -> ADMIN
```

## 13. Test nhanh bang curl

### 13.1. Login admin

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\":\"0900000001\",\"password\":\"admin123\"}"
```

Lay `accessToken` trong response.

### 13.2. Goi API protected

```bash
curl -X GET http://localhost:8080/api/example \
  -H "Authorization: Bearer <accessToken>"
```

Neu chua co API `/api/example`, hay thay bang API protected that trong project.

### 13.3. Dang ky customer moi

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Nguyen Van A\",\"phoneNumber\":\"0912345678\",\"password\":\"123456\"}"
```

### 13.4. Dang nhap customer moi

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\":\"0912345678\",\"password\":\"123456\"}"
```

## 14. Luu y khi phat trien tiep

Nen lam tiep cac viec sau khi project lon hon:

- Tach config JWT secret sang bien moi truong.
- Them refresh token neu can session dai ngay.
- Them API logout neu co blacklist token.
- Xay API quan tri `roles`, `permissions`, `role_permissions` neu muon admin cap nhat quyen truc tiep tu dashboard.
- Them helper lay current user de tranh cast `SecurityContextHolder` lap lai nhieu noi.
- Them test cho login, JWT filter, permission check.

## 15. Lenh chay test

Sau khi may da cau hinh JDK 17 va `JAVA_HOME`, chay:

```powershell
.\mvnw.cmd test
```

Neu gap loi:

```text
The JAVA_HOME environment variable is not defined correctly
```

thi can cai JDK 17 va set lai `JAVA_HOME` truoc khi build/test.
