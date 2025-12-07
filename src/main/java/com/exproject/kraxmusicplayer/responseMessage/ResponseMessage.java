package com.exproject.kraxmusicplayer.responseMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseMessage {
    private Integer statusCode;
    private String statusMessage;
    private String message;
    private Object data;
}
