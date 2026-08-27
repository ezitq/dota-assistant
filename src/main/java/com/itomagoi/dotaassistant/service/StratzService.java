package com.itomagoi.dotaassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itomagoi.dotaassistant.model.Hero;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.ai.tool.annotation.Tool;

import java.rmi.NoSuchObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StratzService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenDotaService openDotaService; // Додаємо сюди

    // Spring автоматично підставить OpenDotaService сюди
    public StratzService(@Value("${stratz.api.token}") String apiToken,
                         OpenDotaService openDotaService) {
        this.openDotaService = openDotaService;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.stratz.com/graphql")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Tool(description = """
            Отримує найактуальнішу статистику героїв на високих рангах (Divine/Immortal) з розбивкою по позиціях.
            JSON містить 5 кореневих об'єктів (аліасів):
            - pos1 (Керрі / Safe Lane)
            - pos2 (Мід / Midlane)
            - pos3 (Офлейн / Hard Lane)
            - pos4 (Семі-саппорт / Soft Support)
            - pos5 (Фул-саппорт / Hard Support)
            
            У кожному з цих полів є масив 'stats', який містить об'єкти з heroId, matchCount (кількість ігор) та winCount (кількість перемог).
            
            СУВОРЕ ПРАВИЛО:
            1. Щоб знайти кращих героїв для міду, шукай ЇХ ТІЛЬКИ в масиві 'pos2'.
            2. Рахуй вінрейт (winCount / matchCount * 100).
            3. Ігноруй героїв, у яких matchCount менше 50 ігор.
            4. Уважно перекладай heroId у назви (наприклад, 107 - Earth Spirit, 90 - Keeper of the Light, 25 - Lina, 106 - Ember Spirit).
            """)
    public String getStratzMetaHeroes() {
        // Робимо 5 паралельних запитів в одному завдяки GraphQL Aliases
        String graphqlQuery = """
                query {
                  pos1: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_1]) {
                      heroId matchCount winCount
                    }
                  }
                  pos2: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_2]) {
                      heroId matchCount winCount
                    }
                  }
                  pos3: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_3]) {
                      heroId matchCount winCount
                    }
                  }
                  pos4: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_4]) {
                      heroId matchCount winCount
                    }
                  }
                  pos5: heroStats {
                    stats(bracketBasicIds: [DIVINE_IMMORTAL], positionIds: [POSITION_5]) {
                      heroId matchCount winCount
                    }
                  }
                }
                """;

        Map<String, String> requestBody = Map.of("query", graphqlQuery);

        try {
            String response = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (response != null) return response;
            return "Немає даних про мету.";

        } catch (Exception e) {
            System.err.println("Помилка Stratz API (Мета): " + e.getMessage());
            return "Не вдалося отримати мету зі Stratz API.";
        }
    }

    // Допоміжний метод для надійного перекладу імені героя в ID
    private int getHeroIdByName(String heroName) throws NoSuchObjectException {
        String name = heroName.toLowerCase().replace(" ", "").replace("-", "");
        OpenDotaService openDotaService = new OpenDotaService();
        List<Hero> dotaHeroes = openDotaService.getAllHeroes();
        Hero hero = dotaHeroes.stream().filter(h -> h.getLocalizedName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchObjectException("no such object"));

        return hero.getId();
    }

    private record CounterStat(int vsId, double enemyWinRate, int matchCount) {
    }

    @Tool(description = """
        Отримує ТОП-5 контрпіків проти вказаного героя за його ID (наприклад, 10 для Morphling, 14 для Pudge).
        Повертає вже готовий відформатований текст з іменами героїв та відсотком переваги.
        Використовуй ці дані для заповнення HTML-шаблону контрпіків.
        """)
    public String getCounterPicks(int heroId) {
        String targetHeroName = openDotaService.getHeroNameById(heroId);

        // Якщо база не знає такого ID (повертає null або дефолтне значення)
        if (targetHeroName == null || targetHeroName.startsWith("Hero ID")) {
            return "Героя з ID " + heroId + " не знайдено в базі.";
        }

        // Запит контрпіків через офіційний ендпоінт матч-апів для хай-ММР
        // Додано heroId2 всередину блоку vs
        String graphqlQuery = """
            query {
              heroStats {
                heroVsHeroMatchup(heroId: %d, bracketBasicIds: [DIVINE_IMMORTAL]) {
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
            """.formatted(heroId);

        try {
            String jsonResponse = restClient.post()
                    .body(Map.of("query", graphqlQuery))
                    .retrieve()
                    .body(String.class);

            // ДОДАЙ ЦЕЙ РЯДОК ДЛЯ ДЕБАГУ:
            System.out.println("STRATZ RAW JSON: " + jsonResponse);

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode disadvantageArray = root.path("data")
                    .path("heroStats")
                    .path("heroVsHeroMatchup")
                    .path("disadvantage");

            if (disadvantageArray.isMissingNode() || disadvantageArray.isEmpty()) {
                return "Немає даних про контрпіки для цього героя.";
            }

            List<CounterStat> counters = new ArrayList<>();

            // Оскільки disadvantage - це масив з одного елемента (нашого героя),
            // всі вороги лежать всередині його масиву 'vs'
            JsonNode vsArray = disadvantageArray.get(0).path("vs");

            if (vsArray.isArray()) {
                for (JsonNode vsStats : vsArray) {
                    int matchCount = vsStats.path("matchCount").asInt();
                    if (matchCount < 50) continue; // Фільтр рідкісних ігор

                    int enemyHeroId = vsStats.path("heroId2").asInt();
                    double synergy = vsStats.path("synergy").asDouble();

                    // У Stratz від'ємна синергія (напр. -4.7) означає, що нашому герою ВАЖКО.
                    // Тобто це і є контрпік. Беремо тільки ті значення, що менше нуля.
                    if (synergy < 0) {
                        double advantagePercent = Math.abs(synergy); // Перетворюємо -4.7 на 4.7% переваги ворога
                        counters.add(new CounterStat(enemyHeroId, advantagePercent, matchCount));
                    }
                }
            }

            // Сортуємо від найбільшого advantagePercent до найменшого
            counters.sort((a, b) -> Double.compare(b.enemyWinRate(), a.enemyWinRate()));


            // Сортуємо від найсильнішого контрпіка (найбільша перевага) до найслабшого
            counters.sort((a, b) -> Double.compare(b.enemyWinRate(), a.enemyWinRate()));

            StringBuilder resultText = new StringBuilder();
            resultText.append("Контрпіки проти ").append(targetHeroName).append(":\n");

            int limit = Math.min(5, counters.size());
            for (int i = 0; i < limit; i++) {
                CounterStat c = counters.get(i);
                String name = openDotaService.getHeroNameById(c.vsId());

                // Змінили текст із "Вінрейт" на "Перевага"
                resultText.append(String.format("%d. %s (Перевага: +%.1f%%, матчів: %d)\n",
                        i + 1, name, c.enemyWinRate(), c.matchCount()));
            }

            return resultText.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Сталася помилка при обробці контрпіків.";
        }
    }

    @Tool(description = """
        Отримує статистику популярності героя на різних позиціях (ролях) за його ID.
        Повертає відсоток ігор на кожній лінії (Керрі, Мід, Офлейн, Сапорт) та вінрейт для кожної ролі.
        Допомагає визначити, де зараз найчастіше і найефективніше грає цей герой у поточній меті.
        """)
    public String getHeroRoleDistribution(int heroId) {
        String targetHeroName = openDotaService.getHeroNameById(heroId);

        if (targetHeroName == null || targetHeroName.startsWith("Hero ID")) {
            return "Героя з ID " + heroId + " не знайдено.";
        }

        // GraphQL запит з групуванням по позиціях
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
            String jsonResponse = restClient.post()
                    .body(Map.of("query", graphqlQuery))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode statsArray = root.path("data").path("heroStats").path("stats");

            if (statsArray.isMissingNode() || statsArray.isEmpty()) {
                return "Немає статистики ігор для героя " + targetHeroName + ".";
            }

            // Внутрішній клас для зберігання даних
            record RoleStat(String position, int matchCount, double winRate) {}
            List<RoleStat> roles = new ArrayList<>();
            int totalMatches = 0;

            // 1. Парсимо JSON і рахуємо загальну кількість ігор
            for (JsonNode node : statsArray) {
                // Якщо поле position = null, це може бути агрегована статистика, пропускаємо її
                if (node.path("position").isNull()) continue;

                String position = node.path("position").asText();
                int matchCount = node.path("matchCount").asInt();
                int winCount = node.path("winCount").asInt();

                if (matchCount > 0) {
                    double winRate = ((double) winCount / matchCount) * 100.0;
                    roles.add(new RoleStat(position, matchCount, winRate));
                    totalMatches += matchCount;
                }
            }

            if (totalMatches == 0) return "Недостатньо даних про матчі.";

            // 2. Сортуємо від найпопулярнішої позиції до найменш популярної
            roles.sort((a, b) -> Integer.compare(b.matchCount(), a.matchCount()));

            // 3. Формуємо красивий текст для ШІ
            StringBuilder result = new StringBuilder();
            result.append("📊 Розподіл ролей для ").append(targetHeroName).append(" (Всього ігор: ").append(totalMatches).append("):\n");

            for (RoleStat r : roles) {
                // Рахуємо, який відсоток від усіх ігор припадає на цю позицію
                double pickRate = ((double) r.matchCount() / totalMatches) * 100.0;

                // Відсіюємо позиції, де герой з'являється рідше ніж у 2% випадків (похибка або випадкові піки)
                if (pickRate < 2.0) continue;

                String readablePosition = translatePosition(r.position());
                result.append(String.format("🔹 %s: %.1f%% ігор | Вінрейт: %.1f%%\n",
                        readablePosition, pickRate, r.winRate()));
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Помилка при отриманні позицій для героя.";
        }
    }

    // Допоміжний метод для перекладу системних позицій Stratz у людський формат
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