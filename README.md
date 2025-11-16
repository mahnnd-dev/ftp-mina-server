# FTP Mina Server

## Tổng quan

Dự án này là một máy chủ FTP được xây dựng bằng Spring Boot và Apache Mina. Nó cung cấp một giải pháp mạnh mẽ và có thể mở rộng để quản lý việc truyền tệp qua FTP và FTPS. Máy chủ được tích hợp với Spring, cho phép dễ dàng cấu hình và quản lý. Nó cũng bao gồm các tính năng như ghi log kiểm toán vào cơ sở dữ liệu và quản lý người dùng dựa trên cơ sở dữ liệu.

## Công nghệ sử dụng

*   **Spring Boot:** Framework chính để xây dựng ứng dụng.
*   **Apache Mina SSHD:** Được sử dụng để tạo máy chủ FTP và FTPS.
*   **Spring Data JPA:** Để tương tác với cơ sở dữ liệu.
*   **Oracle & PostgreSQL:** Hỗ trợ cho cả hai cơ sở dữ liệu.
*   **Log4j2:** Để ghi log.
*   **Maven:** Để quản lý dependency và xây dựng dự án.

## Cấu hình

Cấu hình của ứng dụng được quản lý trong tệp `config/application.yml`. Dưới đây là các thuộc tính chính:

### Cấu hình cơ sở dữ liệu

Ứng dụng yêu cầu hai nguồn dữ liệu: một cho dữ liệu ứng dụng chính và một cho ghi log.

*   `spring.datasource`: Cấu hình nguồn dữ liệu chính.
*   `spring.datasource-log`: Cấu hình nguồn dữ liệu ghi log.

Bạn cần cập nhật các thuộc tính `jdbc-url`, `username`, và `password` cho cả hai nguồn dữ liệu.

### Cấu hình máy chủ FTP

*   `ftp.server.ftp-port`: Cổng cho kết nối FTP (mặc định: `2121`).
*   `ftp.server.ftps-port`: Cổng cho kết nối FTPS (mặc định: `990`).
*   `ftp.server.passive-ports`: Dải cổng thụ động để sử dụng.
*   `ftp.ssl.keystore.path`: Đường dẫn đến tệp kho khóa Java (JKS) cho FTPS.
*   `ftp.ssl.keystore.password`: Mật khẩu cho kho khóa.

## Cách chạy ứng dụng

1.  **Cấu hình `application.yml`**: Đảm bảo rằng các kết nối cơ sở dữ liệu và cài đặt FTP được cấu hình đúng.
2.  **Xây dựng dự án**: Sử dụng Maven để xây dựng dự án:
    ```bash
    mvn clean install
    ```
3.  **Chạy ứng dụng**: Chạy ứng dụng Spring Boot:
    ```bash
    java -jar target/ftp-mina-server-0.0.1-SNAPSHOT.jar
    ```

## Kiến trúc

*   **`com.neo.ftpserver.FtpMinaServerApplication`**: Lớp chính của Spring Boot để khởi động ứng dụng.
*   **`com.neo.ftpserver.config`**: Chứa các lớp cấu hình Spring, bao gồm cả cấu hình cho máy chủ FTP.
*   **`com.neo.ftpserver.service`**: Chứa logic nghiệp vụ, chẳng hạn như quản lý người dùng và xử lý tệp.
*   **`com.neo.ftpserver.ftp`**: Chứa các lớp liên quan đến máy chủ FTP, chẳng hạn như triển khai `FileSystemFactory` và `UserManager` tùy chỉnh.
*   **`com.neo.ftpserver.dto`**: Các đối tượng truyền dữ liệu được sử dụng trong ứng dụng.
*   **`com.neo.ftpserver.model`**: Các thực thể JPA được ánh xạ tới các bảng cơ sở dữ liệu.

## Ghi log

Ứng dụng sử dụng Log4j2 để ghi log. Cấu hình ghi log được xác định trong `config/log4j2.xml`.

### Ghi log kiểm toán

Tất cả các hành động FTP (ví dụ: đăng nhập, tải lên, tải xuống, xóa) được ghi lại vào bảng `ftp_audit_log` trong cơ sở dữ liệu ghi log. Điều này được cấu hình thông qua các thuộc tính `logging.job`.
