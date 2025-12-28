package com.maksym.nosal.distributedimageprocessing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Task {
    @Id
    private String id;

    private TaskStatus status;

    private String errorMessage;

    private String resultUri;
}
