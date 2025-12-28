package com.maksym.nosal.distributedimageprocessing.repository;

import com.maksym.nosal.distributedimageprocessing.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Task getTaskById(String taskId);
}
