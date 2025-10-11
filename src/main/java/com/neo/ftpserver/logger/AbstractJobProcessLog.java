package com.neo.ftpserver.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public abstract class AbstractJobProcessLog {

    private final Logger logger = LoggerFactory.getLogger(AbstractJobProcessLog.class);
    private static final Logger logOfDay = LoggerFactory.getLogger("logs-of-day");

    @Value("${logging.job.path.wait}")
    private String pathWait;

    @Value("${logging.job.path.retry}")
    private String pathRetry;

    @Value("${logging.job.path.failed}")
    private String pathFailed;

    @Value("${logging.job.path.file-pattern}")
    private String filePattern;

    @Value("${logging.job.batch-size}")
    private int batchSize;

    @Value("${logging.job.max-retry}")
    private int maxRetry;

    protected int count = 0;

    // Lưu trữ tên file và số lần retry
    private final Map<String, Integer> storeFileFail = new HashMap<>();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    // Lưu trữ tạm thời các params cho batch (mỗi Object[] là một row)
    private final List<Object[]> batchParams = new ArrayList<>();

    @Autowired
    public AbstractJobProcessLog(JdbcTemplate jdbcTemplate,
                                  PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    /**
     * Hàm xử lý chính, được chạy định kỳ theo cấu hình
     */
    @Scheduled(fixedDelayString = "${logging.job.path.time-read}")
    public void execute() {
        this.readFileLog(pathWait, false); // Đọc file mới
        this.readFileLog(pathRetry, true); // Đọc file cần retry
    }

    /**
     * Đọc tất cả file log trong thư mục
     *
     * @param path     Đường dẫn đến thư mục chứa file log
     * @param isReload Xác định đây là lần đọc đầu tiên hay là lần retry
     */
    public void readFileLog(String path, boolean isReload) {
        try {
            Files.createDirectories(Paths.get(path)); // Tạo thư mục nếu chưa tồn tại
            // Không cần initConnect() nữa, JdbcTemplate handle connection tự động
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path), filePattern)) {
                for (Path filePath : stream) {
                    readFileJava8(filePath, isReload); // Xử lý từng file
                }
            }
            // Không cần disconnect() nữa
        } catch (Exception e) {
            this.logger.error("readFileLog Exception: {}", e.getMessage());
        }
    }

    /**
     * Đọc nội dung của một file log
     *
     * @param filePath Đường dẫn đến file log
     * @param isReload Xác định đây là lần đọc đầu tiên hay là lần retry
     */
    public void readFileJava8(Path filePath, boolean isReload) {
        Instant start = Instant.now();
        try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (!isReload) {
                    logOfDay.info(line); // Ghi log ra file
                }
                processLog(line, start); // Xử lý từng dòng log
            });
        } catch (Exception e) {
            e.printStackTrace();
            this.logger.error("readFileJava8 Exception: {}", e.getMessage());
        } finally {
            handleFileAfterRead(filePath.toFile(), isReload); // Xử lý file sau khi đọc
            commitFinal(start); // Commit cuối cùng nếu còn batch pending
        }
    }

    /**
     * Xử lý từng dòng log, chèn dữ liệu vào batch
     *
     * @param content   Nội dung của dòng log
     * @param startTime Thời điểm bắt đầu xử lý file
     */
    private void processLog(String content, Instant startTime) {
        if (count >= this.batchSize) {
            logger.info("count {}", count);
            this.commit(startTime); // Commit batch hiện tại
            count = 0;
            logger.info("reset batch size {}", count);
        }
        processData(content); // Xử lý dữ liệu của dòng log (sẽ gọi addBatchInsert và incrementCount)
    }

    /**
     * Xử lý file sau khi đọc: xóa file nếu commit thành công, di chuyển file nếu commit thất bại
     *
     * @param f        File log
     * @param isReload Xác định đây là lần đọc đầu tiên hay là lần retry
     */
    private void handleFileAfterRead(File f, boolean isReload) {
        try {
            Instant start = Instant.now();
            if (this.commit(start)) {
                this.logger.info(">>>>>>>>>>>>>>>> delete file: {} status: {}", f.getAbsolutePath(), Files.deleteIfExists(f.toPath()));
            } else {
                handleFileRenameAndRetry(f, isReload); // Xử lý file cần retry
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.logger.error("handleFileAfterRead Exception: {}", e.getMessage());
        }
    }

    /**
     * Xử lý file cần retry: di chuyển file vào thư mục retry hoặc failed
     *
     * @param f        File log
     * @param isReload Xác định đây là lần đọc đầu tiên hay là lần retry
     * @throws IOException
     */
    private void handleFileRenameAndRetry(File f, boolean isReload) throws IOException {
        if (this.maxRetry == -1) {
            if (!isReload) {
                moveFile(this.pathRetry, f); // Di chuyển file vào thư mục retry
            }
        } else if (isReload) {
            handleReloadRetry(f); // Xử lý file retry
        } else {
            storeFileFailAndRetry(f); // Lưu trữ file và di chuyển vào thư mục retry
        }
    }

    /**
     * Xử lý file retry: tăng số lần retry, di chuyển file vào thư mục failed nếu vượt quá số lần retry
     *
     * @param f File log
     * @throws IOException
     */
    private void handleReloadRetry(File f) throws IOException {
        Integer current = this.storeFileFail.get(f.getName());
        if (current != null) {
            if (current < this.maxRetry) {
                current = current + 1;
                this.storeFileFail.put(f.getName(), current);
            } else {
                moveFile(this.pathFailed, f); // Di chuyển file vào thư mục failed
                this.storeFileFail.remove(f.getName());
            }
        }
    }

    /**
     * Lưu trữ file và di chuyển vào thư mục retry
     *
     * @param f File log
     * @throws IOException
     */
    private void storeFileFailAndRetry(File f) throws IOException {
        this.storeFileFail.put(f.getName(), 0);
        moveFile(this.pathRetry, f); // Di chuyển file vào thư mục retry
    }

    /**
     * Commit cuối cùng nếu còn batch pending
     *
     * @param startTime Thời điểm bắt đầu xử lý
     */
    private void commitFinal(Instant startTime) {
        if (count > 0) {
            commit(startTime);
        }
    }

    /**
     * Di chuyển file đến thư mục đích
     *
     * @param targetPath Đường dẫn đến thư mục đích
     * @param sourceFile File cần di chuyển
     * @throws IOException
     */
    private void moveFile(String targetPath, File sourceFile) throws IOException {
        Files.createDirectories(Paths.get(targetPath)); // Tạo thư mục nếu chưa tồn tại
        Path target = Paths.get(targetPath + "/" + sourceFile.getName());
        Files.move(sourceFile.toPath(), target);
        this.logger.error(">>>>>>>>>>>>>>>> move file, rename to: {} to {}", targetPath, sourceFile.toPath());
    }

    /**
     * Thêm dữ liệu vào batch (gọi từ processData trong subclass)
     *
     * @param args Các parameters cho row hiện tại (từ parse string hoặc JSON)
     */
    protected void addBatchInsert(Object... args) {
        if (args != null) {
            batchParams.add(args.clone()); // Clone để tránh mutate
        }
        incrementCount();
    }

    /**
     * Commit batch hiện tại vào database (sử dụng TransactionTemplate cho transaction)
     *
     * @param startTime Thời điểm bắt đầu xử lý batch
     * @return true nếu commit thành công, false nếu commit thất bại
     */
    private boolean commit(Instant startTime) {
        try {
            String currentSql = getSql(); // Lấy SQL từ subclass
            Boolean success = this.transactionTemplate.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    try {
                        if (!batchParams.isEmpty()) {
                            int[] results = jdbcTemplate.batchUpdate(currentSql, new BatchPreparedStatementSetter() {
                                @Override
                                public void setValues(PreparedStatement ps, int i) throws SQLException {
                                    Object[] params = batchParams.get(i);
                                    for (int j = 0; j < params.length; j++) {
                                        ps.setObject(j + 1, params[j]);
                                    }
                                }

                                @Override
                                public int getBatchSize() {
                                    return batchParams.size();
                                }
                            });
                            Instant end = Instant.now();
                            long durationInMilliseconds = Duration.between(startTime, end).toMillis();
                            logger.info("Commit successful num: {} rows; execute time: {} milliseconds", results.length, durationInMilliseconds);
                            batchParams.clear(); // Clear sau success
                            return true;
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        status.setRollbackOnly(); // Rollback nếu lỗi
                        logger.error("Batch execute Exception: {}", e.getMessage());
                        return false;
                    }
                }
            });
            count = 0; // Reset count sau commit
            return success != null && success;
        } catch (Exception e) {
            logger.error("Commit Exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Xử lý dữ liệu của dòng log, chuyển đổi string (hoặc JSON) sang params và chèn vào batch
     * (Implement ở subclass: parse và gọi addBatchInsert(args...))
     *
     * @param data Dữ liệu của dòng log
     */
    public abstract void processData(String data);

    public abstract String getSql();

    protected void incrementCount() {
        count++;
    }
}