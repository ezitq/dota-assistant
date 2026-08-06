package com.itomagoi.dotaassistant.config;

import com.itomagoi.dotaassistant.model.CounterPickRequest;
import com.itomagoi.dotaassistant.service.OpenDotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class AiConfig {


    @Bean
    @Description("Отримує список імен героїв, які є найкращими контрпіками проти вказаного heroId у Dota 2")
    public Function<CounterPickRequest, List<String>> getCounterPicks(OpenDotaService openDotaService) {
        return request -> openDotaService.getHeroCounterPickNames(request.heroId());
    }
}
