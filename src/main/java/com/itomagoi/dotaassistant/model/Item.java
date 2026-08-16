package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    private Integer id;
    private String dname;

    public Item() {
    }

    public Item(Integer id, String dname) {
        this.id = id;
        this.dname = dname;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDname() {
        return dname;
    }

    public void setName(String dname) {
        this.dname = dname;
    }
}
