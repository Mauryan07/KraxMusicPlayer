package com.exproject.kraxmusicplayer.service.impl;

import com.exproject.kraxmusicplayer.service.HlsTranscodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class HlsTranscodeServiceImpl implements HlsTranscodeService {

    private static final String PLAYLIST = "playlist.m3u8";

    @Value("${ffmpeg.binary:ffmpeg}")
    private String ffmpegBinary;

    @Value("${ffmpeg.timeoutSeconds:300}")
    private int timeoutSeconds;

    @Override
    public Path transcodeToHls(Path sourceFile, Path targetDir) {
        try {
            Files.createDirectories(targetDir);
            Path playlistPath = targetDir.resolve(PLAYLIST);
            Path segmentPattern = targetDir.resolve("segment_%03d.ts");

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegBinary, "-y",
                    "-i", sourceFile.toAbsolutePath().toString(),
                    "-vn",
                    "-ac", "2", "-ar", "48000",
                    "-c:a", "aac", "-b:a", "80k",
                    "-hls_time", "5",
                    "-hls_flags", "split_by_time+independent_segments",
                    "-hls_segment_type", "mpegts",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", segmentPattern.toString(),
                    playlistPath.toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Log ffmpeg output to avoid buffer blocking
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        System.out.println("[ffmpeg] " + line);
                    }
                } catch (Exception ignored) {
                }
            }).start();

            boolean finished = p.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IllegalStateException("FFmpeg timed out after " + timeoutSeconds + "s");
            }
            int exit = p.exitValue();
            boolean ok = exit == 0 &&
                    Files.exists(playlistPath) &&
                    Files.list(targetDir).anyMatch(f -> f.getFileName().toString().startsWith("segment_"));

            if (!ok) {
                throw new IllegalStateException("HLS generation failed, exit=" + exit);
            }
            return playlistPath;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HLS transcode failed: " + e.getMessage(), e);
        }
    }
}