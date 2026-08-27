package com.itomagoi.dotaassistant.service;

import com.itomagoi.dotaassistant.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class OpenDotaService {

    private final RestClient restClient;

    // КЕШ: зберігаємо героїв у пам'яті, щоб не робити зайвих HTTP-запитів
    private final Map<Integer, Hero> heroCache = new HashMap<>();
    private final Map<Integer, Item> itemCache = new HashMap<>();

    public OpenDotaService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.opendota.com/api")
                .build();
    }

    // --- РОБОТА З ГЕРОЯМИ ТА КЕШЕМ ---

    public List<Hero> getAllHeroes() {
        // Якщо кеш порожній, робимо запит до API та наповнюємо його
        if (heroCache.isEmpty()) {
            List<Hero> heroes = restClient.get()
                    .uri("/heroes")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (heroes != null) {
                heroes.forEach(hero -> heroCache.put(hero.getId(), hero));
            }
        }
        return new ArrayList<>(heroCache.values());
    }

    public List<Item> getAllItems() {
        if (itemCache.isEmpty()) {
            // Змінюємо очікуваний тип на Map<String, Item>
            Map<String, Item> itemsMap = restClient.get()
                    .uri("/constants/items")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Item>>() {
                    });

            if (itemsMap != null) {
                // Перебираємо значення мапи і кладемо в наш кеш
                itemsMap.values().forEach(item -> {
                    // Обов'язкова перевірка, бо в базі є "пустишки" без ID
                    if (item.getId() != null) {
                        itemCache.put(item.getId(), item);
                    }
                });
            }
        }
        return new ArrayList<>(itemCache.values());
    }

    public String getHeroNameById(int heroId) {
        if (heroCache.isEmpty()) getAllHeroes(); // Гарантуємо, що кеш заповнений
        Hero hero = heroCache.get(heroId);
        return hero != null ? hero.getLocalizedName() : "Unknown Hero";
    }

    // НОВИЙ МЕТОД: для зручного пошуку з контролера
    public Integer getHeroIdByName(String name) {
        if (heroCache.isEmpty()) getAllHeroes(); // Гарантуємо, що кеш заповнений
        return heroCache.values().stream()
                .filter(h -> h.getLocalizedName().equalsIgnoreCase(name))
                .map(Hero::getId)
                .findFirst()
                .orElse(null);
    }

    // --- РОБОТА З API ---

    public List<HeroMatchup> getHeroMatchups(int heroId) {
        // 1. Спочатку переконуємося, що кеш не порожній
        if (heroCache.isEmpty()) {
            getAllHeroes();
        }

        // 2. Перевіряємо, чи існує ТАКИЙ КЛЮЧ (ID) у нашій мапі
        if (heroCache.containsKey(heroId)) {
            return restClient.get()
                    .uri("/heroes/{id}/matchups", heroId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        }

        // Якщо героя з таким ID не існує — повертаємо порожній список
        return new ArrayList<>();
    }

    public List<ProPlayer> getPlayers(int heroId) {
        return restClient.get()
                .uri("/heroes/{id}/players", heroId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<HeroStats> getAllHeroStats() {
        return restClient.get()
                .uri("/heroStats")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Tool(description = "Повертає популярні предмети для всіх стадій гри (старт, рання, мід, лейт) для вказаного heroId")
    public Map<String, List<Item>> getFullHeroItemBuild(int heroId) {
        if (itemCache.isEmpty()) getAllItems();

        ItemPopularity itemPopularity = restClient.get()
                .uri("/heroes/{id}/itemPopularity", heroId)
                .retrieve()
                .body(ItemPopularity.class);

        if (itemPopularity == null) {
            return Collections.emptyMap();
        }

        Map<String, List<Item>> fullBuild = new LinkedHashMap<>(); // LinkedHashMap збереже порядок стадій

        fullBuild.put("Start Game", mapItemIdsToItems(itemPopularity.getStartGameItems()));
        fullBuild.put("Early Game", mapItemIdsToItems(itemPopularity.getEarlyGameItems()));
        fullBuild.put("Mid Game", mapItemIdsToItems(itemPopularity.getMidGameItems()));
        fullBuild.put("Late Game", mapItemIdsToItems(itemPopularity.getLateGameItems()));

        return fullBuild;
    }

    // Допоміжний приватний метод, щоб не дублювати код парсингу та захиститись від null
    private List<Item> mapItemIdsToItems(Map<String, Integer> stageItems) {
        if (stageItems == null) return new ArrayList<>();

        return stageItems.keySet().stream()
                .map(Integer::parseInt)
                .map(itemCache::get)
                .filter(Objects::nonNull) // ЗАХИСТ: відкидаємо null, якщо предмета немає в кеші
                .toList();
    }

    // --- БІЗНЕС-ЛОГІКА ТА ТУЛИ ---

    @Tool(description = "Отримує список імен героїв, які є найкращими контрпіками проти вказаного heroId у Dota 2")
    public List<String> getHeroCounterPickNames(int heroId) {
        if (heroCache.isEmpty()) getAllHeroes(); // Гарантуємо, що кеш заповнений

        return getHeroMatchups(heroId).stream()
                .filter(matchup -> matchup.getGamesPlayed() > 10)
                .filter(matchup -> {
                    double winRate = ((double) matchup.getWins() / matchup.getGamesPlayed()) * 100.0;
                    return winRate < 50.0;
                })
                .sorted((m1, m2) -> {
                    double wr1 = (double) m1.getWins() / m1.getGamesPlayed();
                    double wr2 = (double) m2.getWins() / m2.getGamesPlayed();
                    return Double.compare(wr1, wr2); // Сортування за вінрейтом
                })
                .limit(10)
                .map(matchup -> getHeroNameById(matchup.getHeroId())) // Використовуємо кешований метод
                .filter(name -> !name.equals("Unknown Hero"))
                .toList();
    }

    @Tool(description = "Повертає список найсильніших (метових) героїв на високих рангах пабліку (Ancient, Divine, Immortal) з високим вінрейтом та популярністю")
    public List<String> getMetaHeroes() {
        try {
            List<HeroStats> stats = getAllHeroStats();

            if (stats == null) {
                return List.of();
            }

            // Оголошуємо змінну metaList, щоб зберегти результат фільтрації
            List<String> metaList = stats.stream()
                    .filter(hero -> hero.getHighRankPick() > 200)
                    .sorted((h1, h2) -> {
                        double wr1 = (double) h1.getHighRankWin() / h1.getHighRankPick();
                        double wr2 = (double) h2.getHighRankWin() / h2.getHighRankPick();
                        return Double.compare(wr2, wr1);
                    })
                    .limit(10)
                    .map(hero -> String.format("%s (Winrate: %.1f%%, Matches: %d)",
                            hero.getLocalizedName(),
                            ((double) hero.getHighRankWin() / hero.getHighRankPick()) * 100,
                            hero.getHighRankPick()))
                    .toList();

            // Тепер Java знає, що таке metaList, і цей рядок працюватиме без помилок

            return metaList;

        } catch (Exception e) {
            System.err.println("🔴 Помилка: " + e.getMessage());
            return List.of("Помилка отримання даних.");
        }
    }

    public List<MatchRequest> getHighRankPublicMatches() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/publicMatches")
                        .queryParam("min_rank", 70) // 70 = Divine
                        .queryParam("max_rank", 80) // 80 = Immortal
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<MatchRequest>>() {
                });
    }

    public Map<String, Object> getFullMatchDetails(long matchId) {
        return restClient.get()
                .uri("/matches/{id}", matchId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private PlayerProfile getPlayerProfile(int accountId) {
        return restClient.get()
                .uri("/players/{account_id}", accountId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

    }

    private PlayerWinLoss getPlayerWinLoss(int accountId) {
        return restClient.get()
                .uri("/players/{account_id}/wl", accountId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

    }

    private List<RecentMatch> getPlayerRecentMatches(int accountId) {
        return restClient.get()
                // Беремо 100 матчів із запасом, щоб було з чого вибирати 20 нормальних
                .uri("/players/{account_id}/matches?limit=100", accountId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private List<PlayerHeroStat> getPlayerAllHeroes(int accountId) {
        return restClient.get()
                .uri("/players/{account_id}/heroes", accountId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

    }

    private String convertRankTierToString(Integer rankTier) {
        if (rankTier == null) return "Unranked";

        // Якщо гравець Immortal (80 або вище)
        if (rankTier >= 80) return "Immortal";

        int medalId = rankTier / 10; // Отримуємо десятки
        int star = rankTier % 10;    // Отримуємо одиниці

        String medalName = switch (medalId) {
            case 1 -> "Herald";
            case 2 -> "Guardian";
            case 3 -> "Crusader";
            case 4 -> "Archon";
            case 5 -> "Legend";
            case 6 -> "Ancient";
            case 7 -> "Divine";
            default -> "Unknown";
        };

        return medalName + " " + star;
    }

    @Tool(description = "Отримує зведену статистику профілю гравця Dota 2 за його accountId (ID акаунта). Повертає його поточну форму, вінрейт, улюблену лінію та список найкращих сигнатурних героїв.")
    public PlayerSummaryAggregator getFullPlayerSummary(int accountId) {

        if (heroCache.isEmpty()) getAllHeroes();

        // 1. Асинхронні виклики
        CompletableFuture<PlayerProfile> profileFuture = CompletableFuture.supplyAsync(() -> getPlayerProfile(accountId));
        CompletableFuture<PlayerWinLoss> wlFuture = CompletableFuture.supplyAsync(() -> getPlayerWinLoss(accountId));
        CompletableFuture<List<RecentMatch>> matchesFuture = CompletableFuture.supplyAsync(() -> getPlayerRecentMatches(accountId));
        CompletableFuture<List<PlayerHeroStat>> heroesFuture = CompletableFuture.supplyAsync(() -> getPlayerAllHeroes(accountId));

        CompletableFuture.allOf(profileFuture, wlFuture, matchesFuture, heroesFuture).join();

        PlayerProfile playerProfile = profileFuture.join();
        PlayerWinLoss playerWinLoss = wlFuture.join();
        List<RecentMatch> recentMatches = matchesFuture.join();
        List<PlayerHeroStat> playerAllHeroes = heroesFuture.join();

        // 2. Фільтрація від Турбо та Ability Draft (беремо ВСІ валідні матчі з останніх 100)
        List<RecentMatch> valid100Matches = recentMatches.stream()
                .filter(match -> match.getGameMode() != null)
                .filter(match -> match.getGameMode() != 23 && match.getGameMode() != 18)
                .toList();

        // 3. НОВА ЛОГІКА: Найкращі герої за останні 100 ігор (умова > 10 матчів)
        Map<Integer, int[]> recentHeroesStats = new HashMap<>(); // Key: heroId, Value: [games, wins]

        for (RecentMatch match : valid100Matches) {
            int hId = match.getHeroId();
            if (hId == 0) continue;

            recentHeroesStats.putIfAbsent(hId, new int[]{0, 0});
            recentHeroesStats.get(hId)[0]++; // Додаємо гру
            if (match.isPlayerWin()) {
                recentHeroesStats.get(hId)[1]++; // Додаємо перемогу
            }
        }

        List<PlayerSummaryAggregator.SignatureHero> bestRecentHeroes = new ArrayList<>();
        for (Map.Entry<Integer, int[]> entry : recentHeroesStats.entrySet()) {
            int games = entry.getValue()[0];
            if (games > 5) { // ТА САМА УМОВА: Більше 10 матчів
                int wins = entry.getValue()[1];
                double winrate = ((double) wins / games) * 100.0;
                bestRecentHeroes.add(new PlayerSummaryAggregator.SignatureHero(
                        entry.getKey(), getHeroNameById(entry.getKey()), games, winrate
                ));
            }
        }
        // Сортуємо від найбільшого вінрейту до найменшого
        bestRecentHeroes.sort(Comparator.comparingDouble(PlayerSummaryAggregator.SignatureHero::winrate).reversed());


        // 4. Логіка для останніх 20 матчів (Лінії, KDA, Найкращі/Найгірші ігри)
        List<RecentMatch> filtered20Matches = valid100Matches.stream().limit(20).toList();

        int recentWins = 0;
        Map<Integer, Integer> lanesCountingMap = new HashMap<>();
        lanesCountingMap.put(1, 0);
        lanesCountingMap.put(2, 0);
        lanesCountingMap.put(3, 0);
        lanesCountingMap.put(4, 0);
        lanesCountingMap.put(5, 0);

        List<PlayerSummaryAggregator.MatchPerformance> matchPerformances = new LinkedList<>();

        for (RecentMatch recentMatch : filtered20Matches) {

            recentWins += recentMatch.isPlayerWin() ? 1 : 0;

            String roleName = "Unknown";
            if (recentMatch.getLaneRole() != null) {
                lanesCountingMap.merge(recentMatch.getLaneRole(), 1, Integer::sum);
                if (recentMatch.getLaneRole() >= 1 && recentMatch.getLaneRole() <= 5) {
                    roleName = RoleName.values()[recentMatch.getLaneRole() - 1].name();
                }
            }

            long matchId = recentMatch.getMatchId();
            int heroId = recentMatch.getHeroId();
            String heroName = getHeroNameById(heroId);

            int kills = recentMatch.getKills();
            int deaths = recentMatch.getDeaths();
            int assists = recentMatch.getAssists();

            double kdaRatio = (double) (kills + assists) / (deaths == 0 ? 1 : deaths);
            boolean isWin = recentMatch.isPlayerWin();

            PlayerSummaryAggregator.MatchPerformance matchPerformance = new PlayerSummaryAggregator.MatchPerformance(matchId
                    , heroId
                    , heroName
                    , roleName
                    , kills
                    , deaths
                    , assists
                    , kdaRatio
                    , isWin);

            matchPerformances.add(matchPerformance);
        }

        matchPerformances.sort(Comparator
                .comparing(PlayerSummaryAggregator.MatchPerformance::isWin)
                .thenComparing(PlayerSummaryAggregator.MatchPerformance::kdaRatio));

        List<PlayerSummaryAggregator.MatchPerformance> worstMatches = matchPerformances.stream().limit(5).toList();
        List<PlayerSummaryAggregator.MatchPerformance> bestMatches = matchPerformances.reversed().stream().limit(5).toList();

        String mostFrequentRoleName = "none";
        Integer maxKey = lanesCountingMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (maxKey != null && lanesCountingMap.get(maxKey) > 0) {
            mostFrequentRoleName = RoleName.values()[maxKey - 1].name();
        }

        PlayerSummaryAggregator playerSummaryAggregator = new PlayerSummaryAggregator();

        // 5. Заповнення фінального об'єкта
        if (playerProfile.getProfile() != null) {
            playerSummaryAggregator.setAvatarUrl(playerProfile.getProfile().getAvatarFull());
            playerSummaryAggregator.setPersonaName(playerProfile.getProfile().getPersonaName());
        }
        playerSummaryAggregator.setRankTierName(convertRankTierToString(playerProfile.getRankTier()));

        if (playerWinLoss != null && playerWinLoss.getTotalGames() > 0) {
            playerSummaryAggregator.setTotalGames(playerWinLoss.getTotalGames());
            playerSummaryAggregator.setGlobalWinrate(((double) playerWinLoss.getWin() / playerWinLoss.getTotalGames() * 100.0));
        }

        if (!filtered20Matches.isEmpty()) {
            double recentWinrate = ((double) recentWins / filtered20Matches.size()) * 100.0;
            playerSummaryAggregator.setRecentWinrate(recentWinrate);
        }

        playerSummaryAggregator.setMostFrequentLane(mostFrequentRoleName);

        // 6. Глобальні Сигнатурні герої
        List<PlayerSummaryAggregator.SignatureHero> signatureHeroes = new LinkedList<>();

        for (PlayerHeroStat playerHeroStat : playerAllHeroes) {
            if (playerHeroStat.getHeroId() == null || playerHeroStat.getHeroId().equals("0")) continue;

            int heroId = Integer.parseInt(playerHeroStat.getHeroId());
            String heroName = getHeroNameById(heroId);
            int gamesPlayed = playerHeroStat.getGames();
            double winrate = gamesPlayed > 0 ? ((double) playerHeroStat.getWin() / gamesPlayed * 100.0) : 0.0;

            PlayerSummaryAggregator.SignatureHero signatureHero = new PlayerSummaryAggregator.SignatureHero(heroId, heroName, gamesPlayed, winrate);
            signatureHeroes.add(signatureHero);
        }

        signatureHeroes.sort(Comparator.comparing(hero -> {
            PlayerSummaryAggregator.SignatureHero signatureHero = (PlayerSummaryAggregator.SignatureHero) hero;
            if (signatureHero.gamesPlayed() > 50) {
                return signatureHero.winrate();
            }
            return 0.0;
        }).reversed());

        playerSummaryAggregator.setSignatureHeroes(signatureHeroes.stream().limit(5).toList());
        playerSummaryAggregator.setBestRecentMatches(bestMatches);
        playerSummaryAggregator.setWorstRecentMatches(worstMatches);

        // ВСТАНОВЛЮЄМО НАШ НОВИЙ СПИСОК
        playerSummaryAggregator.setBestRecentSignatureHeroes(bestRecentHeroes);

        return playerSummaryAggregator;
    }

    // 1. Безпечне отримання чисел (захист від null та ClassCastException)
    private int getIntSafe(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number number) {
            return number.intValue();
        }
        return 0; // Значення за замовчуванням, якщо поля немає або воно null
    }

    // 2. Безпечне отримання предмета (захист від порожніх слотів)
    private MatchPlayerDetail.ItemDto getItemDtoSafely(Integer id) {
        if (id == null || id == 0) {
            return new MatchPlayerDetail.ItemDto(0, "Empty"); // Порожній слот
        }
        // ДОДАНО: Перевірка на порожній кеш
        if (itemCache.isEmpty()) {
            getAllItems();
        }

        Item item = itemCache.get(id);
        return new MatchPlayerDetail.ItemDto(id, item != null ? item.getDname() : "Unknown Item");
    }

    // --- ОСНОВНИЙ МЕТОД ---
    @Tool(description = "Отримує детальну статистику та розбір матчу Dota 2 за його унікальним ID (matchId). Повертає інформацію про переможця, тривалість та детальну статистику всіх 10 гравців (KDA, шкода, предмети).")
    public PostMatchSummary getPostMatchSummary(long matchId) {

        Map<String, Object> matchDetails = getFullMatchDetails(matchId);

        // Безпечне отримання булевих значень
        Boolean isRadiantWinObj = (Boolean) matchDetails.get("radiant_win");
        boolean isRadiantWin = isRadiantWinObj != null && isRadiantWinObj;

        // Використовуємо кастування до Number для Long/Integer безпеки
        Number startTimeNum = (Number) matchDetails.get("start_time");
        long startTime = startTimeNum != null ? startTimeNum.longValue() : 0L;

        int duration = getIntSafe(matchDetails, "duration");
        int radiantScore = getIntSafe(matchDetails, "radiant_score");
        int direScore = getIntSafe(matchDetails, "dire_score");

        Integer skill = (Integer) matchDetails.get("skill");
        String skillBracket = "Unknown";
        if (skill != null) {
            skillBracket = switch (skill) {
                case 1 -> "Normal Skill";
                case 2 -> "High Skill";
                case 3 -> "Very High Skill";
                default -> "Unknown";
            };
        }

        List<Map<String, Object>> players = (List<Map<String, Object>>) matchDetails.get("players");
        List<MatchPlayerDetail> radiantPlayers = new LinkedList<>();
        List<MatchPlayerDetail> direPlayers = new LinkedList<>();

        if (players != null) {
            for (Map<String, Object> playerMap : players) {
                Boolean isRadiantObj = (Boolean) playerMap.get("isRadiant");
                boolean isRadiant = isRadiantObj != null && isRadiantObj;

                Number accIdNum = (Number) playerMap.get("account_id");
                Long accountId = accIdNum != null ? accIdNum.longValue() : null;

                String personaname = (String) playerMap.get("personaname");
                if (personaname == null) {
                    personaname = "Anonymous"; // Захист від закритих профілів
                }

                int heroId = getIntSafe(playerMap, "hero_id");
                String heroName = getHeroNameById(heroId); // Твій існуючий безпечний метод
                int level = getIntSafe(playerMap, "level");

                int kills = getIntSafe(playerMap, "kills");
                int deaths = getIntSafe(playerMap, "deaths");
                int assists = getIntSafe(playerMap, "assists");
                double kdaRatio = (double) (kills + assists) / (deaths == 0 ? 1 : deaths); // Захист від ділення на нуль

                int netWorth = getIntSafe(playerMap, "net_worth");
                int goldPerMin = getIntSafe(playerMap, "gold_per_min");
                int xpPerMin = getIntSafe(playerMap, "xp_per_min");

                int heroDamage = getIntSafe(playerMap, "hero_damage");
                int towerDamage = getIntSafe(playerMap, "tower_damage");
                int heroHealing = getIntSafe(playerMap, "hero_healing");
                int obsPlaced = getIntSafe(playerMap, "obs_placed");

                // Безпечно парсимо інвентар через допоміжний метод
                List<MatchPlayerDetail.ItemDto> mainItems = new LinkedList<>();
                for (int i = 0; i < 6; i++) {
                    mainItems.add(getItemDtoSafely((Integer) playerMap.get("item_" + i)));
                }

                List<MatchPlayerDetail.ItemDto> backpackItems = new LinkedList<>();
                for (int i = 0; i < 3; i++) {
                    backpackItems.add(getItemDtoSafely((Integer) playerMap.get("backpack_" + i)));
                }

                MatchPlayerDetail.ItemDto neutralItem = getItemDtoSafely((Integer) playerMap.get("item_neutral"));

                MatchPlayerDetail playerDetail = new MatchPlayerDetail(
                        accountId, personaname, heroId, heroName, level,
                        kills, deaths, assists, kdaRatio,
                        netWorth, goldPerMin, xpPerMin,
                        heroDamage, towerDamage, heroHealing, obsPlaced,
                        mainItems, backpackItems, neutralItem
                );

                if (isRadiant) {
                    radiantPlayers.add(playerDetail);
                } else {
                    direPlayers.add(playerDetail);
                }
            }
        }

        PostMatchSummary postMatchSummary = new PostMatchSummary();
        postMatchSummary.setMatchId(matchId);
        postMatchSummary.setDirePlayers(direPlayers);
        postMatchSummary.setRadiantPlayers(radiantPlayers);
        postMatchSummary.setDireScore(direScore);
        postMatchSummary.setRadiantScore(radiantScore);
        postMatchSummary.setDurationSeconds(duration);
        postMatchSummary.setRadiantWin(isRadiantWin);
        postMatchSummary.setStartTime(startTime);
        postMatchSummary.setSkillBracket(skillBracket);

        return postMatchSummary;
    }


}