package com.maksym.nosal.distributedimageprocessing.service;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.Task;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.ImageRepository;
import com.maksym.nosal.distributedimageprocessing.repository.interfaces.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private TaskServiceImpl taskService;

    private void setUpMockRedis() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void createAndSubmitTask_Success() throws Exception {
        // Arrange
        setUpMockRedis();
        TaskSubmissionDto dto = new TaskSubmissionDto("RESIZE", 800, 600, null, null);
        MultipartFile file = mock(MultipartFile.class);
        byte[] imageData = "test image content".getBytes();
        
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(imageData));
        when(file.getSize()).thenReturn((long) imageData.length);
        when(file.getContentType()).thenReturn("image/jpeg");

        Task savedTask = new Task();
        savedTask.setId("test-uuid-123");
        savedTask.setStatus(TaskStatus.PENDING);
        
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        when(imageRepository.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
            .thenReturn("s3-key-123");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"action\":\"RESIZE\"}");

        // Act
        String taskId = taskService.CreateAndSubmitTask(dto, file);

        // Assert
        assertNotNull(taskId);
        assertEquals("test-uuid-123", taskId);
        verify(taskRepository).save(any(Task.class));
        verify(imageRepository).upload(anyString(), any(InputStream.class), anyLong(), eq("image/jpeg"));
        verify(listOperations).rightPush(eq("image_tasks_queue"), anyString());
    }

    @Test
    void createAndSubmitTask_ThrowsException_WhenFileReadFails() throws Exception {
        // Arrange
        TaskSubmissionDto dto = new TaskSubmissionDto("RESIZE", 800, 600, null, null);
        MultipartFile file = mock(MultipartFile.class);
        
        when(file.getInputStream()).thenThrow(new IOException("Failed to read file"));

        // Act & Assert
        assertThrows(IOException.class, () -> taskService.CreateAndSubmitTask(dto, file));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void getTaskStatus_TaskExists_ReturnsStatus() {
        // Arrange
        String taskId = "test-task-id";
        Task task = new Task();
        task.setId(taskId);
        task.setStatus(TaskStatus.COMPLETED);
        task.setResultUri("result-s3-key");
        
        when(taskRepository.getTaskById(taskId)).thenReturn(task);

        // Act
        TaskStatusDto result = taskService.getTaskStatus(taskId);

        // Assert
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.status());
        assertEquals("result-s3-key", result.resultUri());
        assertNull(result.error());
        verify(taskRepository).getTaskById(taskId);
    }

    @Test
    void getTaskStatus_TaskNotFound_ReturnsNull() {
        // Arrange
        String taskId = "non-existent-task";
        when(taskRepository.getTaskById(taskId)).thenReturn(null);

        // Act
        TaskStatusDto result = taskService.getTaskStatus(taskId);

        // Assert
        assertNull(result);
    }

    @Test
    void getTaskStatus_PendingTask_ReturnsNullResultUri() {
        // Arrange
        String taskId = "pending-task";
        Task task = new Task();
        task.setId(taskId);
        task.setStatus(TaskStatus.PENDING);
        
        when(taskRepository.getTaskById(taskId)).thenReturn(task);

        // Act
        TaskStatusDto result = taskService.getTaskStatus(taskId);

        // Assert
        assertNotNull(result);
        assertEquals(TaskStatus.PENDING, result.status());
        assertNull(result.resultUri());
    }

    @Test
    void getTaskResult_Success_ReturnsInputStream() {
        // Arrange
        String taskId = "test-task-id";
        Task task = new Task();
        task.setId(taskId);
        task.setResultUri("result-key");
        
        ByteArrayInputStream expectedStream = new ByteArrayInputStream("processed image".getBytes());
        
        when(taskRepository.getTaskById(taskId)).thenReturn(task);
        when(imageRepository.download("result-key")).thenReturn(expectedStream);

        // Act
        InputStream result = taskService.getTaskResult(taskId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedStream, result);
        verify(imageRepository).download("result-key");
    }

    @Test
    void getTaskResult_TaskNotFound_ThrowsException() {
        // Arrange
        String taskId = "non-existent";
        when(taskRepository.getTaskById(taskId)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> taskService.getTaskResult(taskId));
        verify(imageRepository, never()).download(any());
    }

    @Test
    void getTaskResult_TaskWithoutResult_ThrowsException() {
        // Arrange
        String taskId = "task-without-result";
        Task task = new Task();
        task.setId(taskId);
        task.setResultUri(null);
        
        when(taskRepository.getTaskById(taskId)).thenReturn(task);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> taskService.getTaskResult(taskId));
        verify(imageRepository, never()).download(any());
    }
}
