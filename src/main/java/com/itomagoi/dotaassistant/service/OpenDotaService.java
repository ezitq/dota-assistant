package com.itomagoi.dotaassistant.service;

import com.itomagoi.dotaassistant.model.Hero;
import com.itomagoi.dotaassistant.model.HeroMatchup;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OpenDotaService {

    private final RestClient restClient;

    public OpenDotaService() {
        // Налаштовуємо RestClient з базовим URL OpenDota API
        this.restClient = RestClient.builder()
                .baseUrl("https://api.opendota.com/api")
                .build();
    }

    // Метод для отримання списку всіх героїв
    public List<Hero> getAllHeroes() {
        return restClient.get()
                .uri("/heroes")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<HeroMatchup> getHeroMatchups(int heroId){
        return restClient.get()
                .uri("/heroes/{id}/matchups", heroId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<String> getHeroCounterPicks(int heroId){

        Map<Integer, String> heroesMap = getAllHeroes().stream()
                .collect(Collectors.toMap(Hero::getId, Hero::getLocalizedName));

        return getHeroMatchups(heroId).stream()
                .filter(matchup -> matchup.getGamesPlayed() > 10)
                .filter(matchup -> {
                    double winRate = ((double) matchup.getWins() / matchup.getGamesPlayed()) * 100.0;
                    return winRate < 45.0;
                })
                .sorted((m1, m2) -> {
                    double wr1 = (double) m1.getWins() / m1.getGamesPlayed();
                    double wr2 = (double) m2.getWins() / m2.getGamesPlayed();
                    return Double.compare(wr1, wr2);
                })
                .limit(10)
                // 3. Замість ID мапимо на локалізоване ім'я героя з мапи
                .map(matchup -> heroesMap.get(matchup.getHeroId()))
                .filter(Objects::nonNull)
                .toList();

    }

    public String getHeroNameById(int heroId){
        Map<Integer, String> heroesMap = getAllHeroes().stream()
                .collect(Collectors.toMap(Hero::getId, Hero::getLocalizedName));

        return heroesMap.get(heroId);
    }
}