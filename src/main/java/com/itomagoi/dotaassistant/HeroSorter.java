package com.itomagoi.dotaassistant;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeroSorter {

    // 1. Оновлений об'єкт для збереження імені, кількості ігор та відсотка виграшів
    static class Hero {
        String name;
        int gamesCount;
        double winRate;

        public Hero(String name, int gamesCount, double winRate) {
            this.name = name;
            this.gamesCount = gamesCount;
            this.winRate = winRate;
        }

        @Override
        public String toString() {
            // Форматований вивід: Ім'я (20 симовлів) | Ігри (10 символів) | Відсоток
            return String.format("%-20s | Matches: %-10d | Winrate: %.1f%%", name, gamesCount, winRate);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("global_meta_report.txt");

        // ОНОВЛЕНО: Додано групу (?<games>\d+) для першого числа після першої вертикальної риски
        String regex = "^(?<name>[^|]+?)\\s*\\|\\s*(?<games>\\d+)\\s*\\|.*\\|\\s*(?<percentage>[\\d,.]+)%";
        Pattern pattern = Pattern.compile(regex);
        List<Hero> heroList = new ArrayList<>();

        // 2. Читання файлу
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                Matcher matcher = pattern.matcher(scanner.nextLine());

                if (matcher.find()) {
                    String name = matcher.group("name").trim();

                    // ОНОВЛЕНО: Парсимо кількість зіграних ігор
                    int gamesCount = Integer.parseInt(matcher.group("games"));

                    String pctStr = matcher.group("percentage").replace(',', '.');
                    double winRate = Double.parseDouble(pctStr);

                    heroList.add(new Hero(name, gamesCount, winRate));
                }
            }
        }

        // 3. Сортування за спаданням відсотка виграшів (як і раніше)
        heroList.sort(Comparator.comparingDouble((Hero h) -> h.winRate).reversed());

        // 4. Виведення результатів з новими даними
        System.out.println("--- Sorted by Percentage (Highest First) ---");
        for (Hero hero : heroList) {
            System.out.println(hero);
        }
    }
}
