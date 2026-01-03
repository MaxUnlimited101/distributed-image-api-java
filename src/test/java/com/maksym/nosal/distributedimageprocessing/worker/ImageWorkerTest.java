package com.maksym.nosal.distributedimageprocessing.worker;

import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.ImageRepository;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.TaskRepository;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.ImageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ImageProcessor imageProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private ImageWorker imageWorker;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void pullAndProcess_NoMessage_DoesNothing() throws Exception {
        // Arrange
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(null);

        // Act
        imageWorker.pullAndProcess();

        // Assert
        verify(imageProcessor, never()).process(any(), any(), any(), any());
        verify(taskRepository, never()).updateTaskStatus(any(), any(), any(), any());
    }

    @Test
    void pullAndProcess_ResizeTask_Success() throws Exception {
        // Arrange
        String message = "{\"action\":\"RESIZE\",\"width\":800,\"height\":600,\"s3ImageKey\":\"test-key\"}";
        TaskSubmissionDto dto = new TaskSubmissionDto("RESIZE", 800, 600, null, "test-key");
        byte[] originalData = "original image".getBytes();
        byte[] processedData = "processed image".getBytes();
        
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(message);
        when(objectMapper.readValue(message, TaskSubmissionDto.class)).thenReturn(dto);
        when(imageRepository.download("test-key")).thenReturn(new ByteArrayInputStream(originalData));
        when(imageProcessor.process(any(InputStream.class), eq("RESIZE"), eq(800), eq(600)))
            .thenReturn(processedData);
        when(imageRepository.upload(anyString(), any(InputStream.class), anyLong(), eq("image/*")))
            .thenReturn("processed-test-key");

        // Act
        imageWorker.pullAndProcess();

        // Assert
        verify(imageProcessor).process(any(InputStream.class), eq("RESIZE"), eq(800), eq(600));
        verify(imageRepository).upload(
            eq("processed-test-key"), 
            any(InputStream.class), 
            eq((long) processedData.length), 
            eq("image/*")
        );
        verify(taskRepository).updateTaskStatus(
            "test-key", 
            TaskStatus.COMPLETED, 
            "processed-test-key", 
            null
        );
    }

    @Test
    void pullAndProcess_GrayscaleTask_Success() throws Exception {
        // Arrange
        String message = "{\"action\":\"GRAYSCALE\",\"s3ImageKey\":\"grayscale-key\"}";
        TaskSubmissionDto dto = new TaskSubmissionDto("GRAYSCALE", null, null, null, "grayscale-key");
        byte[] processedData = "grayscale image".getBytes();
        
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(message);
        when(objectMapper.readValue(message, TaskSubmissionDto.class)).thenReturn(dto);
        when(imageRepository.download("grayscale-key"))
            .thenReturn(new ByteArrayInputStream("original".getBytes()));
        when(imageProcessor.process(any(InputStream.class), eq("GRAYSCALE"), isNull(), isNull()))
            .thenReturn(processedData);
        when(imageRepository.upload(anyString(), any(InputStream.class), anyLong(), eq("image/*")))
            .thenReturn("processed-grayscale-key");

        // Act
        imageWorker.pullAndProcess();

        // Assert
        verify(imageProcessor).process(any(InputStream.class), eq("GRAYSCALE"), isNull(), isNull());
        verify(taskRepository).updateTaskStatus(
            "grayscale-key", 
            TaskStatus.COMPLETED, 
            "processed-grayscale-key", 
            null
        );
    }

    @Test
    void pullAndProcess_ProcessingFails_UpdatesTaskAsFailed() throws Exception {
        // Arrange
        String message = "{\"action\":\"RESIZE\",\"s3ImageKey\":\"test-key\"}";
        TaskSubmissionDto dto = new TaskSubmissionDto("RESIZE", null, null, null, "test-key");
        Exception processingException = new RuntimeException("Image processing failed");
        
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(message);
        when(objectMapper.readValue(message, TaskSubmissionDto.class)).thenReturn(dto);
        when(imageRepository.download("test-key"))
            .thenReturn(new ByteArrayInputStream("original".getBytes()));
        when(imageProcessor.process(any(InputStream.class), any(), any(), any()))
            .thenThrow(processingException);

        // Act
        try {
            imageWorker.pullAndProcess();
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert
        verify(taskRepository).updateTaskStatus(
            eq("test-key"), 
            eq(TaskStatus.FAILED), 
            isNull(), 
            contains("Image processing failed")
        );
    }

    @Test
    void pullAndProcess_DownloadFails_UpdatesTaskAsFailed() throws Exception {
        // Arrange
        String message = "{\"action\":\"RESIZE\",\"s3ImageKey\":\"test-key\"}";
        TaskSubmissionDto dto = new TaskSubmissionDto("RESIZE", null, null, null, "test-key");
        
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(message);
        when(objectMapper.readValue(message, TaskSubmissionDto.class)).thenReturn(dto);
        when(imageRepository.download("test-key"))
            .thenThrow(new RuntimeException("Download failed"));

        // Act
        try {
            imageWorker.pullAndProcess();
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert
        verify(imageProcessor, never()).process(any(), any(), any(), any());
        verify(taskRepository).updateTaskStatus(
            eq("test-key"), 
            eq(TaskStatus.FAILED), 
            isNull(), 
            any()
        );
    }

    @Test
    void pullAndProcess_UploadFails_UpdatesTaskAsFailed() throws Exception {
        // Arrange
        String message = "{\"action\":\"RESIZE\",\"s3ImageKey\":\"test-key\"}";
        TaskSubmissionDto dto = new TaskSubmissionDto("RESIZE", null, null, null, "test-key");
        byte[] processedData = "processed".getBytes();
        
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(message);
        when(objectMapper.readValue(message, TaskSubmissionDto.class)).thenReturn(dto);
        when(imageRepository.download("test-key"))
            .thenReturn(new ByteArrayInputStream("original".getBytes()));
        when(imageProcessor.process(any(InputStream.class), any(), any(), any()))
            .thenReturn(processedData);
        when(imageRepository.upload(anyString(), any(InputStream.class), anyLong(), eq("image/*")))
            .thenThrow(new RuntimeException("Upload failed"));

        // Act
        try {
            imageWorker.pullAndProcess();
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert
        verify(taskRepository).updateTaskStatus(
            eq("test-key"), 
            eq(TaskStatus.FAILED), 
            isNull(), 
            contains("Upload failed")
        );
    }

    @Test
    void pullAndProcess_InvalidJson_HandlesGracefully() throws Exception {
        // Arrange
        String invalidMessage = "invalid json";
        
        when(listOperations.leftPop("image_tasks_queue")).thenReturn(invalidMessage);
        when(objectMapper.readValue(invalidMessage, TaskSubmissionDto.class))
            .thenThrow(new RuntimeException("JSON parse error"));

        // Act & Assert
        try {
            imageWorker.pullAndProcess();
        } catch (RuntimeException e) {
            // Expected
        }
        
        verify(imageProcessor, never()).process(any(), any(), any(), any());
    }
}
