package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class ItemPopularity {

    @JsonProperty("start_game_items")
    private Map<String, Integer> startGameItems;

    @JsonProperty("early_game_items")
    private Map<String, Integer> earlyGameItems;

    @JsonProperty("mid_game_items")
    private Map<String, Integer> midGameItems;

    @JsonProperty("late_game_items")
    private Map<String, Integer> lateGameItems;

    // Геттери та сеттери
    public Map<String, Integer> getStartGameItems() { return startGameItems; }
    public void setStartGameItems(Map<String, Integer> startGameItems) { this.startGameItems = startGameItems; }

    public Map<String, Integer> getEarlyGameItems() { return earlyGameItems; }
    public void setEarlyGameItems(Map<String, Integer> earlyGameItems) { this.earlyGameItems = earlyGameItems; }

    public Map<String, Integer> getMidGameItems() { return midGameItems; }
    public void setMidGameItems(Map<String, Integer> midGameItems) { this.midGameItems = midGameItems; }

    public Map<String, Integer> getLateGameItems() { return lateGameItems; }
    public void setLateGameItems(Map<String, Integer> lateGameItems) { this.lateGameItems = lateGameItems; }
}