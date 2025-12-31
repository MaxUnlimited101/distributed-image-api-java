package com.maksym.nosal.distributedimageprocessing.worker;

import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.ImageRepository;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.TaskRepository;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.ImageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@Profile("worker")
@RequiredArgsConstructor
public class ImageWorker {

    private final StringRedisTemplate redisTemplate;
    private final ImageRepository imageRepository;
    private final TaskRepository taskRepository;
    private final ImageProcessor imageProcessor;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void pullAndProcess() {
        String message = redisTemplate.opsForList().leftPop("image_tasks_queue");
        if (message == null) {
            return;
        }

        var dto = objectMapper.readValue(message, TaskSubmissionDto.class);

        InputStream original = imageRepository.download(dto.s3ImageKey());


        byte[] processedData = null;
        try {
            processedData = imageProcessor.process(original, dto.action(), dto.width(), dto.height());
        } catch (Exception e) {
            taskRepository.updateTaskStatus(dto.s3ImageKey(), TaskStatus.FAILED, null, e.getMessage());
            throw new RuntimeException(e);
        }

        String resultKey = imageRepository.upload("processed-" + dto.s3ImageKey(),
                new ByteArrayInputStream(processedData), processedData.length, "image/*");

        taskRepository.updateTaskStatus(dto.s3ImageKey(), TaskStatus.COMPLETED, resultKey, null);
    }
}
