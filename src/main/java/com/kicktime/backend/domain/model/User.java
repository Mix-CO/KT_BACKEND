package com.kicktime.backend.domain.model;

import com.kicktime.backend.domain.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @ManyToOne
    private Team team;

    @OneToMany(mappedBy = "user")
    private List<Availability> availability;

    @Column(unique = true)
    private String studentId;

}