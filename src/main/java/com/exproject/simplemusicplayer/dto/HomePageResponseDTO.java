package com.exproject.simplemusicplayer.dto;

import lombok.*;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomePageResponseDTO {

    long trackCount;
    long albumCount;
    long artistCount;
    String userName;
    String welcomeMessage;
}
