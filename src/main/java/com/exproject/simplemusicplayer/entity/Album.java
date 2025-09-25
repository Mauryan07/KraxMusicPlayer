package com.exproject.simplemusicplayer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "albums")
@Builder
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;


    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Track> tracks;

    @OneToOne(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    private Martwork artwork;
}
