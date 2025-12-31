package com.maksym.nosal.distributedimageprocessing.controller;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/task")
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/status/{taskId}")
    public TaskStatusDto GetTaskStatus(@PathVariable String taskId) {
        return taskService.getTaskStatus(taskId);
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitTask(
            @RequestPart("task") TaskSubmissionDto dto,
            @RequestPart("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }
            String taskId = taskService.CreateAndSubmitTask(dto, file);
            return ResponseEntity.ok(Map.of("taskId", taskId, "status", "submitted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process file: " + e.getMessage()));
        }
    }
}
