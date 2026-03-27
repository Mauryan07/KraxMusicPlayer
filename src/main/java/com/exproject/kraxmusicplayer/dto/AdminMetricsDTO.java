package com.exproject.kraxmusicplayer.dto;

public record AdminMetricsDTO(
        Double storageUsedGb,
        Double storageFreeGb,
        Integer jobsRunning,
        String lastUploadAt
) {}