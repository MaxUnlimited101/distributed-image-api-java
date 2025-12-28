package com.maksym.nosal.distributedimageprocessing.service.interfaces;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import org.springframework.web.multipart.MultipartFile;

public interface TaskService {
    String CreateAndSubmitTask(TaskSubmissionDto taskSubmissionDto, MultipartFile imageFile);
    TaskStatusDto getTaskStatus(String taskId);
}
