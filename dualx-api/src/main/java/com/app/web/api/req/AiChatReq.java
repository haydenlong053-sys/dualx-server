package com.app.web.api.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatReq {

    @NotBlank
    @Size(max = 4000)
    private String message;
}
