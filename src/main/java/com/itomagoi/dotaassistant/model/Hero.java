package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Hero {
    private int id;
    private String name;

    @JsonProperty("localized_name")
    private String localizedName;

    private String primaryAttr;
    private String attackType;

    // Геттери та сеттери
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocalizedName() { return localizedName; }
    public void setLocalizedName(String localizedName) { this.localizedName = localizedName; }

    public String getPrimaryAttr() { return primaryAttr; }
    public void setPrimaryAttr(String primaryAttr) { this.primaryAttr = primaryAttr; }

    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }
}