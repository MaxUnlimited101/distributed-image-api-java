package com.maksym.nosal.distributedimageprocessing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TaskSubmissionDto(
        @NotBlank(message = "Action is required")
        @Pattern(regexp = "RESIZE|GRAYSCALE|WATERMARK", message = "Invalid action type")
        String action,

        Integer width,
        Integer height,
        String watermarkText
) {}
