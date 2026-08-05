package com.itomagoi.dotaassistant.controller;

import com.itomagoi.dotaassistant.model.Hero;
import com.itomagoi.dotaassistant.service.OpenDotaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}