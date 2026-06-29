package com.example.rental.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlannerStep {
    private String intent;
    private String businessTool;
    private JsonNode args;
    private String thought;
}
