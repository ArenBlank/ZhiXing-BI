package com.huang.zhixing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileParseResultDTO {

    private String content;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
