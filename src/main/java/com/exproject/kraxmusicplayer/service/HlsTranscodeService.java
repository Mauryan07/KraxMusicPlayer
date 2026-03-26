package com.exproject.kraxmusicplayer.service;

import java.nio.file.Path;

public interface HlsTranscodeService {
    /**
     * Transcodes a source audio file to HLS inside targetDir.
     * Returns the generated playlist path.
     */
    Path transcodeToHls(Path sourceFile, Path targetDir);
}