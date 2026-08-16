package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HeroStats {

    private int id;

    @JsonProperty("localized_name")
    private String localizedName;

    @JsonProperty("primary_attr")
    private String primaryAttr;

    // Ранг 6 (Ancient)
    @JsonProperty("6_pick")
    private int ancientPick;
    @JsonProperty("6_win")
    private int ancientWin;

    // Ранг 7 (Divine)
    @JsonProperty("7_pick")
    private int divinePick;
    @JsonProperty("7_win")
    private int divineWin;

    // Ранг 8 (Immortal)
    @JsonProperty("8_pick")
    private int immortalPick;
    @JsonProperty("8_win")
    private int immortalWin;


    // Загальна кількість піків та перемог у пабліках
    @JsonProperty("pub_pick")
    private int pubPick;

    @JsonProperty("pub_win")
    private int pubWin;

    // Геттери та сеттери для них:
    public int getPubPick() { return pubPick; }
    public void setPubPick(int pubPick) { this.pubPick = pubPick; }

    public int getPubWin() { return pubWin; }
    public void setPubWin(int pubWin) { this.pubWin = pubWin; }
    // --- ЗРУЧНІ МЕТОДИ ДЛЯ АГРЕГАЦІЇ ХАЙ-ММР ---

    public int getHighRankPick() {
        return ancientPick + divinePick + immortalPick;
    }

    public int getHighRankWin() {
        return ancientWin + divineWin + immortalWin;
    }

    // --- СТАНДАРТНІ ГЕТТЕРИ ТА СЕТТЕРИ ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLocalizedName() { return localizedName; }
    public void setLocalizedName(String localizedName) { this.localizedName = localizedName; }

    public String getPrimaryAttr() { return primaryAttr; }
    public void setPrimaryAttr(String primaryAttr) { this.primaryAttr = primaryAttr; }

    public int getAncientPick() { return ancientPick; }
    public void setAncientPick(int ancientPick) { this.ancientPick = ancientPick; }

    public int getAncientWin() { return ancientWin; }
    public void setAncientWin(int ancientWin) { this.ancientWin = ancientWin; }

    public int getDivinePick() { return divinePick; }
    public void setDivinePick(int divinePick) { this.divinePick = divinePick; }

    public int getDivineWin() { return divineWin; }
    public void setDivineWin(int divineWin) { this.divineWin = divineWin; }

    public int getImmortalPick() { return immortalPick; }
    public void setImmortalPick(int immortalPick) { this.immortalPick = immortalPick; }

    public int getImmortalWin() { return immortalWin; }
    public void setImmortalWin(int immortalWin) { this.immortalWin = immortalWin; }
}