package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerWinLoss {

    private int win;
    private int lose;

    public int getWin() { return win; }
    public void setWin(int win) { this.win = win; }

    public int getLose() { return lose; }
    public void setLose(int lose) { this.lose = lose; }

    // Допоміжний метод для зручності
    public int getTotalGames() {
        return win + lose;
    }
}