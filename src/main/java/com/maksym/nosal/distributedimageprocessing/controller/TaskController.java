package com.maksym.nosal.distributedimageprocessing.controller;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/task")
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/status/{taskId}")
    public ResponseEntity<?> getTaskStatus(@PathVariable String taskId) {
        var status = taskService.getTaskStatus(taskId);

        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
        }

        return ResponseEntity.ok(status);
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

    @GetMapping("/results/{taskId}")
    public ResponseEntity<?> getTaskResults(@PathVariable String taskId) {
        try {
            TaskStatusDto status = taskService.getTaskStatus(taskId);

            if (status == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Task not found"));
            }

            if (status.status() != TaskStatus.COMPLETED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Task is not completed yet", "status", status.status()));
            }

            if (status.resultUri() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Result file not found"));
            }

            InputStream imageStream = taskService.getTaskResult(taskId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentDispositionFormData("attachment", "processed-" + taskId + ".jpg");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(imageStream));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve result: " + e.getMessage()));
        }
    }
}
