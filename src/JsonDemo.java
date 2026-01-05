import automaton.config.JsonAutomatonLoader;
import automaton.core.CoreProbabilisticAutomaton;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Пример загрузки автомата из JSON-конфигурации.
 * Читает файл automaton_config.json и запускает алгоритм "check".
 */
public class JsonDemo {
    public static void main(String[] args) throws Exception {
        JsonAutomatonLoader loader = new JsonAutomatonLoader();

        try (InputStream in = new FileInputStream("automaton_config.json")) {
            CoreProbabilisticAutomaton automaton = loader.load(in);

            for (int i = 0; i < 5; i++) {
                System.out.println("==== JSON run #" + (i + 1) + " ====");
                automaton.executeCurrentStateAlgorithm("check");
                automaton.printStatus();
                automaton.reset();
                System.out.println();
            }
        }
    }
}
