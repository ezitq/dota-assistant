package com.itomagoi.dotaassistant.controller;

import com.itomagoi.dotaassistant.model.*;
import com.itomagoi.dotaassistant.service.OpenDotaService;
import com.itomagoi.dotaassistant.service.ReportExportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dota")
public class DotaController {

    private final OpenDotaService openDotaService;
    private final ReportExportService reportExportService;

    public DotaController(OpenDotaService openDotaService, ReportExportService reportExportService) {
        this.openDotaService = openDotaService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/heroes")
    public List<Hero> getHeroes() {
        return openDotaService.getAllHeroes();
    }

    @GetMapping("/heroes/{id}/matchups")
    public List<HeroMatchup> getHeroesMatchups(@PathVariable int id) {
        return openDotaService.getHeroMatchups(id);
    }

    @GetMapping("/heroes/{name}/counterpick")
    public List<String> getHeroCounterPicks(@PathVariable String name) {
        Integer heroId = openDotaService.getHeroIdByName(name);

        if (heroId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Героя з іменем " + name + " не знайдено");
        }

        List<String> counterPicks = new ArrayList<>(openDotaService.getHeroCounterPickNames(heroId));
        counterPicks.add("CounterPickOfHero");
        counterPicks.add(openDotaService.getHeroNameById(heroId));

        return counterPicks;
    }

    @GetMapping("/heroes/{name}/player")
    public List<ProPlayer> getPlayersByHeroName(@PathVariable String name) {
        Integer heroId = openDotaService.getHeroIdByName(name);

        if (heroId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Героя з іменем " + name + " не знайдено");
        }

        return openDotaService.getPlayers(heroId);
    }

    @GetMapping("/export-global-meta")
    public String exportGlobalMeta() {
        return reportExportService.exportGlobalMetaToFile();
    }

    @GetMapping("/heroes/{name}/items")
    public Map<String, List<Item>> getHeroItems(@PathVariable String name) {
        Integer heroId = openDotaService.getHeroIdByName(name);
        if (heroId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Героя не знайдено");
        }
        return openDotaService.getFullHeroItemBuild(heroId);
    }

    @GetMapping("/matches/{id}/full")
    public Map<String, Object> getFullMatchDetailByMatchId(@PathVariable long id){

        return openDotaService.getFullMatchDetails(id);
    }

    @GetMapping("/players/{accountId}/summary")
    public PlayerSummaryAggregator getFullPLayerSummary(@PathVariable int accountId){
        return openDotaService.getFullPlayerSummary(accountId);
    }


    @GetMapping("/matches/{id}/summary")
    public PostMatchSummary getPostMatchSummary(@PathVariable long id){

        return openDotaService.getPostMatchSummary(id);
    }
}