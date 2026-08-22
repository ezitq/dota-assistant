package com.itomagoi.dotaassistant.model;

import java.util.List;

public class PostMatchSummary {

    private long matchId;
    private boolean radiantWin;
    private int durationSeconds; // Тривалість у секундах
    private int radiantScore;
    private int direScore;
    private long startTime; // Unix timestamp
    private String skillBracket; // Наприклад: "Normal Skill", "High Skill", "Very High Skill"

    private List<MatchPlayerDetail> radiantPlayers;
    private List<MatchPlayerDetail> direPlayers;

    // --- Геттери та Сеттери ---

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public boolean isRadiantWin() { return radiantWin; }
    public void setRadiantWin(boolean radiantWin) { this.radiantWin = radiantWin; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getRadiantScore() { return radiantScore; }
    public void setRadiantScore(int radiantScore) { this.radiantScore = radiantScore; }

    public int getDireScore() { return direScore; }
    public void setDireScore(int direScore) { this.direScore = direScore; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public String getSkillBracket() { return skillBracket; }
    public void setSkillBracket(String skillBracket) { this.skillBracket = skillBracket; }

    public List<MatchPlayerDetail> getRadiantPlayers() { return radiantPlayers; }
    public void setRadiantPlayers(List<MatchPlayerDetail> radiantPlayers) { this.radiantPlayers = radiantPlayers; }

    public List<MatchPlayerDetail> getDirePlayers() { return direPlayers; }
    public void setDirePlayers(List<MatchPlayerDetail> direPlayers) { this.direPlayers = direPlayers; }
}