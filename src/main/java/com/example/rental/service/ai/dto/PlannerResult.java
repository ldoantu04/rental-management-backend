package com.example.rental.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlannerResult {

    public enum Status { NEEDS_INFO, NEEDS_CONFIRM, ANSWER, ERROR }

    private Status status;
    private String text;
    private String hanhDong;
    private String moTaNgan;
    private JsonNode payload;
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

    public static PlannerResult needsConfirm(String hanhDong, String moTaNgan, JsonNode payload) {
        PlannerResult r = new PlannerResult();
        r.setStatus(Status.NEEDS_CONFIRM);
        r.setHanhDong(hanhDong);
        r.setMoTaNgan(moTaNgan);
        r.setPayload(payload);
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
