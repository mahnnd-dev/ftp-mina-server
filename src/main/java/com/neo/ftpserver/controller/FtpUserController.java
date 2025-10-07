package com.neo.ftpserver.controller;

import com.neo.ftpserver.dto.AccountFtpCreateDto;
import com.neo.ftpserver.dto.AccountFtpResponseDto;
import com.neo.ftpserver.dto.AccountFtpUpdateDto;
import com.neo.ftpserver.service.AccountFtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ftp-users")
@CrossOrigin(origins = "*")
@Tag(name = "FTP User Management", description = "Quản lý người dùng FTP (CRUD APIs)")
public class FtpUserController {

    @Autowired
    private AccountFtpService userService;

    @PostMapping
    @Operation(
            summary = "Tạo mới FTP User",
            description = "API để tạo người dùng FTP mới",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User được tạo thành công",
                            content = @Content(schema = @Schema(implementation = AccountFtpResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request không hợp lệ")
            }
    )
    public ResponseEntity<AccountFtpResponseDto> createUser(
            @Valid @RequestBody AccountFtpCreateDto dto) {
        AccountFtpResponseDto response = userService.createUser(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật FTP User",
            description = "API cập nhật thông tin người dùng FTP theo ID"
    )
    public ResponseEntity<AccountFtpResponseDto> updateUser(
            @Parameter(description = "ID của user cần cập nhật")
            @PathVariable Long id,
            @Valid @RequestBody AccountFtpUpdateDto dto) {
        AccountFtpResponseDto response = userService.updateUser(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa FTP User",
            description = "API xóa người dùng FTP theo ID"
    )
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID của user cần xóa")
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy thông tin FTP User theo ID",
            description = "API trả về thông tin chi tiết của user theo ID"
    )
    public ResponseEntity<AccountFtpResponseDto> getUserById(
            @Parameter(description = "ID của user cần lấy")
            @PathVariable Long id) {
        AccountFtpResponseDto response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    @Operation(
            summary = "Lấy thông tin FTP User theo Username",
            description = "API trả về thông tin chi tiết của user theo username"
    )
    public ResponseEntity<AccountFtpResponseDto> getUserByUsername(
            @Parameter(description = "Tên đăng nhập của user cần lấy")
            @PathVariable String username) {
        AccountFtpResponseDto response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Danh sách tất cả FTP User",
            description = "API trả về toàn bộ danh sách người dùng FTP"
    )
    public ResponseEntity<List<AccountFtpResponseDto>> getAllUsers() {
        List<AccountFtpResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
