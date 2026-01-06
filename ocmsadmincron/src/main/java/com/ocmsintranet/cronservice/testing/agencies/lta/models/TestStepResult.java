package com.ocmsintranet.cronservice.testing.agencies.lta.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model class to represent the result of a test step in the LTA test flow.
 */
public class TestStepResult {
    private String step;
    private String status; // "SUCCESS", "FAILED", "SKIPPED"
    private List<String> detail;
    private Object jsonData; // Untuk menyimpan data JSON sebagai objek

    public TestStepResult() {
        this.detail = new ArrayList<>();
    }

    public TestStepResult(String step, String status) {
        this.step = step;
        this.status = status;
        this.detail = new ArrayList<>();
    }

    public TestStepResult(String step, String status, List<String> detail) {
        this.step = step;
        this.status = status;
        this.detail = detail;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDetail() {
        return detail;
    }

    public void setDetail(List<String> detail) {
        this.detail = detail;
    }

    public void addDetail(String detailItem) {
        if (this.detail == null) {
            this.detail = new ArrayList<>();
        }
        this.detail.add(detailItem);
    }

    public Object getJsonData() {
        return jsonData;
    }

    public void setJsonData(Object jsonData) {
        this.jsonData = jsonData;
    }
}
