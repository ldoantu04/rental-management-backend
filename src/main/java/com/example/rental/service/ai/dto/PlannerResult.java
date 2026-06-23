package com.example.rental.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlannerResult {

    public enum Status { NEEDS_INFO, ANSWER, ERROR }

    private Status status;
    private String text;
    private List<Map<String, Object>> trace = new ArrayList<>();
    private Map<String, Object> context = new HashMap<>();

    public static PlannerResult answer(String text) {
        PlannerResult r = new PlannerResult();
        r.setStatus(Status.ANSWER);
        r.setText(text);
        return r;
    }

    public static PlannerResult needsInfo(String text) {
        PlannerResult r = new PlannerResult();
        r.setStatus(Status.NEEDS_INFO);
        r.setText(text);
        return r;
    }

    public static PlannerResult error(String text) {
        PlannerResult r = new PlannerResult();
        r.setStatus(Status.ERROR);
        r.setText(text);
        return r;
    }

    public PlannerResult addTrace(String step, Object detail) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("step", step);
        entry.put("detail", detail);
        trace.add(entry);
        return this;
    }
}
