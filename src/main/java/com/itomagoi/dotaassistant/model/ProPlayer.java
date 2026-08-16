package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProPlayer {

    @JsonProperty("account_id")
    private Long accountId; // Змінено з long на Long

    @JsonProperty("games_played")
    private Integer gamesPlayed; // Змінено з int на Integer

    private Integer wins; // Змінено з int на Integer

    // --- Геттери та сеттери ---

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Integer getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(Integer gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
}