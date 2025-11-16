# Tài liệu Kiến trúc Kỹ thuật - FTP Mina Server

## 1. Tổng quan Kiến trúc

FTP Mina Server là một ứng dụng Spring Boot được thiết kế để cung cấp dịch vụ truyền tệp an toàn và hiệu quả qua giao thức FTP và FTPS. Kiến trúc của hệ thống được xây dựng theo các module chính, tận dụng sức mạnh của Spring Framework để quản lý cấu hình, dependency injection và các tác vụ nền.

Trái tim của hệ thống là sự kết hợp giữa **Apache Mina FtpServer** và một cơ chế quản lý người dùng tùy chỉnh, được tối ưu hóa hiệu năng thông qua một lớp **cache trung gian**.

![Kiến trúc tổng quan](https://i.imgur.com/your-architecture-diagram.png)
*(Lưu ý: Đây là placeholder cho sơ đồ kiến trúc. Bạn có thể tạo sơ đồ bằng các công cụ như diagrams.net và thay thế link này.)*

## 2. Luồng Hoạt động Chính

### 2.1. Luồng Khởi tạo và Đồng bộ Dữ liệu

1.  **Khởi động ứng dụng:** Khi ứng dụng Spring Boot khởi chạy, `FtpServerConfig` được kích hoạt.
2.  **Nạp Cache ban đầu:** Bean `AccountFtpCache` được khởi tạo. Nó ngay lập tức thực hiện một truy vấn SQL (`SELECT * FROM ACCOUNT_FTP`) để lấy toàn bộ thông tin người dùng từ cơ sở dữ liệu.
3.  **Lưu vào Cache:** Dữ liệu người dùng được lưu vào một `ConcurrentHashMap` trong bộ nhớ, với tên tài khoản (`account`) làm khóa.
4.  **Khởi tạo FTP Server:** `FtpServerFactory` cấu hình máy chủ FTP, sử dụng `CustomUserManager` làm trình quản lý người dùng.
5.  **Lắng nghe kết nối:** Máy chủ bắt đầu lắng nghe các kết nối đến trên các cổng đã được định cấu hình cho FTP và FTPS.
6.  **Tác vụ Đồng bộ Định kỳ:** Một tác vụ đã lên lịch (`@Scheduled`) trong `AccountFtpCache` được kích hoạt **mỗi 10 giây**. Tác vụ này thực hiện lại việc truy vấn cơ sở dữ liệu và làm mới hoàn toàn bộ đệm. Điều này đảm bảo rằng mọi thay đổi trong bảng `ACCOUNT_FTP` (thêm/sửa/xóa người dùng) sẽ được phản ánh trong hệ thống gần như ngay lập tức mà không cần khởi động lại.

### 2.2. Luồng Xác thực Người dùng

1.  **Yêu cầu kết nối:** Một client FTP gửi yêu cầu đăng nhập với `username` và `password`.
2.  **Ủy quyền cho UserManager:** Apache Mina nhận yêu cầu và chuyển nó đến phương thức `authenticate()` của `CustomUserManager`.
3.  **Truy vấn Cache (Không phải DB):** `CustomUserManager` **không** truy vấn trực tiếp cơ sở dữ liệu. Thay vào đó, nó tìm kiếm thông tin người dùng trong `AccountFtpCache` bằng `username`.
4.  **Xác thực Mật khẩu:**
    *   Mật khẩu do client cung cấp được mã hóa bằng thuật toán **SHA-256**.
    *   Kết quả mã hóa được so sánh với mật khẩu đã được lưu trong cache.
5.  **Kiểm tra Trạng thái:** Hệ thống kiểm tra xem tài khoản có đang hoạt động không (`status == 1`).
6.  **Phản hồi:**
    *   **Thành công:** Nếu cả mật khẩu và trạng thái đều hợp lệ, một đối tượng `User` được tạo ra. `CustomUserManager` sẽ xây dựng đường dẫn thư mục cá nhân (`homeDirectory`) cho người dùng và trả về đối tượng `User`. Client được cấp quyền truy cập.
    *   **Thất bại:** Nếu thông tin không hợp lệ, một `AuthenticationFailedException` được ném ra, và client nhận được thông báo lỗi đăng nhập.

## 3. Các Thành phần Chi tiết

### 3.1. `CustomUserManager`

Đây là lớp tùy chỉnh cốt lõi, chịu trách nhiệm thay thế cơ chế quản lý người dùng mặc định của Apache Mina.

*   **Tích hợp Cache:** Phụ thuộc vào `AccountFtpCache` để lấy dữ liệu, giúp giảm độ trễ và tải cho DB.
*   **Logic Mã hóa:** Thực thi logic mã hóa mật khẩu nhất quán (SHA-256).
*   **Xây dựng Home Directory Động:** Tạo đường dẫn thư mục cho người dùng dựa trên các trường `folderAccess` và `folderFix` từ DB. Logic này cho phép cấu hình thư mục linh hoạt cho từng người dùng hoặc nhóm người dùng.
    *   Nếu `folderAccess` tồn tại, nó sẽ là thư mục gốc.
    *   Nếu `folderFix` tồn tại, nó sẽ được nối vào `folderAccess`.
    *   Nếu không có `folderAccess`, thư mục gốc sẽ là tên tài khoản.
    *   Tất cả các thư mục này đều nằm trong thư mục `ftp/` của ứng dụng.
*   **Read-Only:** Không triển khai các phương thức `save()` hoặc `delete()`, củng cố kiến trúc rằng việc quản lý người dùng được thực hiện ngoài băng (out-of-band), trực tiếp trên cơ sở dữ liệu.

### 3.2. `AccountFtpCache`

Lớp này đóng vai trò là một bộ đệm (cache) read-only, hiệu năng cao cho dữ liệu người dùng.

*   **Nguồn dữ liệu:** Sử dụng `JdbcTemplate` để truy vấn trực tiếp bảng `ACCOUNT_FTP`.
*   **Lưu trữ:** Dùng `ConcurrentHashMap` để đảm bảo an toàn luồng và truy cập nhanh.
*   **Cơ chế làm mới:** Sử dụng `@Scheduled` của Spring để tự động làm mới dữ liệu từ DB sau mỗi 10 giây. Đây là một cơ chế đơn giản nhưng hiệu quả để giữ cho dữ liệu cache luôn cập nhật.

### 3.3. `FtpServerConfig`

Lớp cấu hình Spring này chịu trách nhiệm khởi tạo và kết nối các thành phần lại với nhau.

*   **Định cấu hình Listeners:** Thiết lập các cổng và cấu hình SSL/TLS cho cả FTP và FTPS.
*   **Tiêm Dependencies:** Tiêm `CustomUserManager` và các `Ftplet` (như `AuditLogFtplet`) vào `FtpServerFactory`.
*   **Cấu hình Passive Mode:** Định cấu hình dải cổng thụ động, một yêu cầu quan trọng để FTP hoạt động qua tường lửa và NAT.

### 3.4. `AuditLogFtplet`

Một `Ftplet` được đăng ký với máy chủ FTP để lắng nghe các sự kiện. Dựa trên tên của nó, chức năng của lớp này là:

*   **Ghi lại Hoạt động:** Lắng nghe các sự kiện như `onLogin`, `onUploadFile`, `onDownloadFile`, `onDeleteFile`, v.v.
*   **Gửi đến DB Log:** Khi một sự kiện xảy ra, nó thu thập thông tin chi tiết (tên người dùng, hành động, đường dẫn tệp, IP client) và ghi một bản ghi vào bảng `ftp_audit_log` trong cơ sở dữ liệu log chuyên dụng.

## 4. Cơ sở dữ liệu

Hệ thống sử dụng hai kết nối cơ sở dữ liệu riêng biệt:

1.  **`spring.datasource` (Chính):**
    *   **Bảng:** `ACCOUNT_FTP`
    *   **Mục đích:** Lưu trữ thông tin cấu hình của người dùng FTP, bao gồm tên tài khoản, mật khẩu đã mã hóa, trạng thái, và các quy tắc về thư mục.
2.  **`spring.datasource-log` (Log):**
    *   **Bảng:** `FTP_AUDIT_LOG`
    *   **Mục đích:** Ghi lại nhật ký kiểm toán chi tiết về mọi hoạt động diễn ra trên máy chủ FTP. Việc tách riêng DB log giúp giảm tải cho DB chính và đơn giản hóa việc quản lý dữ liệu log.
