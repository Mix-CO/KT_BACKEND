package com.kicktime.backend.repository;

import com.kicktime.backend.domain.model.Field;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, Long> {
}
