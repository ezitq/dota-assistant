package com.itomagoi.dotaassistant.controller;

import com.itomagoi.dotaassistant.model.Hero;
import com.itomagoi.dotaassistant.model.HeroMatchup;
import com.itomagoi.dotaassistant.service.OpenDotaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/dota")
public class DotaController {

    private final OpenDotaService openDotaService;

    public DotaController(OpenDotaService openDotaService) {
        this.openDotaService = openDotaService;
    }

    @GetMapping("/heroes")
    public List<Hero> getHeroes() {
        return openDotaService.getAllHeroes();
    }

    @GetMapping("/heroes/{id}/matchups")
    public List<HeroMatchup> getHeroes(@PathVariable int id) {
        return openDotaService.getHeroMatchups(id);
    }

    @GetMapping("/heroes/{id}/counterpick")
    public List<String> getHeroCounterPicks(@PathVariable int id) {

        List<String> counterPicks = new ArrayList<>(List.copyOf(openDotaService.getHeroCounterPicks(id))) ;
        counterPicks.add("CounterPickOfHero");
        counterPicks.add(openDotaService.getHeroNameById(id));
        return counterPicks;
    }
}