package com.itomagoi.dotaassistant.model;

import java.util.List;

public class PlayerSummaryAggregator {

    // 1. Базова інформація
    private String personaName;
    private String avatarUrl;
    private String rankTierName; // Наприклад, "Divine 3" або просто число 73

    // 2. Глобальна статистика
    private int totalGames;
    private double globalWinrate; // Відсоток (наприклад, 54.2)

    // 3. Статистика за останні 20 матчів
    private double recentWinrate;
    private String mostFrequentLane; // Наприклад, "Midlane" або "Safe Lane"

    // 4. Списки (Топ-5)
    private List<SignatureHero> signatureHeroes;
    private List<MatchPerformance> bestRecentMatches;
    private List<MatchPerformance> worstRecentMatches;

    private List<SignatureHero> bestRecentSignatureHeroes;
    // --- Вкладені класи (або records) для списків ---

    public record SignatureHero(
            int heroId,
            String heroName, // Підтягнеться з твого heroCache
            int gamesPlayed,
            double winrate
    ) {}

    public record MatchPerformance(
            long matchId,
            int heroId,
            String heroName, // Підтягнеться з твого heroCache
            String lane,
            int kills,
            int deaths,
            int assists,
            double kdaRatio, // Розрахований KDA
            boolean isWin
    ) {}

    public String getPersonaName() {
        return personaName;
    }

    public void setPersonaName(String personaName) {
        this.personaName = personaName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRankTierName() {
        return rankTierName;
    }

    public void setRankTierName(String rankTierName) {
        this.rankTierName = rankTierName;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public double getGlobalWinrate() {
        return globalWinrate;
    }

    public void setGlobalWinrate(double globalWinrate) {
        this.globalWinrate = globalWinrate;
    }

    public double getRecentWinrate() {
        return recentWinrate;
    }

    public void setRecentWinrate(double recentWinrate) {
        this.recentWinrate = recentWinrate;
    }

    public String getMostFrequentLane() {
        return mostFrequentLane;
    }

    public void setMostFrequentLane(String mostFrequentLane) {
        this.mostFrequentLane = mostFrequentLane;
    }

    public List<SignatureHero> getSignatureHeroes() {
        return signatureHeroes;
    }

    public void setSignatureHeroes(List<SignatureHero> signatureHeroes) {
        this.signatureHeroes = signatureHeroes;
    }

    public List<MatchPerformance> getBestRecentMatches() {
        return bestRecentMatches;
    }

    public void setBestRecentMatches(List<MatchPerformance> bestRecentMatches) {
        this.bestRecentMatches = bestRecentMatches;
    }

    public List<MatchPerformance> getWorstRecentMatches() {
        return worstRecentMatches;
    }

    public void setWorstRecentMatches(List<MatchPerformance> worstRecentMatches) {
        this.worstRecentMatches = worstRecentMatches;
    }

    public List<SignatureHero> getBestRecentSignatureHeroes() {
        return bestRecentSignatureHeroes;
    }

    public void setBestRecentSignatureHeroes(List<SignatureHero> bestRecentSignatureHeroes) {
        this.bestRecentSignatureHeroes = bestRecentSignatureHeroes;
    }
}