package ru.sbrf.school.classloader.ring;

import ru.sbrf.school.classloader.RockPaperScissorsEnum;
import ru.sbrf.school.classloader.api.PlayableRockPaperScissors;
import ru.sbrf.school.classloader.ring.exceptions.RingException;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.sbrf.school.classloader.RockPaperScissorsEnum.*;

public class MainRingApplication {

    private static final HashMap<String, Integer> winners = new HashMap<>();

    public static void main(String[] args) {
        List<PlayableRockPaperScissors> plugins = getRpcPlugins();
        PlayableRockPaperScissors lastWinner = null;

        for (int round = 0; round < 5; round++) {
            System.out.println("round " + (round + 1));
            if (plugins.size() < 2) {
                // подгрузка
                plugins = getRpcPlugins();
                plugins.remove(lastWinner);
            }
            if (plugins.size() > 1) {
                PlayableRockPaperScissors player1 = null;
                if (plugins.get(0) != lastWinner) {
                    player1 = plugins.get(0);
                } else {
                    player1 = plugins.get(1);
                }
                PlayableRockPaperScissors player2 = null;
                if (lastWinner != null) {
                    player2 = lastWinner;
                } else {
                    player2 = plugins.get(1);
                }
                RockPaperScissorsEnum result1 = player1.play();
                RockPaperScissorsEnum result2 = player2.play();
                RockPaperScissorsEnum winner = getWinner(result1, result2);
                if (winner == null) {
                    System.out.println("ничья");
                    printScore();
                    continue;
                } else if (winner.equals(result1)) {
                    lastWinner = player1;
                    plugins.remove(player2);
                    System.out.println("победил " + player1.getName());
                    putWinner(player1.getName());
                } else {
                    lastWinner = player2;
                    plugins.remove(player1);
                    System.out.println("победил " + player2.getName());
                    putWinner(player2.getName());
                }
                printScore();
            } else {
                throw new RingException("Игра невозможна. Количество игроков: " + plugins.size());
            }
        }
    }

    private static void putWinner(String name) {
        winners.merge(name, 1, Integer::sum);
    }

    private static void printScore() {
        System.out.println("Счет:");
        for (Map.Entry<String, Integer> entry: winners.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println();
    }

    private static List<PlayableRockPaperScissors> getRpcPlugins() {
        List<PlayableRockPaperScissors> plugins = getPlugins().stream().map(plugin -> {
            try {
                return (PlayableRockPaperScissors) plugin.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        if (plugins.size() < 2) {
            throw new RingException("Игра невозможна. Количество игроков: " + plugins.size());
        }
        return plugins;
    }

    /**
     * Получить победителя
     * @param first
     * @param second
     * @return возвращает победителя, а если ничья, то null
     */
    private static RockPaperScissorsEnum getWinner(RockPaperScissorsEnum first, RockPaperScissorsEnum second) {
        if (first.equals(ROCK)) {
            if (second.equals(SCISSORS)) {
                return first;
            } else if (second.equals(PAPER)) {
                return second;
            }
        }
        if (first.equals(SCISSORS)) {
            if (second.equals(PAPER)) {
                return first;
            } else if (second.equals(ROCK)) {
                return second;
            }
        }
        if (first.equals(PAPER)) {
            if (second.equals(ROCK)) {
                return first;
            } else if (second.equals(SCISSORS)) {
                return second;
            }
        }
        return null;
    }

    /**
     * @return Получаем весь список плагинов-классов
     */
    private static List<Class> getPlugins() {
        File[] jars = getPluginFiles();

        Class[] pluginClasses = new Class[jars.length];

        for (int i = 0; i < jars.length; i++) {
            try {
                URL jarURL = jars[i].toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{jarURL});
                pluginClasses[i] = classLoader.loadClass("ru.sbrf.school.classloader.player.RockPaperScissorsPlayer");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return Arrays.stream(pluginClasses).collect(Collectors.toList());
    }


    /**
     *
     * @return список файлов-плагинов из папки для плагинов
     */
    private static File[] getPluginFiles() {
        File pluginDir = new File("plugins");

        return pluginDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
    }

}
