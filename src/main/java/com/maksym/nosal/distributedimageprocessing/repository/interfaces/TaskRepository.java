package com.maksym.nosal.distributedimageprocessing.repository.interfaces;

import com.maksym.nosal.distributedimageprocessing.model.Task;
import com.maksym.nosal.distributedimageprocessing.model.TaskStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Task getTaskById(String taskId);

    @Modifying
    @Transactional
    @Query("UPDATE ImageTask t SET t.status = :status WHERE t.taskId = :id")
    void updateTaskStatus(@Param("id") String id, @Param("status") TaskStatus status);
}

