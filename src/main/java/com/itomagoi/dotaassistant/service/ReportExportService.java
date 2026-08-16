package com.itomagoi.dotaassistant.service;

import com.itomagoi.dotaassistant.model.HeroStats;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportExportService {

    private final OpenDotaService openDotaService;

    public ReportExportService(OpenDotaService openDotaService) {
        this.openDotaService = openDotaService;
    }

    public String exportGlobalMetaToFile() {
        String filename = "global_meta_report.txt";

        try {
            List<HeroStats> statsList = openDotaService.getAllHeroStats();

            if (statsList == null || statsList.isEmpty()) {
                return "Не вдалося отримати статистику героїв.";
            }

            // --- ДОДАНО: Сортування за вінрейтом (від найвищого до найнижчого) ---
            statsList.sort((s1, s2) -> {
                double wr1 = s1.getPubPick() > 0 ? (double) s1.getPubWin() / s1.getPubPick() : 0.0;
                double wr2 = s2.getPubPick() > 0 ? (double) s2.getPubWin() / s2.getPubPick() : 0.0;
                return Double.compare(wr2, wr1);
            });
            // ---------------------------------------------------------------------

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("=== ГЛОБАЛЬНА МЕТА ДОТИ (Зібрано: " + LocalDateTime.now() + ") ===");
                writer.println(String.format("%-20s | %-12s | %-10s | %-10s", "Герой", "Загалом ігор", "Перемог", "Вінрейт"));
                writer.println("------------------------------------------------------------------");

                for (HeroStats stat : statsList) {
                    int pick = stat.getPubPick();
                    int win = stat.getPubWin();

                    double winrate = pick > 0 ? ((double) win / pick) * 100 : 0.0;

                    writer.println(String.format("%-20s | %-12d | %-10d | %.1f%%",
                            stat.getLocalizedName(), pick, win, winrate));
                }
            }

            return "Глобальну мету успішно збережено у файл: " + filename;

        } catch (Exception e) {
            return "Помилка при експорті мети: " + e.getMessage();
        }
    }
}
