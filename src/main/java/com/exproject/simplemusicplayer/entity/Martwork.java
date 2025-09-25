package com.exproject.simplemusicplayer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artwork")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Martwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "album_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Album album;

    private byte[] imageData;

    private String mimeType;
}
