package com.leansixlab.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AIResponseMetaData {
    private Boolean isInformationFound;
    private String[] metadata;
}
