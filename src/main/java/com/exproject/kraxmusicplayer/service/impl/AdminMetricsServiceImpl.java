package com.exproject.kraxmusicplayer.service.impl;

import com.exproject.kraxmusicplayer.dto.AdminMetricsDTO;
import com.exproject.kraxmusicplayer.service.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminMetricsServiceImpl implements AdminMetricsService {

    @Value("${track.storage.location}")
    private String storagePath;

    // Simple job counter (wire it to ffmpeg jobs later)
    private final TranscodeJobTracker transcodeJobTracker;

    @Override
    public AdminMetricsDTO getMetrics() {
        Path root = Paths.get(storagePath);

        Double usedGb = null;
        Double freeGb = null;
        String lastUploadAt = null;

        try {
            Files.createDirectories(root);

            FileStore store = Files.getFileStore(root);
            long total = store.getTotalSpace();
            long unallocated = store.getUnallocatedSpace();
            long used = total - unallocated;

            usedGb = bytesToGb(used);
            freeGb = bytesToGb(unallocated);

            Optional<Instant> lastModified = findLastModifiedInstant(root);
            lastUploadAt = lastModified.map(Instant::toString).orElse(null);

        } catch (Exception ignored) {
            // return nulls; frontend will show —
        }

        return new AdminMetricsDTO(
                usedGb,
                freeGb,
                transcodeJobTracker.getRunningJobs(),
                lastUploadAt
        );
    }

    private static double bytesToGb(long bytes) {
        return Math.round((bytes / 1024d / 1024d / 1024d) * 100.0) / 100.0;
    }

    private Optional<Instant> findLastModifiedInstant(Path root) throws IOException {
        // Scan only files (playlist/segments), get max lastModifiedTime
        try (var walk = Files.walk(root)) {
            return walk
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant();
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder());
        }
    }
}