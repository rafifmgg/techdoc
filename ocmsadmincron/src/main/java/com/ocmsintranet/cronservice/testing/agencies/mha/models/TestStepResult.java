package com.ocmsintranet.cronservice.testing.agencies.mha.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class to store test step execution results.
 * Used to track the status and details of each test step.
 */
public class TestStepResult {
  private String stepName;
  private String status; // SUCCESS, FAILED, WARNING, SKIPPED
  private List<String> details;
  private String jsonData;

  /**
   * Constructor for TestStepResult.
   * 
   * @param stepName Name of the test step
   * @param status Status of the test step (SUCCESS, FAILED, WARNING, SKIPPED)
   */
  public TestStepResult(String stepName, String status) {
    this.stepName = stepName;
    this.status = status;
    this.details = new ArrayList<>();
  }

  /**
   * Add a detail message to the test step result.
   * 
   * @param detail Detail message to add
   */
  public void addDetail(String detail) {
    this.details.add(detail);
  }

  /**
   * Get the name of the test step.
   * 
   * @return The step name
   */
  public String getStepName() {
    return stepName;
  }

  /**
   * Set the name of the test step.
   * 
   * @param stepName The step name to set
   */
  public void setStepName(String stepName) {
    this.stepName = stepName;
  }

  /**
   * Get the status of the test step.
   * 
   * @return The status (SUCCESS, FAILED, WARNING, SKIPPED)
   */
  public String getStatus() {
    return status;
  }

  /**
   * Set the status of the test step.
   * 
   * @param status The status to set (SUCCESS, FAILED, WARNING, SKIPPED)
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Get the list of detail messages.
   * 
   * @return The list of details
   */
  public List<String> getDetails() {
    return details;
  }

  /**
   * Set the list of detail messages.
   * 
   * @param details The list of details to set
   */
  public void setDetails(List<String> details) {
    this.details = details;
  }

  /**
   * Get the JSON data associated with this test step.
   * 
   * @return The JSON data
   */
  public String getJsonData() {
    return jsonData;
  }

  /**
   * Set the JSON data associated with this test step.
   * 
   * @param jsonData The JSON data to set
   */
  public void setJsonData(String jsonData) {
    this.jsonData = jsonData;
  }
}
