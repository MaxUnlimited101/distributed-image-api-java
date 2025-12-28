package com.maksym.nosal.distributedimageprocessing.service;

import com.maksym.nosal.distributedimageprocessing.dto.TaskStatusDto;
import com.maksym.nosal.distributedimageprocessing.dto.TaskSubmissionDto;
import com.maksym.nosal.distributedimageprocessing.model.Task;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import com.maksym.nosal.distributedimageprocessing.repository.TaskRepository;
import com.maksym.nosal.distributedimageprocessing.service.interfaces.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;

    @Override
    public String CreateAndSubmitTask(TaskSubmissionDto taskSubmissionDto, MultipartFile imageFile) {
        Task task = new Task();
        task.setStatus(TaskStatus.PENDING);
        task = taskRepository.save(task);
        SubmitTask(taskSubmissionDto, imageFile);
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

    private void SubmitTask(TaskSubmissionDto taskSubmissionDto, MultipartFile imageFile) {
        // TODO: submit task somewhere
    }
}
