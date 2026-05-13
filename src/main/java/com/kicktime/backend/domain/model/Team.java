package com.kicktime.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne
    private User captain;

    @OneToMany(mappedBy = "team")
    private List<User> players;

    @ManyToOne
    private Tournament tournament;

    private String logoUrl;

}