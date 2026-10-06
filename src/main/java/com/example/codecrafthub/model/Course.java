package com.example.codecrafthub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A course a developer wants to learn.
 *
 * Jackson turns this class into JSON (and back) using the getters and setters.
 * Java fields use camelCase; @JsonProperty sets the snake_case name used in JSON.
 */
// Jackson 3 sorts fields alphabetically by default; this keeps them in a logical order.
@JsonPropertyOrder({"id", "name", "description", "target_date", "status", "created_at"})
public class Course {

    // Set by the server (1, 2, 3, ...). Any id sent by the client is ignored.
    private Long id;

    private String name;

    private String description;

    // Stored as text in the format YYYY-MM-DD, e.g. "2026-12-31".
    @JsonProperty("target_date")
    private String targetDate;

    // Must be exactly "Not Started", "In Progress" or "Completed".
    private String status;

    // Set by the server when the course is created, e.g. "2026-10-06T09:43:35.366Z".
    @JsonProperty("created_at")
    private String createdAt;

    // Jackson needs an empty constructor to create objects from JSON.
    public Course() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
