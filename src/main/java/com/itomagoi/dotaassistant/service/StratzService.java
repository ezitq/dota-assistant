package com.itomagoi.dotaassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StratzService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenDotaService openDotaService;

    // Простий кеш primary positions (оновлюється раз на ~1 годину)
    private final AtomicReference<Map<Integer, String>> primaryPositionCache = new AtomicReference<>();
    private volatile long primaryPositionCacheAtMs = 0L;
    private static final long PRIMARY_POSITION_CACHE_TTL_MS = 60 * 60 * 1000L;

    public StratzService(@Value("${stratz.api.token}") String apiToken,
                         OpenDotaService openDotaService) {
        this.openDotaService = openDotaService;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.stratz.com/graphql")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "STRATZ_API")
                .build();
    }

    private record Counter(int heroId, double advantage, int matchCount) {
    }

    // -------------------------------------------------------------------------
    // TOOLS
    // -------------------------------------------------------------------------

    @Tool(description = """
            Отримує ТОП-8 overall контрпіків проти героя за ПОТОЧНИМ тижнем (Divine/Immortal).
            Це контрпіки ПО ГРІ (не по лінії і не по позиції).
            Дані зі Stratz: synergy/advantage + matchCount.
            Використовуй, коли потрібні актуальні контрпіки поточної мети.
            """)
    public String getOverallCounters(int heroId) {
        String targetHeroName = resolveHeroName(heroId);
        if (targetHeroName == null) {
            return "Героя з ID " + heroId + " не знайдено.";
        }

        try {
            List<Counter> counters = loadOverallCounterStats(heroId);
            if (counters.isEmpty()) {
                return "Сильних overall-контрпіків проти " + targetHeroName + " не знайдено.";
            }

            counters.sort(counterComparator());

            StringBuilder result = new StringBuilder();
            result.append("Overall контрпіки проти ")
                    .append(targetHeroName)
                    .append(" (поточний тиждень, Divine/Immortal):\n\n");

            int limit = Math.min(8, counters.size());
            for (int i = 0; i < limit; i++) {
                Counter c = counters.get(i);
                String name = openDotaService.getHeroNameById(c.heroId());
                String tier = tierLabel(c);

                result.append(String.format(
                        "%d. %s [%s] — advantage +%.1f%% (матчів: %d)\n",
                        i + 1, name, tier, c.advantage(), c.matchCount()
                ));
            }
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Помилка при отриманні overall-контрпіків: " + e.getMessage();
        }
    }

    @Tool(description = """
            Отримує ТОП контрпіків проти героя з розбивкою по позиціях (Pos1–Pos5).
            Overall synergy береться з поточного тижня (Divine/Immortal),
            позиція контргероя — його основна роль у поточній меті.
            Використовуй, коли потрібно зрозуміти, кого пікати на конкретну роль проти героя.
            """)
    public String getPositionalCounters(int heroId) {
        String targetHeroName = resolveHeroName(heroId);
        if (targetHeroName == null) {
            return "Героя з ID " + heroId + " не знайдено.";
        }

        try {
            List<Counter> counters = loadOverallCounterStats(heroId);
            if (counters.isEmpty()) {
                return "Сильних контрпіків проти " + targetHeroName + " не знайдено.";
            }

            Map<Integer, String> primaryPositionByHeroId = loadPrimaryPositionsCached();

            Map<String, List<Counter>> buckets = new LinkedHashMap<>();
            buckets.put("POSITION_1", new ArrayList<>());
            buckets.put("POSITION_2", new ArrayList<>());
            buckets.put("POSITION_3", new ArrayList<>());
            buckets.put("POSITION_4", new ArrayList<>());
            buckets.put("POSITION_5", new ArrayList<>());

            for (Counter c : counters) {
                String pos = primaryPositionByHeroId.get(c.heroId());
                if (pos != null && buckets.containsKey(pos)) {
                    buckets.get(pos).add(c);
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("Позиційні контрпіки проти ")
                    .append(targetHeroName)
                    .append(" (поточний тиждень, Divine/Immortal):\n");

            for (Map.Entry<String, List<Counter>> entry : buckets.entrySet()) {
                List<Counter> list = entry.getValue();
                list.sort(counterComparator());

                result.append("\n").append(translatePosition(entry.getKey())).append(":\n");

                if (list.isEmpty()) {
                    result.append("  недостатньо даних\n");
                    continue;
                }

                int limit = Math.min(8, list.size());
                for (int i = 0; i < limit; i++) {
                    Counter c = list.get(i);
                    String name = openDotaService.getHeroNameById(c.heroId());
                    String tier = tierLabel(c);

                    result.append(String.format(
                            "  %d. %s [%s] — advantage +%.1f%% (матчів: %d)\n",
                            i + 1, name, tier, c.advantage(), c.matchCount()
                    ));
                }
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Помилка при отриманні позиційних контрпіків: " + e.getMessage();
        }
    }

    @Tool(description = """
            Отримує lane-outcome статистику для героя: проти кого йому важко/легко на лінії.
            Дані за поточний тиждень, Divine/Immortal.
            isWith=false → тільки суперники (against).
            Показує топ найважчих і найлегших lane-матчапів.
            """)
    public String getLaneOutcomes(int heroId) {
        String targetHeroName = resolveHeroName(heroId);
        if (targetHeroName == null) {
            return "Героя з ID " + heroId + " не знайдено.";
        }

        try {
            JsonNode arr = fetchLaneOutcomes(heroId, true);
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                // fallback без week
                arr = fetchLaneOutcomes(heroId, false);
            }

            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                return "Немає lane-даних для " + targetHeroName + ".";
            }

            record LaneRow(
                    int enemyId,
                    String position,
                    int matchCount,
                    double laneWinRate,
                    double matchWinRate
            ) {
            }

            List<LaneRow> rows = new ArrayList<>();

            for (JsonNode node : arr) {
                int matchCount = node.path("matchCount").asInt();
                if (matchCount < 30) continue;

                int enemyId = node.path("heroId2").asInt();
                String position = node.path("position").asText();

                int laneWins = node.path("winCount").asInt() + node.path("stompWinCount").asInt();
                int laneLosses = node.path("lossCount").asInt() + node.path("stompLossCount").asInt();
                int draws = node.path("drawCount").asInt();
                int lanePlayed = laneWins + laneLosses + draws;
                if (lanePlayed <= 0) continue;

                double laneWinRate = (double) laneWins / lanePlayed * 100.0;
                double matchWinRate = (double) node.path("matchWinCount").asInt() / matchCount * 100.0;

                rows.add(new LaneRow(enemyId, position, matchCount, laneWinRate, matchWinRate));
            }

            if (rows.isEmpty()) {
                return "Недостатньо lane-матчапів для " + targetHeroName + ".";
            }

            List<LaneRow> hardest = rows.stream()
                    .sorted(Comparator
                            .comparingDouble(LaneRow::laneWinRate)
                            .thenComparing(Comparator.comparingInt(LaneRow::matchCount).reversed()))
                    .limit(8)
                    .toList();

            List<LaneRow> easiest = rows.stream()
                    .sorted(Comparator
                            .comparingDouble(LaneRow::laneWinRate).reversed()
                            .thenComparing(Comparator.comparingInt(LaneRow::matchCount).reversed()))
                    .limit(8)
                    .toList();

            StringBuilder result = new StringBuilder();
            result.append("Lane outcomes для ").append(targetHeroName)
                    .append(" (against, Divine/Immortal):\n");

            result.append("\nНайважчі лінії (контрять на лінії):\n");
            int i = 1;
            for (LaneRow r : hardest) {
                String name = openDotaService.getHeroNameById(r.enemyId());
                result.append(String.format(
                        "%d. %s — lane WR %.1f%% | match WR %.1f%% | games %d | pos %s\n",
                        i++, name, r.laneWinRate(), r.matchWinRate(), r.matchCount(),
                        translatePosition(r.position())
                ));
            }

            result.append("\nНайлегші лінії:\n");
            i = 1;
            for (LaneRow r : easiest) {
                String name = openDotaService.getHeroNameById(r.enemyId());
                result.append(String.format(
                        "%d. %s — lane WR %.1f%% | match WR %.1f%% | games %d | pos %s\n",
                        i++, name, r.laneWinRate(), r.matchWinRate(), r.matchCount(),
                        translatePosition(r.position())
                ));
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Помилка при отриманні lane outcomes: " + e.getMessage();
        }
    }

    public String getHeroSpammer(int heroId) {
        final int MIN_MATCHES = 10;
        final double MIN_WINRATE = 50;
        final int TAKE = 100; // скільки гравців брати з лідерборду

        String graphqlQuery = """
            query {
              leaderboard {
                hero(request: {
                  heroIds: [%d]
                  bracketIds: [DIVINE]
                  take: %d
                }) {
                  steamAccountId
                  wins
                  losses
                  position
                }
              }
            }
            """.formatted(heroId, TAKE);

        try {
            JsonNode root = postGraphql(graphqlQuery);

            if (root.has("errors")) {
                return "Помилка Stratz API: " + root.path("errors");
            }

            JsonNode stats = root.path("data")
                    .path("leaderboard")
                    .path("hero");

            if (!stats.isArray() || stats.isEmpty()) {
                return "Nothing found";
            }

            record Player(long steamId, double wr, String position) {}

            List<Player> players = new ArrayList<>();

            for (JsonNode node : stats) {
                long accountId = node.path("steamAccountId").asLong();
                int wins = node.path("wins").asInt();
                int losses = node.path("losses").asInt();
                int matches = wins + losses;

                if (matches < MIN_MATCHES) continue;

                double wr = matches > 0 ? (double) wins / matches * 100 : 0;
                if (wr < MIN_WINRATE) continue;

                String position = node.path("position").asText();
                players.add(new Player(accountId, wr, position));
            }

            if (players.isEmpty()) {
                return "Нікого не знайдено з WR ≥ " + MIN_WINRATE + "% і " + MIN_MATCHES + "+ матчами";
            }

            String heroName = resolveHeroName(heroId);
            if (heroName == null) {
                return "Невідомий герой з id: " + heroId;
            }

            StringBuilder result = new StringBuilder();
            result.append("HERO NAME: ").append(heroName.toUpperCase()).append("\n");

            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                result.append(String.format(
                        "%d. %d — WR %.1f%% (позиція: %s)%n",
                        i + 1,
                        p.steamId,
                        p.wr,
                        p.position
                ));
            }

            return result.toString();

        } catch (Exception e) {
            throw new RuntimeException("Не вдалося отримати дані для heroId=" + heroId, e);
        }
    }

    @Tool(description = """
            Отримує найактуальнішу мету героїв на Divine/Immortal з розбивкою по 5 позиціях.
            Для кожної позиції повертає топ героїв за winrate (мін. 50 ігор).
            """)
    public String getStratzMetaHeroes(boolean withWeek) {
        String weekPart = withWeek ? "week: " + getCurrentStratzWeekEpoch() + "\n" : "";
        String graphqlQuery = """
                query {
                  pos1: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_1], %s) {
                      heroId matchCount winCount
                    }
                  }
                  pos2: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_2], %s) {
                      heroId matchCount winCount
                    }
                  }
                  pos3: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_3], %s) {
                      heroId matchCount winCount
                    }
                  }
                  pos4: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_4], %s) {
                      heroId matchCount winCount
                    }
                  }
                  pos5: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_5], %s) {
                      heroId matchCount winCount
                    }
                  }
                }
                """.formatted(weekPart, weekPart, weekPart, weekPart, weekPart);

        try {
            JsonNode root = postGraphql(graphqlQuery);
            if (root.has("errors")) {
                return "Помилка Stratz API: " + root.path("errors");
            }

            record MetaHero(int heroId, int matchCount, double winRate) {
            }

            Map<String, String> aliases = Map.of(
                    "pos1", "POSITION_1",
                    "pos2", "POSITION_2",
                    "pos3", "POSITION_3",
                    "pos4", "POSITION_4",
                    "pos5", "POSITION_5"
            );

            StringBuilder result = new StringBuilder("Мета Divine/Immortal по позиціях:\n");
            JsonNode data = root.path("data");

            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                JsonNode stats = data.path(entry.getKey()).path("stats");
                List<MetaHero> heroes = new ArrayList<>();

                if (stats.isArray()) {
                    for (JsonNode node : stats) {
                        int matchCount = node.path("matchCount").asInt();
                        if (matchCount < 400) continue;
                        int winCount = node.path("winCount").asInt();
                        double wr = (double) winCount / matchCount * 100.0;
                        if (wr < 52.0) continue;

                        heroes.add(new MetaHero(node.path("heroId").asInt(), matchCount, wr));
                    }
                }

                heroes.sort(Comparator
                        .comparingDouble(MetaHero::winRate).reversed()
                        .thenComparingInt(MetaHero::matchCount).reversed());

                result.append("\n").append(translatePosition(entry.getValue())).append(":\n");
                int limit = Math.min(8, heroes.size());
                if (limit == 0) {
                    result.append("  недостатньо даних\n");
                    continue;
                }
                for (int i = 0; i < limit; i++) {
                    MetaHero h = heroes.get(i);
                    result.append(String.format(
                            "  %d. %s — WR %.1f%% (матчів: %d)\n",
                            i + 1,
                            openDotaService.getHeroNameById(h.heroId()),
                            h.winRate(),
                            h.matchCount()
                    ));
                }
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Не вдалося отримати мету зі Stratz API: " + e.getMessage();
        }
    }

    @Tool(description = """
            Отримує статистику популярності героя на різних позиціях (ролях) за його ID.
            Повертає відсоток ігор на кожній лінії та вінрейт для кожної ролі.
            """)
    public String getHeroRoleDistribution(int heroId) {
        String targetHeroName = resolveHeroName(heroId);
        if (targetHeroName == null) {
            return "Героя з ID " + heroId + " не знайдено.";
        }

        String graphqlQuery = """
                query {
                  heroStats {
                    stats(
                      heroIds: [%d]
                      bracketBasicIds: [DIVINE_IMMORTAL]
                      groupByPosition: true
                    ) {
                      position
                      matchCount
                      winCount
                    }
                  }
                }
                """.formatted(heroId);

        try {
            JsonNode root = postGraphql(graphqlQuery);
            if (root.has("errors")) {
                return "Помилка Stratz API: " + root.path("errors");
            }

            JsonNode statsArray = root.path("data").path("heroStats").path("stats");
            if (!statsArray.isArray() || statsArray.isEmpty()) {
                return "Немає статистики ігор для героя " + targetHeroName + ".";
            }

            record RoleStat(String position, int matchCount, double winRate) {
            }
            List<RoleStat> roles = new ArrayList<>();
            int totalMatches = 0;

            for (JsonNode node : statsArray) {
                if (node.path("position").isNull()) continue;

                String position = node.path("position").asText();
                int matchCount = node.path("matchCount").asInt();
                int winCount = node.path("winCount").asInt();
                if (matchCount <= 0) continue;

                double winRate = (double) winCount / matchCount * 100.0;
                roles.add(new RoleStat(position, matchCount, winRate));
                totalMatches += matchCount;
            }

            if (totalMatches == 0) {
                return "Недостатньо даних про матчі.";
            }

            roles.sort((a, b) -> Integer.compare(b.matchCount(), a.matchCount()));

            StringBuilder result = new StringBuilder();
            result.append("Розподіл ролей для ").append(targetHeroName)
                    .append(" (всього ігор: ").append(totalMatches).append("):\n");

            for (RoleStat r : roles) {
                double pickRate = (double) r.matchCount() / totalMatches * 100.0;
                if (pickRate < 2.0) continue;

                result.append(String.format(
                        "- %s: %.1f%% ігор | Вінрейт: %.1f%%\n",
                        translatePosition(r.position()), pickRate, r.winRate()
                ));
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Помилка при отриманні позицій для героя: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // DATA LOADERS
    // -------------------------------------------------------------------------

    private List<Counter> loadOverallCounterStats(int heroId) throws Exception {
        List<Counter> counters = fetchMatchupCounters(heroId, true);
        if (counters.isEmpty()) {
            // fallback: без week, якщо за тиждень мало даних
            counters = fetchMatchupCounters(heroId, false);
        }
        return counters;
    }

    private List<Counter> fetchMatchupCounters(int heroId, boolean withWeek) throws Exception {
        String weekPart = withWeek ? "week: " + getCurrentStratzWeekEpoch() + "\n" : "";

        String graphqlQuery = """
                query {
                  heroStats {
                    heroVsHeroMatchup(
                      heroId: %d
                      %s
                      bracketBasicIds: [DIVINE_IMMORTAL]
                    ) {
                      disadvantage {
                        heroId
                        vs {
                          heroId2
                          synergy
                          matchCount
                        }
                      }
                    }
                  }
                }
                """.formatted(heroId, weekPart);

        JsonNode root = postGraphql(graphqlQuery);
        if (root.has("errors")) {
            throw new IllegalStateException(root.path("errors").toString());
        }

        JsonNode disadvantageArray = root.path("data")
                .path("heroStats")
                .path("heroVsHeroMatchup")
                .path("disadvantage");

        List<Counter> counters = new ArrayList<>();
        if (!disadvantageArray.isArray() || disadvantageArray.isEmpty()) {
            return counters;
        }

        JsonNode vsArray = disadvantageArray.get(0).path("vs");
        if (!vsArray.isArray()) {
            return counters;
        }

        for (JsonNode vs : vsArray) {
            int matchCount = vs.path("matchCount").asInt();
            if (matchCount < 40) continue;

            double synergy = vs.path("synergy").asDouble();
            if (synergy >= 0) continue;

            int enemyId = vs.path("heroId2").asInt();
            counters.add(new Counter(enemyId, Math.abs(synergy), matchCount));
        }
        return counters;
    }

    private JsonNode fetchLaneOutcomes(int heroId, boolean withWeek) throws Exception {
        String weekPart = withWeek ? "week: " + getCurrentStratzWeekEpoch() + "\n" : "";

        String graphqlQuery = """
                query {
                  heroStats {
                    laneOutcome(
                      heroId: %d
                      isWith: false
                      %s
                      bracketBasicIds: [DIVINE_IMMORTAL]
                    ) {
                      heroId1
                      heroId2
                      position
                      matchCount
                      winCount
                      lossCount
                      drawCount
                      stompWinCount
                      stompLossCount
                      matchWinCount
                    }
                  }
                }
                """.formatted(heroId, weekPart);

        JsonNode root = postGraphql(graphqlQuery);
        if (root.has("errors")) {
            throw new IllegalStateException(root.path("errors").toString());
        }
        return root.path("data").path("heroStats").path("laneOutcome");
    }

    private Map<Integer, String> loadPrimaryPositionsCached() throws Exception {
        long now = System.currentTimeMillis();
        Map<Integer, String> cached = primaryPositionCache.get();
        if (cached != null && (now - primaryPositionCacheAtMs) < PRIMARY_POSITION_CACHE_TTL_MS) {
            return cached;
        }

        Map<Integer, String> loaded = loadPrimaryPositions();
        primaryPositionCache.set(loaded);
        primaryPositionCacheAtMs = now;
        return loaded;
    }

    private Map<Integer, String> loadPrimaryPositions() throws Exception {
        String graphqlQuery = """
                query {
                  pos1: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_1]) {
                      heroId matchCount
                    }
                  }
                  pos2: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_2]) {
                      heroId matchCount
                    }
                  }
                  pos3: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_3]) {
                      heroId matchCount
                    }
                  }
                  pos4: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_4]) {
                      heroId matchCount
                    }
                  }
                  pos5: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_5]) {
                      heroId matchCount
                    }
                  }
                }
                """;

        JsonNode root = postGraphql(graphqlQuery);
        if (root.has("errors")) {
            throw new IllegalStateException(root.path("errors").toString());
        }

        Map<Integer, Integer> bestGames = new HashMap<>();
        Map<Integer, String> bestPos = new HashMap<>();

        Map<String, String> aliasToPos = Map.of(
                "pos1", "POSITION_1",
                "pos2", "POSITION_2",
                "pos3", "POSITION_3",
                "pos4", "POSITION_4",
                "pos5", "POSITION_5"
        );

        JsonNode data = root.path("data");
        for (Map.Entry<String, String> entry : aliasToPos.entrySet()) {
            JsonNode stats = data.path(entry.getKey()).path("stats");
            if (!stats.isArray()) continue;

            for (JsonNode node : stats) {
                int id = node.path("heroId").asInt();
                int matchCount = node.path("matchCount").asInt();
                if (matchCount <= 0) continue;

                Integer currentBest = bestGames.get(id);
                if (currentBest == null || matchCount > currentBest) {
                    bestGames.put(id, matchCount);
                    bestPos.put(id, entry.getValue());
                }
            }
        }
        return bestPos;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private JsonNode postGraphql(String query) throws Exception {
        String jsonResponse = restClient.post()
                .body(Map.of("query", query))
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(jsonResponse);
    }

    private String resolveHeroName(int heroId) {
        String name = openDotaService.getHeroNameById(heroId);
        if (name == null
                || name.startsWith("Unknown")
                || name.startsWith("Hero ID")) {
            return null;
        }
        return name;
    }

    private Comparator<Counter> counterComparator() {
        return (a, b) -> {
            int byAdv = Double.compare(b.advantage(), a.advantage());
            if (byAdv != 0) return byAdv;
            return Integer.compare(b.matchCount(), a.matchCount());
        };
    }

    private String tierLabel(Counter c) {
        return (c.matchCount() >= 150 && c.advantage() >= 5.0) ? "HARD" : "soft";
    }

    /**
     * Stratz week starts Thursday 00:00 UTC.
     */
    private long getCurrentStratzWeekEpoch() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        int day = now.getDayOfWeek().getValue(); // Mon=1 ... Sun=7
        int daysFromThursday = (day >= 4) ? (day - 4) : (day + 3);

        return now.minusDays(daysFromThursday)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toEpochSecond();
    }

    private String translatePosition(String stratzPosition) {
        return switch (stratzPosition) {
            case "POSITION_1" -> "Керрі (Safe Lane)";
            case "POSITION_2" -> "Мід (Mid Lane)";
            case "POSITION_3" -> "Офлейн (Hard Lane)";
            case "POSITION_4" -> "Семі-саппорт (Soft Support)";
            case "POSITION_5" -> "Фул-саппорт (Hard Support)";
            default -> stratzPosition;
        };
    }
}