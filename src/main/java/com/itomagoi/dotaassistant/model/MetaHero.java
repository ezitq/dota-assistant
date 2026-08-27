package com.itomagoi.dotaassistant.model;

public class MetaHero {

    private double winRate;
    private String heroName;
    private int matchCount;

    public MetaHero(double winRate, String heroName,int matchCount) {
        this.winRate = winRate;
        this.heroName = heroName;
        this.matchCount = matchCount;
    }

    public MetaHero() {
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public String getHeroName() {
        return heroName;
    }

    public void setHeroName(String heroName) {
        this.heroName = heroName;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }
}
