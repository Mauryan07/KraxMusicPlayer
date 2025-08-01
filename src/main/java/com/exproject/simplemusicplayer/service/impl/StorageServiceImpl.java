package com.exproject.simplemusicplayer.service.impl;


import com.exproject.simplemusicplayer.model.Album;
import com.exproject.simplemusicplayer.model.Artist;
import com.exproject.simplemusicplayer.model.Martwork;
import com.exproject.simplemusicplayer.model.Track;
import com.exproject.simplemusicplayer.repository.AlbumRepository;
import com.exproject.simplemusicplayer.repository.ArtistRepository;
import com.exproject.simplemusicplayer.repository.ArtworkRepository;
import com.exproject.simplemusicplayer.repository.TrackRepository;
import com.exproject.simplemusicplayer.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {


    @Value("${file.storage.location}")
    private String storagePath;

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(storagePath));
    }

    @Override
    public void store(MultipartFile multipartFile) {
        try {
            // Save file
            Path destination = Paths.get(storagePath, multipartFile.getOriginalFilename());
            Files.copy(multipartFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // Get file name without extension
            String originalFileName = multipartFile.getOriginalFilename();
            String fileNameWithoutExtension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                    : originalFileName;

            // Read metadata
            MP3File audioFile = (MP3File) AudioFileIO.read(destination.toFile());
            Tag tag = audioFile.getTag() != null ? audioFile.getTag() : audioFile.getID3v1Tag();
            MP3AudioHeader audioHeader = (MP3AudioHeader) audioFile.getAudioHeader();

            String title = fileNameWithoutExtension;    // tag.getFirst(FieldKey.ARTIST);
            String duration = audioHeader.getTrackLengthAsString();
            String bitrate = audioHeader.getBitRate();
            String hashCode = "" + tag.hashCode();


            // 3. Extract artist and album

            //Read and add Artist and album name to there table
            String rawArtistName = tag.getFirst(FieldKey.ARTIST);
            final String artistName = rawArtistName.isBlank() ? "Unknown Artist" : rawArtistName;

            Artist artist = artistRepository.findByNameIgnoreCase(artistName)
                    .orElseGet(() -> artistRepository.save(Artist.builder().name(artistName).build()));


            String rawAlbumName = tag.getFirst(FieldKey.ALBUM);
            final String albumName = rawAlbumName.isBlank() ? "Unknown Album" : rawAlbumName;

            Album album = albumRepository.findByNameIgnoreCase(albumName)
                    .orElseGet(() -> albumRepository.save(Album.builder().name(albumName).build()));


            // Music  Extract artwork
            byte[] imageData = null;
            String mimeType = null;
            Artwork artwork = tag.getFirstArtwork();
            if (Objects.isNull(artwork)) {
                System.out.println("Artwork is null for file: " + originalFileName);
            } else {
                try {
                    imageData = artwork.getBinaryData();
                    mimeType = artwork.getMimeType();
                    System.out.println("Artwork extracted, MIME type: " + mimeType);
                } catch (Exception e) {
                    System.out.println("Error extracting artwork: " + e.getMessage());
                }
            }

            // Save or update artwork
            if (imageData != null && mimeType != null) {
                Martwork existingArtwork =  album.getArtwork();
                if (existingArtwork == null) {
                    Martwork newArtwork = Martwork.builder()
                            .album(album)
                            .imageData(imageData)
                            .mimeType(mimeType)
                            .build();
                    artworkRepository.save(newArtwork);
                    album.setArtwork(newArtwork);
                    albumRepository.save(album);
                } else if (existingArtwork.getImageData() == null) {
                    existingArtwork.setImageData(imageData);
                    existingArtwork.setMimeType(mimeType);
                    artworkRepository.save(existingArtwork);
                }
            }





// 4. Save track

            Track track = trackRepository.findByTitleIgnoreCase(title)
                    .orElseGet(() -> trackRepository.save(Track.builder()
                            .fileHash(hashCode)
                            .title(title)
                            .duration(duration)
                            .bitrate(bitrate)
                            .filePath(destination.toAbsolutePath().toString())
                            .artist(artist)
                            .album(album)
                            .build()));



            //
            System.out.println("Title: " + title + " \nduration:" + duration + "\nartist:" + artist.getName() + "\nalbum: " + album.getName() + "\nBitrate: " + bitrate);
            //

        } catch (
                Exception e) {
            throw new RuntimeException("Failed to store file: " + multipartFile.getOriginalFilename(), e);
        }
    }

}
