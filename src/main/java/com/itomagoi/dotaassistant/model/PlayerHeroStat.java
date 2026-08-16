package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerHeroStat {

    @JsonProperty("hero_id")
    private String heroId; // Зверни увагу, тут String!

    private int games;
    private int win;

    public String getHeroId() { return heroId; }
    public void setHeroId(String heroId) { this.heroId = heroId; }

    public int getGames() { return games; }
    public void setGames(int games) { this.games = games; }

    public int getWin() { return win; }
    public void setWin(int win) { this.win = win; }
}