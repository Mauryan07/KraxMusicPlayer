package com.exproject.kraxmusicplayer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;

@Entity
@Table(name = "tracks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {
    @Id
    private long fileHash;
    @Column(nullable = false, unique = true)
    private String title;
    private String duration;
    @Column(nullable = false, unique = true)
    private String filePath;
    private String bitrate;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "artist_id")
    private Artist artist;


    //timestamp

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalTime created;

    @UpdateTimestamp
    private LocalTime updated;


}
