package com.itomagoi.dotaassistant.service;

import com.itomagoi.dotaassistant.model.Hero;
import com.itomagoi.dotaassistant.model.HeroMatchup;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

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
}