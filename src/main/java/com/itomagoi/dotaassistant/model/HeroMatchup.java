package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroMatchup {

    @JsonProperty("hero_id")
    private int heroId;

    @JsonProperty("games_played")
    private int gamesPlayed;

    private int wins;

    // Геттери та сеттери
    public int getHeroId() { return heroId; }
    public void setHeroId(int heroId) { this.heroId = heroId; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
}