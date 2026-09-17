package com.hkywt.attendance.module.task.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class ClassPhotoCleanupScheduler {

    @Value("${attendance.class-photo.root-dir:uploads/class-photos}")
    private String classPhotoRootDir;

    @Value("${attendance.class-photo.retention-days:30}")
    private long retentionDays;

    /**
     * 默认每月 1 号 03:20 清理一次过期班级照片
     */
    @Scheduled(cron = "${attendance.class-photo.cleanup-cron:0 20 3 1 * ?}", zone = "${attendance.timezone:Asia/Shanghai}")
    public void cleanupExpiredClassPhotos() {
        Path root = Paths.get(classPhotoRootDir).toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return;
        }

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deletedFiles = 0;

        try (Stream<Path> fileStream = Files.walk(root)) {
            List<Path> files = fileStream
                    .filter(Files::isRegularFile)
                    .toList();
            for (Path file : files) {
                try {
                    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
                    if (lastModifiedTime.toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                        deletedFiles++;
                    }
                } catch (Exception e) {
                    log.warn("Class photo cleanup skip file={}, reason={}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Class photo cleanup failed to walk dir={}, reason={}", root, e.getMessage(), e);
            return;
        }

        int deletedDirs = 0;
        try (Stream<Path> dirStream = Files.walk(root)) {
            List<Path> dirs = dirStream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path dir : dirs) {
                if (dir.equals(root)) {
                    continue;
                }
                try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                    if (!children.iterator().hasNext()) {
                        Files.deleteIfExists(dir);
                        deletedDirs++;
                    }
                } catch (Exception e) {
                    log.warn("Class photo cleanup skip dir={}, reason={}", dir, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Class photo cleanup failed to cleanup empty dirs, dir={}, reason={}", root, e.getMessage(), e);
            return;
        }

        log.info("Class photo cleanup finished, root={}, retentionDays={}, deletedFiles={}, deletedDirs={}",
                root, retentionDays, deletedFiles, deletedDirs);
    }
}
