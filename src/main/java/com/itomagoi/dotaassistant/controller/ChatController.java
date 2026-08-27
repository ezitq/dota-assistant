package com.itomagoi.dotaassistant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.itomagoi.dotaassistant.model.MatchPlayerDetail;
import com.itomagoi.dotaassistant.model.PostMatchSummary;
import com.itomagoi.dotaassistant.service.OpenDotaService;
import com.itomagoi.dotaassistant.service.StratzService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final OpenDotaService openDotaService;
    private final StratzService stratzService;


    public ChatController(ChatClient.Builder chatClientBuilder, OpenDotaService openDotaService, StratzService stratzService) {
        this.openDotaService = openDotaService;
        this.stratzService = stratzService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                            Ти — професійний AI-аналітик Dota 2. Твоя мета — допомагати гравцям.
                        
                            СУВОРІ ПРАВИЛА ФОРМАТУВАННЯ ТЕКСТУ (КРИТИЧНО ВАЖЛИВО):
                            1. Використовуй ТІЛЬКИ HTML-теги для форматування (<b>, <br>, <h3>, <ul>, <li>).
                            2. КАТЕГОРИЧНО ЗАБОРОНЕНО використовувати Markdown (* або #). 
                            3. НІКОЛИ не обгортай свою відповідь у блоки коду (```). Твоя відповідь піде напряму в innerHTML.
                        
                            ПРАВИЛО ФОРМАТУВАННЯ АНАЛІЗУ МАТЧУ:
                            Твоя відповідь на розбір матчу ОБОВ'ЯЗКОВО має складатися з двох частин:
                            1. HTML-дошка матчу.
                            2. Текстова аналітика за суворим шаблоном.
                        
                            --- ЧАСТИНА 1: HTML-ДОШКА ---
                            <div class="match-board-container">
                                <div class="match-summary-title">Перемога <b>[ПЕРЕМОЖЕЦЬ]</b> | Тривалість: [ЧАС хв]</div>
                                <div class="match-board">
                                    <div class="team-col">
                                        <div class="team-header radiant-header">RADIANT</div>
                                        <div class="heroes-row">
                                            <!-- Згенеруй 5 блоків -->
                                            <div class="hero-box" data-hero="[ГЕРОЙ]" data-nick="[НІК]" data-account-id="[ACCOUNT_ID]" data-kda="[K]/[D]/[A]" data-nw="[NW]" data-gpm="[GPM]" data-xpm="[XPM]" data-dmg="[DMG]" data-items="[ПРЕДМЕТИ]" onclick="openHeroInfo(this)">[ГЕРОЙ]</div>
                                        </div>
                                    </div>
                                    <div class="team-col">
                                        <div class="team-header dire-header">DIRE</div>
                                        <div class="heroes-row">
                                            <!-- Згенеруй 5 блоків -->
                                            <div class="hero-box" ... onclick="openHeroInfo(this)">[ГЕРОЙ]</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="hero-info-panel"></div>
                            </div>
                        
                            --- ЧАСТИНА 2: ТЕКСТОВА АНАЛІТИКА ---
                            Відразу під дошкою виведи аналіз СУВОРО за цим HTML-шаблоном:
                        
                            <div style="margin-top: 20px;">
                                <h3 style="color: #b388ff; margin-bottom: 10px;">🏆 Рекорди матчу</h3>
                                <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                    <li><span style="color: #ffd54f;">💰 Топ нетворс:</span> <b>[Герой]</b> ([Значення] золота)</li>
                                    <li><span style="color: #ff8a80;">⚔️ Найбільше шкоди:</span> <b>[Герой]</b> ([Значення] шкоди)</li>
                                    <li><span style="color: #00ff88;">🎯 Найкраще KDA:</span> <b>[Герой]</b> ([K/D/A])</li>
                                </ul>
                        
                                <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">🗺️ Стадія ліній (Laning Phase)</h3>
                                <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                    <li><b>Топ (Top):</b> [Хто виграв лінію та коротко чому (наприклад: перефарм, вбивства)]</li>
                                    <li><b>Мід (Mid):</b> [Хто виграв лінію та коротко чому]</li>
                                    <li><b>Бот (Bot):</b> [Хто виграв лінію та коротко чому]</li>
                                </ul>
                        
                                <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">💡 Чому перемогли [ПЕРЕМОЖЕЦЬ]?</h3>
                                <div style="color: #e0e6ed; line-height: 1.5; background: rgba(255,255,255,0.02); padding: 12px; border-radius: 8px;">
                                    [Напиши короткий, влучний абзац на 3-4 речення з головними причинами перемоги команди: перевага в економіці, кращий драфт, критичні помилки опонентів у лейті тощо.]
                                </div>
                            </div>
                        
                        ПРАВИЛО ФОРМАТУВАННЯ ПРОФІЛЮ ГРАВЦЯ:
                            Коли ти аналізуєш профіль гравця (Player Summary), ти ОБОВ'ЯЗКОВО повинен використовувати цей суворий HTML-шаблон. Ніколи не використовуй звичайний текст!
                        
                            ШАБЛОН ПРОФІЛЮ:
                            <div style="display: flex; align-items: center; gap: 15px; margin-bottom: 20px;">
                                <img src="[AVATAR_URL_АБО_СТАНДАРТНА_КАРТИНКА]" style="width: 70px; height: 70px; border-radius: 50%; border: 2px solid #b388ff; object-fit: cover;">
                                <div>
                                    <h2 style="color: #fff; margin: 0; font-size: 1.4rem;">👤 Профіль гравця: [NICKNAME]</h2>
                                    <div style="color: #ffd54f; font-weight: bold; margin-top: 4px;">Ранг: [RANK_NAME]</div>
                                    <div style="color: #8892b0; font-size: 0.9rem; margin-top: 2px;">Усього матчів: [TOTAL_MATCHES] | Загальний вінрейт: [GLOBAL_WINRATE]%</div>
                                </div>
                            </div>
                        
                            <h3 style="color: #b388ff; margin-bottom: 10px;">📊 Поточна форма та пул героїв</h3>
                            <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                <li>📉 <span style="color: #ff8a80;">Поточний вінрейт (recent winrate):</span> <b>[RECENT_WINRATE]%</b> <i>([ТВІЙ_КОМЕНТАР_ЩОДО_СПАДУ_ЧИ_ПІДЙОМУ])</i></li>
                                <li>🗺️ <span style="color: #00ff88;">Пріоритетна роль:</span> <b>[УЛЮБЛЕНА_ЛІНІЯ]</b> — <i>[ТВІЙ_КОМЕНТАР_ПРО_ГНУЧКІСТЬ_ЧИ_ВУЗЬКИЙ_ПУЛ]</i></li>
                            </ul>
                        
                            <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">🔥 Найкращі сигнатурні герої за весь час</h3>
                            <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                <!-- Згенеруй список героїв -->
                                <li><b>[ГЕРОЙ]:</b> [ІГОР] ігор, <b>[ВІНРЕЙТ]%</b> вінрейт</li>
                            </ul>
                            <div style="font-style: italic; color: #8892b0; font-size: 0.9rem; margin-top: 8px;">Примітка: [ТВІЙ_АНАЛІТИЧНИЙ_КОМЕНТАР_ПРО_СИГНАТУРКИ]</div>
                        
                            <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">⚡ Актуальні герої (Останній час)</h3>
                            <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                <!-- Використовуй емодзі ⭐ (успішні), ⚡ (нейтральні), ⚠️ (погані) залежно від вінрейту -->
                                <li>[ЕМОДЗІ] <b style="color: #00ff88;">[ГЕРОЙ]</b>: [ІГОР] ігор, <b>[ВІНРЕЙТ]%</b> вінрейт <i>([ТВІЙ_КОМЕНТАР])</i></li>
                            </ul>
                        
                            <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">📈 Короткий аналітичний висновок</h3>
                            <div style="color: #e0e6ed; line-height: 1.5; background: rgba(255,255,255,0.02); padding: 12px; border-radius: 8px; border-left: 4px solid #b388ff;">
                                [РОЗГОРНУТИЙ ВИСНОВОК АНАЛІТИКА НА 3-4 РЕЧЕННЯ. Поради для підняття рейтингу, на чому сфокусуватися, чого уникати.]
                            </div>
                        
                        ПРАВИЛО ФОРМАТУВАННЯ МЕТИ (ТІР-ЛИСТ ГЕРОЇВ):
                            Коли користувач питає про поточну мету, сильних героїв або тір-лист, ти ОБОВ'ЯЗКОВО повинен використовувати цей HTML-шаблон.
                            Знайди найкращих героїв для кожної позиції (за вінрейтом та популярністю) і виведи їх у вигляді списку.
                        
                            ШАБЛОН МЕТИ:
                            <div style="background: rgba(15, 8, 25, 0.6); border: 1px solid rgba(179, 136, 255, 0.2); border-radius: 12px; padding: 20px; margin-top: 15px;">
                                <h2 style="color: #fff; margin-top: 0; margin-bottom: 5px; text-align: center;">🔥 Поточна Мета (Divine / Immortal)</h2>
                                <div style="text-align: center; color: #8892b0; font-size: 0.9rem; margin-bottom: 20px;">На основі найвищого вінрейту та частоти піків</div>
                        
                                <!-- Керрі (POSITION_1) -->
                                <h3 style="color: #ffd54f; margin-bottom: 10px; border-bottom: 1px solid rgba(255, 213, 79, 0.2); padding-bottom: 4px;">⚔️ Керрі (Safe Lane)</h3>
                                <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6; margin-bottom: 15px;">
                                    <li><b style="color: #fff;">1. [ГЕРОЙ]</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>(Коротко: чому він зараз сильний)</i></li>
                                    <li><b style="color: #fff;">2. [ГЕРОЙ]</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>(Коротко: чому він зараз сильний)</i></li>
                                    <li><b style="color: #fff;">3. [ГЕРОЙ]</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>(Коротко: чому він зараз сильний)</i></li>
                                </ul>
                        
                                <!-- Мідлейн (POSITION_2) -->
                                <h3 style="color: #ff8a80; margin-bottom: 10px; border-bottom: 1px solid rgba(255, 138, 128, 0.2); padding-bottom: 4px;">🎯 Мідлейн (Mid)</h3>
                                <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6; margin-bottom: 15px;">
                                    <li><b style="color: #fff;">1. [ГЕРОЙ]</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>([КОМЕНТАР])</i></li>
                                    <!-- Додай ще 2 героїв -->
                                </ul>
                        
                                <!-- Офлейн (POSITION_3) -->
                                <h3 style="color: #b388ff; margin-bottom: 10px; border-bottom: 1px solid rgba(179, 136, 255, 0.2); padding-bottom: 4px;">🛡️ Офлейн (Hard Lane)</h3>
                                <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6; margin-bottom: 15px;">
                                    <li><b style="color: #fff;">1. [ГЕРОЙ]</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>([КОМЕНТАР])</i></li>
                                    <!-- Додай ще 2 героїв -->
                                </ul>
                        
                                <!-- Саппорти (POSITION_4 та POSITION_5) -->
                                <h3 style="color: #00ff88; margin-bottom: 10px; border-bottom: 1px solid rgba(0, 255, 136, 0.2); padding-bottom: 4px;">⚕️ Саппорти (Soft & Hard)</h3>
                                <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                    <li><b style="color: #fff;">1. [ГЕРОЙ] (Поз 4)</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>([КОМЕНТАР])</i></li>
                                    <li><b style="color: #fff;">2. [ГЕРОЙ] (Поз 5)</b> — <span style="color: #00ff88;">[ВІНРЕЙТ]% WR</span> <i>([КОМЕНТАР])</i></li>
                                    <!-- Додай ще 2-3 героїв -->
                                </ul>
                            </div>                        
                            Відповідай українською мовою.
                        """)
                .build();

    }

    @PostMapping
    public Map<String, String> processChatMessage(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Map.of("response", "Повідомлення не може бути порожнім.");
        }

        try {
            // Спроба 1: Звертаємося до Gemini
            String aiResponse = chatClient.prompt()
                    .user(userMessage)
                    .tools(openDotaService, stratzService)
                    .call()
                    .content();

            return Map.of("response", aiResponse);

        } catch (Exception e) {
            System.err.println("Gemini API Error: " + e.getMessage());

            // Спроба 2: FALLBACK. Якщо Gemini впав, пробуємо розпарсити самі
            String fallbackResponse = handleFallback(userMessage);
            return Map.of("response", fallbackResponse);
        }
    }

    // --- ЛОГІКА ЗАПАСНОГО ВАРІАНТУ (FALLBACK) ---

    private String handleFallback(String message) {
        String lowerMsg = message.toLowerCase();

        // 1. ПЕРЕВІРКА НА ЗАПИТ ПРО МАТЧ
        Pattern pattern = Pattern.compile("(?i)(матч|гра|match)\\s*(\\d{8,})");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            try {
                long matchId = Long.parseLong(matcher.group(2));
                PostMatchSummary summary = openDotaService.getPostMatchSummary(matchId);
                return "<i style='color:#ffaa00; font-size: 0.85rem;'>⚠️ ШІ відпочиває. Використовую пряме завантаження матчу:</i><br><br>"
                        + buildManualHtmlBoard(summary) + buildManualAnalysis(summary);
            } catch (Exception ex) {
                return "Не вдалося завантажити цей матч напряму. Перевір, чи правильний ID.";
            }
        }

        // 2. ПЕРЕВІРКА НА ЗАПИТ ПРО МЕТУ
        if (lowerMsg.contains("мет") || lowerMsg.contains("геро") || lowerMsg.contains("мід") || lowerMsg.contains("тір")) {
            try {
                String stratzJson = stratzService.getStratzMetaHeroes();
                return "<i style='color:#ffaa00; font-size: 0.85rem;'>⚠️ ШІ відпочиває. Згенерував тір-лист напряму з бази Stratz:</i><br><br>"
                        + buildManualMetaHtml(stratzJson);
            } catch (Exception ex) {
                ex.printStackTrace();
                return "Не вдалося завантажити статистику мети. Спробуй пізніше.";
            }
        }

        return "⚠️ Наразі мій штучний інтелект вичерпав ліміт запитів (помилка 429). Спробуй пізніше!";
    }

    // Метод парсингу JSON від Stratz
    private String buildManualMetaHtml(String jsonString) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);
        JsonNode data = root.has("data") ? root.get("data") : root;

        StringBuilder html = new StringBuilder();
        html.append("<div style=\"background: rgba(15, 8, 25, 0.6); border: 1px solid rgba(179, 136, 255, 0.2); border-radius: 12px; padding: 20px; margin-top: 15px;\">");
        html.append("<h2 style=\"color: #fff; margin-top: 0; margin-bottom: 5px; text-align: center;\">🔥 Поточна Мета (Divine / Immortal)</h2>");
        html.append("<div style=\"text-align: center; color: #8892b0; font-size: 0.9rem; margin-bottom: 20px;\">Пряме завантаження (Fallback Mode)</div>");

        // Парсимо кожну позицію окремо (використовуємо аліаси з нашого GraphQL запиту)
        html.append(buildPositionListHtml(data.get("pos1"), "⚔️ Керрі (Safe Lane)", "#ffd54f"));
        html.append(buildPositionListHtml(data.get("pos2"), "🎯 Мідлейн (Mid)", "#ff8a80"));
        html.append(buildPositionListHtml(data.get("pos3"), "🛡️ Офлейн (Hard Lane)", "#b388ff"));
        html.append(buildPositionListHtml(data.get("pos4"), "⚕️ Семі-саппорт (Soft Support)", "#00ff88"));
        html.append(buildPositionListHtml(data.get("pos5"), "⚕️ Фул-саппорт (Hard Support)", "#00ff88"));

        html.append("</div>");
        return html.toString();
    }

    // Внутрішній клас для зручного сортування
    private record HeroStat(int heroId, int matchCount, double winRate) {}

    // Метод генерації блоку для однієї лінії
    private String buildPositionListHtml(JsonNode posNode, String title, String color) {
        if (posNode == null || !posNode.has("stats")) return "";

        List<HeroStat> stats = new ArrayList<>();

        for (JsonNode statNode : posNode.get("stats")) {
            int matchCount = statNode.get("matchCount").asInt();
            if (matchCount < 50) continue; // Фільтруємо непопулярні піки

            int winCount = statNode.get("winCount").asInt();
            int heroId = statNode.get("heroId").asInt();
            double winRate = ((double) winCount / matchCount) * 100.0;

            stats.add(new HeroStat(heroId, matchCount, winRate));
        }

        // Сортуємо за вінрейтом (від найбільшого до найменшого)
        stats.sort((a, b) -> Double.compare(b.winRate(), a.winRate()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<h3 style=\"color: %s; margin-bottom: 10px; border-bottom: 1px solid %s33; padding-bottom: 4px;\">%s</h3>", color, color, title));
        sb.append("<ul style=\"list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6; margin-bottom: 15px;\">");

        // Беремо топ-3 героїв
        int limit = Math.min(3, stats.size());
        for (int i = 0; i < limit; i++) {
            HeroStat s = stats.get(i);
            String heroName = getHeroName(s.heroId());
            sb.append(String.format("<li><b style=\"color: #fff;\">%d. %s</b> — <span style=\"color: #00ff88;\">%.1f%% WR</span> <i>(%d ігор)</i></li>",
                    i + 1, heroName, s.winRate(), s.matchCount()));
        }
        sb.append("</ul>");

        return sb.toString();
    }

    private String getHeroName(int id) {
        return switch (id) {
            case 1 -> "Anti-Mage"; case 2 -> "Axe"; case 3 -> "Bane"; case 4 -> "Bloodseeker";
            case 5 -> "Crystal Maiden"; case 6 -> "Drow Ranger"; case 7 -> "Earthshaker";
            case 8 -> "Juggernaut"; case 9 -> "Mirana"; case 10 -> "Morphling";
            case 11 -> "Shadow Fiend"; case 14 -> "Pudge"; case 17 -> "Storm Spirit";
            case 25 -> "Lina"; case 26 -> "Lion"; case 90 -> "Keeper of the Light";
            case 106 -> "Ember Spirit"; case 107 -> "Earth Spirit";
            // Якщо героя немає в базі, виведемо його ID, хоча в ідеалі сюди варто підключити JSON з усіма героями
            default -> "Hero ID: " + id;
        };
    }

    // Генерація HTML-дошки точно в такому форматі, як це робив би Gemini
    private String buildManualHtmlBoard(PostMatchSummary summary) {
        String winner = summary.isRadiantWin() ? "<span style='color:#00ff88;'>RADIANT</span>" : "<span style='color:#ff4c4c;'>DIRE</span>";
        int durationMins = summary.getDurationSeconds() / 60;
        int durationSecs = summary.getDurationSeconds() % 60;
        String time = String.format("%02d:%02d", durationMins, durationSecs);

        StringBuilder html = new StringBuilder();
        html.append("<div class='match-board-container'>");
        html.append(String.format("<div class='match-summary-title'>Перемога %s | Тривалість: %s</div>", winner, time));
        html.append("<div class='match-board'>");

        // Колонка Radiant
        html.append("<div class='team-col'><div class='team-header radiant-header'>RADIANT</div><div class='heroes-row'>");
        for (MatchPlayerDetail p : summary.getRadiantPlayers()) {
            html.append(buildHeroBox(p));
        }
        html.append("</div></div>");

        // Колонка Dire
        html.append("<div class='team-col'><div class='team-header dire-header'>DIRE</div><div class='heroes-row'>");
        for (MatchPlayerDetail p : summary.getDirePlayers()) {
            html.append(buildHeroBox(p));
        }
        html.append("</div></div>");

        html.append("</div><div class='hero-info-panel'></div></div>");
        return html.toString();
    }

    private String buildHeroBox(MatchPlayerDetail p) {
        String nick = (p.personaName() != null && !p.personaName().equals("Anonymous")) ? p.personaName() : "Приховано";
        String kda = p.kills() + "/" + p.deaths() + "/" + p.assists();

        // Збираємо імена предметів в один рядок
        String items = p.inventory().stream()
                .filter(i -> i.id() != 0)
                .map(MatchPlayerDetail.ItemDto::name)
                .collect(Collectors.joining(", "));
        if (items.isEmpty()) items = "Немає предметів";

        return String.format(
                "<div class='hero-box' data-hero='%s' data-nick='%s' data-kda='%s' data-nw='%d' data-gpm='%d' data-xpm='%d' data-dmg='%d' data-items='%s' onclick='openHeroInfo(this)'>%s</div>",
                escape(p.heroName()), escape(nick), kda, p.netWorth(), p.gpm(), p.xpm(), p.heroDamage(), escape(items), escape(p.heroName())
        );
    }

    // Допоміжний метод, щоб уникнути поломки HTML через одинарні лапки у назвах предметів/ніках
    private String escape(String input) {
        if (input == null) return "";
        return input.replace("'", "&#39;").replace("\"", "&quot;");
    }

    private String buildManualAnalysis(PostMatchSummary summary) {
        // Збираємо всіх гравців в один список для пошуку рекордів
        List<MatchPlayerDetail> allPlayers = new ArrayList<>();
        allPlayers.addAll(summary.getRadiantPlayers());
        allPlayers.addAll(summary.getDirePlayers());

        // Знаходимо гравця з найбільшим нетворсом
        MatchPlayerDetail topNwPlayer = allPlayers.stream()
                .max(Comparator.comparing(MatchPlayerDetail::netWorth))
                .orElse(allPlayers.get(0));

        // Знаходимо гравця з найбільшою шкодою
        MatchPlayerDetail topDmgPlayer = allPlayers.stream()
                .max(Comparator.comparing(MatchPlayerDetail::heroDamage))
                .orElse(allPlayers.get(0));

        // Знаходимо гравця з найкращим KDA
        MatchPlayerDetail bestKdaPlayer = allPlayers.stream()
                .max(Comparator.comparing(MatchPlayerDetail::kdaRatio))
                .orElse(allPlayers.get(0));

        String winnerStr = summary.isRadiantWin() ? "RADIANT" : "DIRE";

        // Формуємо HTML за нашим стандартом, підставляючи знайдені рекорди
        return String.format("""
                        <div style="margin-top: 20px;">
                            <h3 style="color: #b388ff; margin-bottom: 10px;">🏆 Рекорди матчу</h3>
                            <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                <li><span style="color: #ffd54f;">💰 Топ нетворс:</span> <b>%s</b> (%d золота)</li>
                                <li><span style="color: #ff8a80;">⚔️ Найбільше шкоди:</span> <b>%s</b> (%d шкоди)</li>
                                <li><span style="color: #00ff88;">🎯 Найкраще KDA:</span> <b>%s</b> (%d/%d/%d)</li>
                            </ul>
                        
                            <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">🗺️ Стадія ліній (Laning Phase)</h3>
                            <ul style="list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.6;">
                                <li><b>Топ (Top):</b> невідомо.</li>
                                <li><b>Мід (Mid):</b> невідомо.</li>
                                <li><b>Бот (Bot):</b> невідомо.</li>
                            </ul>
                        
                            <h3 style="color: #b388ff; margin-top: 15px; margin-bottom: 10px;">💡 Чому перемогли %s?</h3>
                            <div style="color: #e0e6ed; line-height: 1.5; background: rgba(255,255,255,0.02); padding: 12px; border-radius: 8px; border-left: 4px solid #b388ff;">
                                Ліміт ШІ помічника вичерпаний, тому детальний аналіз матчу наразі недоступний.
                            </div>
                        </div>
                        """,
                topNwPlayer.heroName(), topNwPlayer.netWorth(),
                topDmgPlayer.heroName(), topDmgPlayer.heroDamage(),
                bestKdaPlayer.heroName(), bestKdaPlayer.kills(), bestKdaPlayer.deaths(), bestKdaPlayer.assists(),
                winnerStr
        );
    }
    private String buildManualCountersHtml(String jsonString, String targetHero) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);

        // 1. Знаходимо масив matchUp
        JsonNode matchUpArray = root.path("data").path("heroStats").path("matchUp");

        record CounterStat(int vsId, double enemyWinRate, int matchCount) {}
        List<CounterStat> counters = new ArrayList<>();

        // 2. Перевіряємо, чи повернувся масив і чи він не порожній
        if (matchUpArray.isArray() && matchUpArray.size() > 0) {

            // 3. Беремо перший об'єкт з matchUp і дістаємо з нього масив vs
            JsonNode vsArray = matchUpArray.get(0).path("vs");

            if (vsArray.isArray()) {
                for (JsonNode node : vsArray) {
                    int matchCount = node.path("matchCount").asInt();
                    if (matchCount < 50) continue; // Фільтр непопулярних матч-апів

                    int h2 = node.path("heroId2").asInt();
                    double enemyWinRate = node.path("winRateHeroId2").asDouble();
                    double normalizedWinRate = enemyWinRate < 1.0 ? enemyWinRate * 100 : enemyWinRate;

                    // Якщо ворог перемагає частіше ніж у 50% ігор - це контрпік
                    if (normalizedWinRate > 50.0) {
                        counters.add(new CounterStat(h2, normalizedWinRate, matchCount));
                    }
                }
            }
        }

        counters.sort((a, b) -> Double.compare(b.enemyWinRate(), a.enemyWinRate()));

        StringBuilder html = new StringBuilder();
        html.append("<div style=\"background: rgba(20, 10, 25, 0.7); border: 1px solid rgba(255, 76, 76, 0.3); border-radius: 12px; padding: 20px; margin-top: 15px;\">");
        html.append(String.format("<h2 style=\"color: #fff; margin-top: 0;\">🛡️ Контрпіки проти <b>%s</b></h2>", targetHero.toUpperCase()));
        html.append("<ul style=\"list-style-type: none; padding-left: 0; color: #e0e6ed; line-height: 1.8;\">");

        int limit = Math.min(5, counters.size());
        for (int i = 0; i < limit; i++) {
            CounterStat c = counters.get(i);
            String counterHeroName = getHeroName(c.vsId());

            html.append(String.format("""
                <li style="border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 8px; margin-bottom: 8px;">
                    <b style="color: #ff4c4c; font-size: 1.1rem;">%d. %s</b> 
                    <span style="color: #ffd54f; margin-left: 8px;">(Вінрейт проти цілі: %.1f%%)</span>
                </li>
                """, i + 1, counterHeroName, c.enemyWinRate()));
        }

        html.append("</ul></div>");
        return html.toString();
    }
}