package com.itomagoi.dotaassistant.model;

import java.util.List;

public record MatchPlayerDetail(
        Long accountId,
        String personaName,      // Якщо закритий профіль — буде "Anonymous"

        int heroId,
        String heroName,         // Назва героя з твого кешу
        int level,

        int kills,
        int deaths,
        int assists,
        double kdaRatio,         // Відразу порахуємо KDA для зручності

        int netWorth,
        int gpm,                 // Gold Per Minute
        int xpm,                 // XP Per Minute

        int heroDamage,
        int towerDamage,
        int heroHealing,
        int observerWardsPlaced, // Корисно для сапортів

        List<ItemDto> inventory, // 6 основних слотів
        List<ItemDto> backpack,  // 3 слоти в рюкзаку
        ItemDto neutralItem      // 1 нейтральний предмет


) {

    // Вкладений record для предметів (щоб на фронтенді можна було малювати іконки за ID)
    public record ItemDto(
            Integer id,
            String name
    ) {}


}