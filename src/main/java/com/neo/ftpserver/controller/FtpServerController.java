package com.neo.ftpserver.controller;

import com.neo.ftpserver.ftp.FtpServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ftp-server")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "FTP Server Management", description = "API để quản lý vòng đời của FTP server (start/stop/status)")
public class FtpServerController {

    private final FtpServerService ftpServerService;

    @PostMapping("/start")
    @Operation(
            summary = "Khởi động FTP server",
            description = "Start FTP server nếu đang dừng",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Server đã khởi động",
                            content = @Content(schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "500", description = "Có lỗi khi khởi động server")
            }
    )
    public ResponseEntity<?> startServer() {
        try {
            ftpServerService.start();
            return ResponseEntity.ok(Map.of("status", "started"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/stop")
    @Operation(
            summary = "Dừng FTP server",
            description = "Stop FTP server nếu đang chạy",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Server đã dừng",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    public ResponseEntity<?> stopServer() {
        ftpServerService.stop();
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    @GetMapping("/status")
    @Operation(
            summary = "Kiểm tra trạng thái FTP server",
            description = "Trả về trạng thái hiện tại (running, suspended)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trạng thái server",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "running", ftpServerService.isRunning(),
                "suspended", ftpServerService.isSuspended()
        ));
    }
}
