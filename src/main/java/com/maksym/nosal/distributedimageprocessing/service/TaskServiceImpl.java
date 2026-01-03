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
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private static final String QUEUE_NAME = "image_tasks_queue";
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

        var key = imageRepository.upload(randomUUID, imageFile.getInputStream(),
                imageFile.getSize(), imageFile.getContentType());

        task = taskRepository.save(task);
        SubmitTask(taskSubmissionDto.withS3ImageKey(key));
        return task.getId();
    }

    @Override
    public TaskStatusDto getTaskStatus(String taskId) {
        var t = taskRepository.getTaskById(taskId);
        if (t == null) {
            return null;
        }
        return new TaskStatusDto(t.getResultUri(), t.getStatus(), t.getErrorMessage());
    }

    private void SubmitTask(TaskSubmissionDto taskSubmissionDto) {
        String message = objectMapper.writeValueAsString(taskSubmissionDto);
        redisTemplate.opsForList().rightPush(QUEUE_NAME, message);
    }

    @Override
    public InputStream getTaskResult(String taskId) {
        Task task = taskRepository.getTaskById(taskId);
        if (task == null || task.getResultUri() == null) {
            throw new IllegalArgumentException("Task not found or result not available");
        }
        return imageRepository.download(task.getResultUri());
    }
}
