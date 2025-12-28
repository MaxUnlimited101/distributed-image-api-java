package com.maksym.nosal.distributedimageprocessing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class Task {
    @Id
    private String id;

    @Setter
    private TaskStatus status;

    @Setter
    private String errorMessage;

    @Setter
    private String resultUri;
}
