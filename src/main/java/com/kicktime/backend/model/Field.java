package com.kicktime.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
public class Field {
    @Id
    public int number;
}
