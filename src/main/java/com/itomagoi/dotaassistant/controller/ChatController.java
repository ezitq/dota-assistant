package com.itomagoi.dotaassistant.controller;

import com.itomagoi.dotaassistant.model.MatchPlayerDetail;
import com.itomagoi.dotaassistant.model.PostMatchSummary;
import com.itomagoi.dotaassistant.service.OpenDotaService;
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

    public ChatController(ChatClient.Builder chatClientBuilder, OpenDotaService openDotaService) {
        this.openDotaService = openDotaService;
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
                    .tools(openDotaService)
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
        Pattern pattern = Pattern.compile("(?i)(матч|гра|match)\\s*(\\d{8,})");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            try {
                long matchId = Long.parseLong(matcher.group(2));
                PostMatchSummary summary = openDotaService.getPostMatchSummary(matchId);

                // Збираємо все разом: Попередження + Дошка матчу + Рекорди/Заглушки
                return "<i style='color:#ffaa00; font-size: 0.85rem;'>⚠️ ШІ тимчасово недоступний. Використовую пряме завантаження даних:</i><br><br>"
                        + buildManualHtmlBoard(summary)
                        + buildManualAnalysis(summary); // Додали цей рядок

            } catch (Exception ex) {
                return "AI відпочиває, і мені не вдалося завантажити цей матч напряму. Перевір, чи правильний ID.";
            }
        }

        return "⚠️ Наразі мій штучний інтелект вичерпав ліміт запитів (помилка 429). Спробуй пізніше!";
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
}