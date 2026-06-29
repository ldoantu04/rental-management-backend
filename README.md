# 🏠 Hệ Thống Quản Lý Nhà Trọ — Backend API

> **Đồ án tốt nghiệp** — Hệ thống quản lý nhà trọ  
> RESTful API xây dựng trên Spring Boot 4, cung cấp toàn bộ nghiệp vụ quản lý nhà trọ, phòng trọ, hợp đồng, hóa đơn, thanh toán trực tuyến và trợ lý AI.

🔗 **Giao diện Frontend:** [rental-management-frontend](https://github.com/ldoantu04/rental-management-frontend.git)

---

## 📋 Mục lục

- [Tổng quan](#-tổng-quan)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc dự án](#-kiến-trúc-dự-án)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt và chạy dự án](#-cài-đặt-và-chạy-dự-án)
- [Cấu hình](#-cấu-hình)
- [Tài liệu API](#-tài-liệu-api)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [Tác giả](#-tác-giả)

---

## 🌟 Tổng quan

Hệ thống quản lý nhà trọ là một ứng dụng web full-stack giúp chủ trọ và nhân viên quản lý toàn diện hoạt động cho thuê phòng trọ. Phần Backend cung cấp REST API phục vụ các chức năng:

- **Quản lý nhà trọ & phòng trọ** — Thêm, sửa, xóa nhà trọ và phòng trọ với các trạng thái (Đang thuê / Trống / Bảo trì)
- **Quản lý khách thuê** — Lưu trữ thông tin cá nhân, CCCD, người ở ghép
- **Quản lý hợp đồng** — Tạo mới, gia hạn, thanh lý hợp đồng thuê phòng
- **Quản lý hóa đơn** — Tính tiền điện, nước, dịch vụ hàng tháng, xuất PDF, gửi email
- **Thanh toán trực tuyến VNPAY** — Tích hợp cổng thanh toán VNPAY (sandbox)
- **Quản lý nhân viên** — Phân quyền Admin / Nhân viên, gán quản lý nhà trọ
- **Thống kê & Báo cáo** — Biểu đồ doanh thu theo tháng, tình trạng phòng, cảnh báo hợp đồng sắp hết hạn
- **Trợ lý AI** — Chatbot tích hợp Gemini 2.5 Flash qua Spring AI, hỗ trợ tra cứu dữ liệu bằng ngôn ngữ tự nhiên
- **Thông báo & Email** — Gửi thông báo tự động, email hóa đơn cho khách thuê
- **Xuất dữ liệu** — Xuất hóa đơn dạng PDF, báo cáo dạng Excel

---

## 🛠 Công nghệ sử dụng

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Java** | 17 | Ngôn ngữ lập trình chính |
| **Spring Boot** | 4.0.6 | Framework phát triển backend |
| **Spring Security** | — | Xác thực và phân quyền |
| **Spring Data JPA** | — | Tương tác cơ sở dữ liệu (ORM) |
| **Spring AI** | 2.0.0 | Tích hợp trí tuệ nhân tạo (Gemini) |
| **Spring Mail** | — | Gửi email qua SMTP |
| **MySQL** | 8.x | Hệ quản trị cơ sở dữ liệu |
| **JWT (jjwt)** | 0.11.5 | Xác thực bằng token |
| **Cloudinary** | 1.39.0 | Lưu trữ ảnh trên đám mây |
| **OpenPDF** | 1.3.43 | Tạo hóa đơn dạng PDF |
| **Apache POI** | 5.3.0 | Xuất báo cáo dạng Excel |
| **VNPAY** | Sandbox | Cổng thanh toán trực tuyến |
| **Lombok** | — | Giảm mã lặp (getter, setter, ...) |
| **Maven** | Wrapper | Quản lý thư viện phụ thuộc |

---

## 🏗 Kiến trúc dự án

```
rental/
├── src/main/java/com/example/rental/
│   ├── RentalApplication.java          # Điểm khởi chạy ứng dụng
│   ├── ServletInitializer.java         # Hỗ trợ triển khai WAR
│   ├── config/                         # Cấu hình ứng dụng
│   │   ├── AppConfig.java              #   CORS, chuỗi bộ lọc bảo mật
│   │   ├── JwtProvider.java            #   Tạo và xác thực JWT
│   │   ├── JwtTokenValidator.java      #   Bộ lọc xác thực yêu cầu
│   │   ├── JwtConstant.java            #   Hằng số JWT
│   │   ├── VNPayConfig.java            #   Cấu hình cổng VNPAY
│   │   ├── AiConfig.java               #   Cấu hình Spring AI
│   │   └── JacksonConfig.java          #   Cấu hình chuyển đổi JSON
│   ├── controller/                     # Bộ điều khiển REST API
│   │   ├── AuthController.java         #   Đăng nhập, xác thực OTP
│   │   ├── MotelController.java        #   Quản lý nhà trọ
│   │   ├── RoomController.java         #   Quản lý phòng trọ
│   │   ├── TenantController.java       #   Quản lý khách thuê
│   │   ├── ContractController.java     #   Quản lý hợp đồng
│   │   ├── InvoiceController.java      #   Quản lý hóa đơn
│   │   ├── PaymentController.java      #   Thanh toán VNPAY
│   │   ├── TransactionController.java  #   Lịch sử giao dịch
│   │   ├── EmployeeController.java     #   Quản lý nhân viên
│   │   ├── DashboardController.java    #   Thống kê tổng hợp
│   │   ├── OverviewController.java     #   Tổng quan hệ thống
│   │   ├── ChatController.java         #   Trợ lý AI
│   │   ├── NotificationController.java #   Thông báo
│   │   ├── EmailTemplateController.java#   Mẫu email
│   │   ├── InvoiceSettingsController.java # Cài đặt hóa đơn
│   │   ├── UploadController.java       #   Tải lên tệp tin
│   │   └── UserController.java         #   Thông tin người dùng
│   ├── model/                          # Các thực thể JPA (Entity)
│   │   ├── User.java                   #   Người dùng
│   │   ├── Motel.java                  #   Nhà trọ
│   │   ├── Room.java                   #   Phòng trọ
│   │   ├── Tenant.java                 #   Khách thuê
│   │   ├── Roommate.java              #   Người ở ghép
│   │   ├── Contract.java              #   Hợp đồng
│   │   ├── ContractServiceItem.java   #   Dịch vụ đi kèm hợp đồng
│   │   ├── Invoice.java               #   Hóa đơn
│   │   ├── InvoiceServiceItem.java    #   Chi tiết dịch vụ hóa đơn
│   │   ├── Transaction.java           #   Giao dịch thanh toán
│   │   ├── Service.java               #   Dịch vụ (WiFi, giữ xe, ...)
│   │   ├── Notification.java          #   Thông báo
│   │   ├── EmailTemplate.java         #   Mẫu email
│   │   ├── ChatConversation.java      #   Cuộc hội thoại AI
│   │   ├── ChatMessage.java           #   Tin nhắn AI
│   │   ├── OtpToken.java              #   Mã OTP đăng nhập
│   │   └── SystemSetting.java         #   Cài đặt hệ thống
│   ├── dto/                            # Đối tượng truyền dữ liệu (DTO)
│   ├── domain/                         # Các enum trạng thái
│   │   ├── UserRole.java               #   ADMIN, STAFF
│   │   ├── RoomStatus.java             #   DANG_THUE, TRONG, BAO_TRI
│   │   ├── ContractStatus.java         #   DANG_HIEU_LUC, HET_HAN, ...
│   │   ├── InvoiceStatus.java          #   CHUA_THANH_TOAN, DA_THANH_TOAN, QUA_HAN
│   │   ├── PaymentStatus.java          #   THANH_CONG, THAT_BAI, ...
│   │   └── ...                         #   Các enum khác
│   ├── repository/                     # Lớp truy vấn dữ liệu (Repository)
│   ├── service/                        # Lớp xử lý nghiệp vụ
│   │   ├── *Service.java               #   Giao diện (Interface)
│   │   └── impl/                       #   Lớp triển khai (Implementation)
│   └── exception/                      # Xử lý ngoại lệ tùy chỉnh
├── src/main/resources/
│   ├── application.properties          # Tệp cấu hình ứng dụng
│   ├── static/                         # Tài nguyên tĩnh
│   └── templates/                      # Mẫu email
├── pom.xml                             # Danh sách thư viện Maven
├── mvnw / mvnw.cmd                     # Maven Wrapper
└── README.md
```

---

## 💻 Yêu cầu hệ thống

| Yêu cầu | Phiên bản tối thiểu |
|---|---|
| **Java JDK** | 17 trở lên |
| **MySQL** | 8.0 trở lên |
| **Maven** | 3.9+ (đã tích hợp sẵn Maven Wrapper) |
| **Git** | 2.x |

---

## 🚀 Cài đặt và chạy dự án

### Bước 1: Tải mã nguồn

```bash
git clone <đường-dẫn-repository>
cd rental
```

### Bước 2: Tạo cơ sở dữ liệu MySQL

Mở MySQL Workbench hoặc cửa sổ dòng lệnh MySQL và chạy:

```sql
CREATE DATABASE RentalManagement CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Bước 3: Cấu hình kết nối cơ sở dữ liệu

Mở tệp `src/main/resources/application.properties` và chỉnh sửa thông tin kết nối phù hợp với máy của bạn:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/RentalManagement
spring.datasource.username=root
spring.datasource.password=<mật_khẩu_MySQL_của_bạn>
```

### Bước 4: Cấu hình dịch vụ bên ngoài *(tùy chọn)*

Xem mục [Cấu hình](#-cấu-hình) bên dưới để thiết lập email, Cloudinary, VNPAY, trợ lý AI.

### Bước 5: Chạy ứng dụng

**Trên Windows:**

```cmd
mvnw.cmd spring-boot:run
```

**Trên macOS / Linux:**

```bash
./mvnw spring-boot:run
```

### Bước 6: Kiểm tra

Sau khi khởi động thành công, API sẽ chạy tại:

```
http://localhost:8080
```

> **Ghi chú:** Lần chạy đầu tiên, Hibernate sẽ tự động tạo các bảng trong cơ sở dữ liệu nhờ cấu hình `spring.jpa.hibernate.ddl-auto=update`.

### Bước 7: Cài đặt giao diện Frontend

Xem hướng dẫn cài đặt tại repository Frontend:  
👉 **[rental-management-frontend](https://github.com/ldoantu04/rental-management-frontend.git)**

---

## ⚙ Cấu hình

Toàn bộ cấu hình nằm trong tệp `src/main/resources/application.properties`:

### Cơ sở dữ liệu (MySQL)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/RentalManagement
spring.datasource.username=root
spring.datasource.password=<mật_khẩu_của_bạn>
```

### Email (SMTP Gmail)

Để sử dụng tính năng gửi email (mã OTP, thông báo hóa đơn), bạn cần tạo **Mật khẩu ứng dụng** từ tài khoản Gmail:

1. Truy cập [Bảo mật Tài khoản Google](https://myaccount.google.com/security)
2. Bật **Xác minh 2 bước**
3. Tạo **Mật khẩu ứng dụng** (App Password)
4. Điền vào cấu hình:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<email_của_bạn@gmail.com>
spring.mail.password=<mật_khẩu_ứng_dụng_16_ký_tự>
```

### Cloudinary (Lưu trữ ảnh đám mây)

Đăng ký tài khoản miễn phí tại [cloudinary.com](https://cloudinary.com) và điền thông tin:

```properties
cloudinary.cloud-name=<tên_cloud_của_bạn>
cloudinary.api-key=<khóa_api>
cloudinary.api-secret=<mã_bí_mật_api>
```

### VNPAY (Thanh toán trực tuyến)

Hệ thống sử dụng VNPAY Sandbox để thử nghiệm thanh toán:

1. Đăng ký tại [VNPAY Sandbox](https://sandbox.vnpayment.vn)
2. Lấy `Mã TMN` và `Chuỗi bí mật`

```properties
vnpay.tmn-code=<mã_tmn_của_bạn>
vnpay.hash-secret=<chuỗi_bí_mật>
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment/return
```

### Trợ lý AI (Gemini qua Spring AI)

Hệ thống tích hợp Gemini 2.5 Flash thông qua Spring AI:

```properties
spring.ai.openai.api-key=<khóa_api_gemini>
spring.ai.openai.base-url=https://generativelanguage.googleapis.com/v1beta/openai/
spring.ai.openai.chat.options.model=gemini-2.5-flash
```

Lấy khóa API miễn phí tại [Google AI Studio](https://aistudio.google.com/apikey).

---

## 📖 Tài liệu API

### Xác thực (Authentication)

Hệ thống sử dụng **JWT Bearer Token**. Sau khi đăng nhập, client gửi token trong tiêu đề mỗi yêu cầu:

```
Authorization: Bearer <jwt_token>
```

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `POST` | `/auth/send-otp` | Gửi mã OTP qua email | ❌ |
| `POST` | `/auth/verify-otp` | Xác thực OTP, nhận JWT | ❌ |

### Nhà trọ

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/motels` | Lấy danh sách nhà trọ | ✅ |
| `POST` | `/api/motels` | Tạo nhà trọ mới | ✅ Admin |
| `PUT` | `/api/motels/{id}` | Cập nhật nhà trọ | ✅ Admin |
| `DELETE` | `/api/motels/{id}` | Xóa nhà trọ | ✅ Admin |

### Phòng trọ

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/rooms` | Lấy danh sách phòng trọ | ✅ |
| `POST` | `/api/rooms` | Tạo phòng mới | ✅ |
| `PUT` | `/api/rooms/{id}` | Cập nhật thông tin phòng | ✅ |
| `DELETE` | `/api/rooms/{id}` | Xóa phòng | ✅ |

### Khách thuê

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/tenants` | Lấy danh sách khách thuê | ✅ |
| `POST` | `/api/tenants` | Thêm khách thuê mới | ✅ |
| `PUT` | `/api/tenants/{id}` | Cập nhật thông tin khách | ✅ |
| `DELETE` | `/api/tenants/{id}` | Xóa khách thuê | ✅ |

### Hợp đồng

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/contracts` | Lấy danh sách hợp đồng | ✅ |
| `POST` | `/api/contracts` | Tạo hợp đồng mới | ✅ |
| `PUT` | `/api/contracts/{id}` | Cập nhật hợp đồng | ✅ |
| `DELETE` | `/api/contracts/{id}` | Xóa hợp đồng | ✅ |

### Hóa đơn

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/invoices` | Lấy danh sách hóa đơn | ✅ |
| `POST` | `/api/invoices` | Tạo hóa đơn mới | ✅ |
| `PUT` | `/api/invoices/{id}` | Cập nhật hóa đơn | ✅ |
| `GET` | `/api/invoices/{id}/pdf` | Xuất hóa đơn dạng PDF | ✅ |

### Thanh toán

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `POST` | `/api/payment/create` | Tạo liên kết thanh toán VNPAY | ✅ |
| `GET` | `/payment/return` | Trang kết quả sau thanh toán | ❌ |
| `GET` | `/api/payment/ipn` | Nhận thông báo từ VNPAY (IPN) | ❌ |

### Giao dịch

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/transactions` | Lấy lịch sử giao dịch | ✅ |

### Thống kê

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/overview` | Dữ liệu tổng quan hệ thống | ✅ |
| `GET` | `/api/dashboard/revenue` | Biểu đồ doanh thu theo tháng | ✅ |
| `GET` | `/api/dashboard/room-status` | Tình trạng phòng trọ | ✅ |
| `GET` | `/api/dashboard/filters` | Bộ lọc thống kê | ✅ |

### Nhân viên

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/employees` | Lấy danh sách nhân viên | ✅ Admin |
| `POST` | `/api/employees` | Thêm nhân viên mới | ✅ Admin |
| `PUT` | `/api/employees/{id}` | Cập nhật nhân viên | ✅ Admin |
| `DELETE` | `/api/employees/{id}` | Xóa nhân viên | ✅ Admin |

### Trợ lý AI

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `POST` | `/api/chat` | Gửi tin nhắn đến trợ lý AI | ✅ |
| `GET` | `/api/chat/conversations` | Lấy lịch sử hội thoại | ✅ |

### Thông báo & Cài đặt

| Phương thức | Đường dẫn | Mô tả | Xác thực |
|---|---|---|---|
| `GET` | `/api/notifications` | Lấy danh sách thông báo | ✅ |
| `GET` | `/api/email-templates` | Lấy danh sách mẫu email | ✅ |
| `GET` | `/api/invoice-settings` | Lấy cài đặt hóa đơn | ✅ |
| `POST` | `/api/upload` | Tải lên tệp tin / ảnh | ✅ |

---

## 🗄 Cơ sở dữ liệu

### Sơ đồ quan hệ

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Người dùng  │     │   Nhà trọ    │     │  Phòng trọ   │
│ (nguoi_dung) │────▶│  (nha_tro)  │────▶│ (phong_tro)  │
└─────────────┘     └─────────────┘     └──────┬───────┘
                                                │
┌─────────────┐     ┌─────────────┐            │
│  Khách thuê  │     │  Hợp đồng   │◀───────────┘
│ (khach_thue) │────▶│ (hop_dong)  │
└─────────────┘     └──────┬──────┘
                           │
                    ┌──────▼──────┐     ┌─────────────┐
                    │   Hóa đơn   │────▶│  Giao dịch   │
                    │  (hoa_don)  │     │ (giao_dich)  │
                    └─────────────┘     └─────────────┘
```

### Danh sách bảng chính

| Tên bảng | Mô tả |
|---|---|
| `nguoi_dung` | Người dùng hệ thống (Admin / Nhân viên) |
| `nha_tro` | Thông tin nhà trọ |
| `phong_tro` | Thông tin phòng trọ |
| `khach_thue` | Thông tin khách thuê |
| `hop_dong` | Hợp đồng thuê phòng |
| `hoa_don` | Hóa đơn hàng tháng |
| `giao_dich` | Lịch sử giao dịch thanh toán |
| `dich_vu` | Dịch vụ đi kèm (WiFi, giữ xe, ...) |
| `thong_bao` | Thông báo hệ thống |
| `email_template` | Mẫu email thông báo |
| `cai_dat_he_thong` | Cài đặt hệ thống |
| `otp_token` | Mã OTP đăng nhập |

---

## 👤 Tác giả

- **Sinh viên:** Lê Đoan Tú
- **Email:** ledoantu04@gmail.com
- **Đồ án tốt nghiệp** — Hệ thống quản lý nhà trọ

---

<p align="center">
  <i>Hệ thống Quản lý Nhà trọ — Backend API © 2026</i>
</p>
