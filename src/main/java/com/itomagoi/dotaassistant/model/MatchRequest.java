package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class MatchRequest{

    @JsonProperty("match_id")
    private Integer id;

    @JsonProperty("start_time")
    private Long date;

    public MatchRequest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }
}
