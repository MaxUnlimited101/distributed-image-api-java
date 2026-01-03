package com.maksym.nosal.distributedimageprocessing.controller;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void getTaskStatus_Success() throws Exception {
        // Arrange
        String taskId = "test-task-123";
        TaskStatusDto statusDto = new TaskStatusDto("result-uri", TaskStatus.COMPLETED, null);
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);

        // Act & Assert
        mockMvc.perform(get("/api/task/status/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.resultUri").value("result-uri"));
        
        verify(taskService).getTaskStatus(taskId);
    }

    @Test
    void getTaskStatus_PendingTask() throws Exception {
        // Arrange
        String taskId = "pending-task";
        TaskStatusDto statusDto = new TaskStatusDto(null, TaskStatus.PENDING, null);
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);

        // Act & Assert
        mockMvc.perform(get("/api/task/status/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.resultUri").doesNotExist());
    }

    @Test
    void getTaskStatus_FailedTask() throws Exception {
        // Arrange
        String taskId = "failed-task";
        TaskStatusDto statusDto = new TaskStatusDto(null, TaskStatus.FAILED, "Processing error");
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);

        // Act & Assert
        mockMvc.perform(get("/api/task/status/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.error").value("Processing error"));
    }

    @Test
    void submitTask_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test-image.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        MockMultipartFile taskJson = new MockMultipartFile(
            "task",
            "",
            "application/json",
            "{\"action\":\"RESIZE\",\"width\":800,\"height\":600}".getBytes()
        );

        when(taskService.CreateAndSubmitTask(any(), any())).thenReturn("task-123");

        // Act & Assert
        mockMvc.perform(multipart("/api/task/submit")
                .file(file)
                .file(taskJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value("task-123"))
            .andExpect(jsonPath("$.status").value("submitted"));
        
        verify(taskService).CreateAndSubmitTask(any(), any());
    }

    @Test
    void submitTask_EmptyFile_ReturnsBadRequest() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            new byte[0]
        );
        
        MockMultipartFile taskJson = new MockMultipartFile(
            "task",
            "",
            "application/json",
            "{\"action\":\"RESIZE\",\"width\":800,\"height\":600}".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/task/submit")
                .file(emptyFile)
                .file(taskJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("File is empty"));
        
        verify(taskService, never()).CreateAndSubmitTask(any(), any());
    }

    @Test
    void submitTask_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            "image content".getBytes()
        );
        
        MockMultipartFile taskJson = new MockMultipartFile(
            "task",
            "",
            "application/json",
            "{\"action\":\"RESIZE\"}".getBytes()
        );

        when(taskService.CreateAndSubmitTask(any(), any()))
            .thenThrow(new RuntimeException("Internal error"));

        // Act & Assert
        mockMvc.perform(multipart("/api/task/submit")
                .file(file)
                .file(taskJson))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Failed to process file: Internal error"));
    }

    @Test
    void submitTask_InvalidArgument_ReturnsBadRequest() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            "image content".getBytes()
        );
        
        MockMultipartFile taskJson = new MockMultipartFile(
            "task",
            "",
            "application/json",
            "{\"action\":\"INVALID\"}".getBytes()
        );

        when(taskService.CreateAndSubmitTask(any(), any()))
            .thenThrow(new IllegalArgumentException("Invalid action"));

        // Act & Assert
        mockMvc.perform(multipart("/api/task/submit")
                .file(file)
                .file(taskJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid action"));
    }

    @Test
    void getTaskResults_CompletedTask_ReturnsImage() throws Exception {
        // Arrange
        String taskId = "completed-task";
        TaskStatusDto statusDto = new TaskStatusDto("result-uri", TaskStatus.COMPLETED, null);
        byte[] imageData = "processed image data".getBytes();
        
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);
        when(taskService.getTaskResult(taskId)).thenReturn(new ByteArrayInputStream(imageData));

        // Act & Assert
        mockMvc.perform(get("/api/task/results/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType("image/jpeg"));
        
        verify(taskService).getTaskStatus(taskId);
        verify(taskService).getTaskResult(taskId);
    }

    @Test
    void getTaskResults_PendingTask_ReturnsBadRequest() throws Exception {
        // Arrange
        String taskId = "pending-task";
        TaskStatusDto statusDto = new TaskStatusDto(null, TaskStatus.PENDING, null);
        
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);

        // Act & Assert
        mockMvc.perform(get("/api/task/results/{taskId}", taskId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Task is not completed yet"))
            .andExpect(jsonPath("$.status").value("PENDING"));
        
        verify(taskService).getTaskStatus(taskId);
        verify(taskService, never()).getTaskResult(any());
    }

    @Test
    void getTaskResults_ProcessingTask_ReturnsBadRequest() throws Exception {
        // Arrange
        String taskId = "processing-task";
        TaskStatusDto statusDto = new TaskStatusDto(null, TaskStatus.PENDING, null);
        
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);

        // Act & Assert
        mockMvc.perform(get("/api/task/results/{taskId}", taskId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Task is not completed yet"))
            .andExpect(jsonPath("$.status").value(TaskStatus.PENDING.name()));
    }

    @Test
    void getTaskResults_FailedTask_ReturnsBadRequest() throws Exception {
        // Arrange
        String taskId = "failed-task";
        TaskStatusDto statusDto = new TaskStatusDto(null, TaskStatus.FAILED, "Processing failed");
        
        when(taskService.getTaskStatus(taskId)).thenReturn(statusDto);

        // Act & Assert
        mockMvc.perform(get("/api/task/results/{taskId}", taskId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Task is not completed yet"))
            .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
