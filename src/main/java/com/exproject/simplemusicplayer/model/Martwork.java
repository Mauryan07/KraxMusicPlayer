package com.exproject.simplemusicplayer.model;

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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "album_id", unique = true)
    private Album album;

    private byte[] imageData;

    private String mimeType;
}
