package com.maksym.nosal.distributedimageprocessing.dto;

import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;

public record TaskStatusDto(
        String resultUri,
        TaskStatus status,
        String error
) { }
