package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecentMatch {

    @JsonProperty("match_id")
    private long matchId;

    @JsonProperty("hero_id")
    private int heroId;

    @JsonProperty("game_mode")
    private Integer gameMode;

    private int kills;
    private int deaths;
    private int assists;

    @JsonProperty("lane_role")
    private Integer laneRole; // Може бути null, тому використовуємо Integer

    @JsonProperty("radiant_win")
    private boolean radiantWin;

    @JsonProperty("player_slot")
    private int playerSlot;

    // --- Допоміжний метод для визначення перемоги конкретного гравця ---
    public boolean isPlayerWin() {
        boolean isRadiant = playerSlot < 128; // В OpenDota слоти 0-127 це Radiant, 128-255 це Dire
        return isRadiant == radiantWin;
    }

    // Стандартні геттери та сеттери
    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public int getHeroId() { return heroId; }
    public void setHeroId(int heroId) { this.heroId = heroId; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public Integer getLaneRole() { return laneRole; }
    public void setLaneRole(Integer laneRole) { this.laneRole = laneRole; }

    public boolean isRadiantWin() { return radiantWin; }
    public void setRadiantWin(boolean radiantWin) { this.radiantWin = radiantWin; }

    public int getPlayerSlot() { return playerSlot; }
    public void setPlayerSlot(int playerSlot) { this.playerSlot = playerSlot; }

    public Integer getGameMode() {
        return gameMode;
    }

    public void setGameMode(Integer gameMode) {
        this.gameMode = gameMode;
    }
}