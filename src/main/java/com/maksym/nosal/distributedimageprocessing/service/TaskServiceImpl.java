package com.maksym.nosal.distributedimageprocessing.service;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.Task;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.ImageRepository;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.TaskRepository;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private static final String QUEUE_NAME = "TASK_QUEUE";
    private final TaskRepository taskRepository;
    private final StringRedisTemplate redisTemplate;
    private final ImageRepository imageRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public String CreateAndSubmitTask(TaskSubmissionDto taskSubmissionDto, MultipartFile imageFile) throws IOException {
        Task task = new Task();
        var randomUUID = UUID.randomUUID().toString();
        task.setId(randomUUID);
        task.setStatus(TaskStatus.PENDING);
        task = taskRepository.save(task);

        var key = imageRepository.upload(randomUUID, imageFile.getInputStream(),
                imageFile.getSize(), imageFile.getContentType());

        SubmitTask(taskSubmissionDto.withS3ImageKey(key));
        return task.getId();
    }

    @Override
    public TaskStatusDto getTaskStatus(String taskId) {
        var t = taskRepository.getTaskById(taskId);
        if (t == null) {
            return new TaskStatusDto(null, TaskStatus.FAILED, "Task not found");
        }
        return new TaskStatusDto(t.getResultUri(), t.getStatus(), t.getErrorMessage());
    }

    private void SubmitTask(TaskSubmissionDto taskSubmissionDto) {
        String message = objectMapper.writeValueAsString(taskSubmissionDto);
        redisTemplate.opsForList().rightPush(QUEUE_NAME, message);
    }
}
