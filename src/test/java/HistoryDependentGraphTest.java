import automaton.builder.AlgorithmBuilder;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.SimpleInputMessage;
import automaton.probability.HistoryProbabilityProvider;
import automaton.random.RandomProvider;
import automaton.state.State;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Пример многоуровневого графа с маршрутизацией на основе истории.
 *
 * Правило: если пришли в N3 из N1 -> переходим в N11,
 *          если пришли в N3 из N5 -> переходим в N12.
 */
public class HistoryDependentGraphTest {

    /** Детерминированный источник случайности для стабильных тестов. */
    static class SequenceRandomProvider implements RandomProvider {
        private final double[] seq;
        private int i = 0;

        SequenceRandomProvider(double... seq) {
            this.seq = seq;
        }

        @Override
        public double nextUnit() {
            if (seq.length == 0) return 0.0;
            double v = seq[Math.min(i, seq.length - 1)];
            i++;
            // Ограничиваем диапазон [0,1)
            if (v < 0.0) v = 0.0;
            if (v >= 1.0) v = Math.nextDown(1.0);
            return v;
        }
    }

    /**
     * Провайдер, возвращающий базовые веса для рёбер, но переопределяющий маршрутизацию для N3 на основе истории.
     * Возвращаются веса (не обязательно нормализованные) — команда нормализует их внутри.
     */
    static class GraphHistoryProbabilityProvider implements HistoryProbabilityProvider {

        @Override
        public double[] computeProbabilities(List<State> history,
                                             automaton.input.InputMessage input,
                                             List<State> nextStates) {
            State current = history.get(history.size() - 1);
            String cur = current.getName();

            // Маршрутизация на основе истории для N3
            if ("N3".equals(cur)) {
                String prev = history.size() >= 2 ? history.get(history.size() - 2).getName() : "";
                double[] w = new double[nextStates.size()];
                for (int i = 0; i < nextStates.size(); i++) {
                    String n = nextStates.get(i).getName();
                    if ("N11".equals(n) && "N1".equals(prev)) w[i] = 1.0;
                    else if ("N12".equals(n) && "N5".equals(prev)) w[i] = 1.0;
                    else if (!"N1".equals(prev) && !"N5".equals(prev)) w[i] = 1.0; // Запасной вариант: равномерное
                    else w[i] = 0.0;
                }
                return w;
            }

            // Базовые веса для остальных узлов (фиксированные)
            Map<String, Map<String, Double>> base = new HashMap<>();
            base.put("N0", Map.of("N1", 0.5, "N2", 0.5));
            base.put("N1", Map.of("N3", 0.6, "N4", 0.4));
            base.put("N2", Map.of("N5", 0.7, "N6", 0.3));
            base.put("N4", Map.of("N0", 0.2, "N7", 0.8));
            base.put("N5", Map.of("N9", 0.2, "N10", 0.2, "N3", 0.6));

            Map<String, Double> edges = base.getOrDefault(cur, Collections.emptyMap());
            double[] w = new double[nextStates.size()];
            for (int i = 0; i < nextStates.size(); i++) {
                w[i] = edges.getOrDefault(nextStates.get(i).getName(), 1.0);
            }
            return w;
        }
    }

    private static CoreProbabilisticAutomaton buildAutomaton(HistoryProbabilityProvider provider, RandomProvider rnd) {
        // Создаём состояния (листья — финальные)
        State N0  = new State("N0",  false);
        State N1  = new State("N1",  false);
        State N2  = new State("N2",  false);
        State N3  = new State("N3",  false);
        State N4  = new State("N4",  false);
        State N5  = new State("N5",  false);
        State N6  = new State("N6",  true);
        State N7  = new State("N7",  true);
        State N9  = new State("N9",  true);
        State N10 = new State("N10", true);
        State N11 = new State("N11", true);
        State N12 = new State("N12", true);

        // Прикрепляем алгоритмы ("tick") к нефинальным состояниям
        N0.addAlgorithm("tick", new AlgorithmBuilder("tick")
                .clearNextStates()
                .addExistingState(N1, N1.isFinal(), 0.5)
                .addExistingState(N2, N2.isFinal(), 0.5)
                .historyBasedTransition(provider, rnd)
                .build());

        N1.addAlgorithm("tick", new AlgorithmBuilder("tick")
                .clearNextStates()
                .addExistingState(N3, N3.isFinal(), 0.6)
                .addExistingState(N4, N4.isFinal(), 0.4)
                .historyBasedTransition(provider, rnd)
                .build());

        N2.addAlgorithm("tick", new AlgorithmBuilder("tick")
                .clearNextStates()
                .addExistingState(N5, N5.isFinal(), 0.7)
                .addExistingState(N6, N6.isFinal(), 0.3)
                .historyBasedTransition(provider, rnd)
                .build());

        N4.addAlgorithm("tick", new AlgorithmBuilder("tick")
                .clearNextStates()
                .addExistingState(N0, N0.isFinal(), 0.2)
                .addExistingState(N7, N7.isFinal(), 0.8)
                .historyBasedTransition(provider, rnd)
                .build());

        N5.addAlgorithm("tick", new AlgorithmBuilder("tick")
                .clearNextStates()
                .addExistingState(N9, N9.isFinal(), 0.2)
                .addExistingState(N10, N10.isFinal(), 0.2)
                .addExistingState(N3, N3.isFinal(), 0.6)
                .historyBasedTransition(provider, rnd)
                .build());

        N3.addAlgorithm("tick", new AlgorithmBuilder("tick")
                .clearNextStates()
                .addExistingState(N11, N11.isFinal(), 0.5)
                .addExistingState(N12, N12.isFinal(), 0.5)
                .historyBasedTransition(provider, rnd)
                .build());

        return new CoreProbabilisticAutomaton(N0);
    }

    @Test
    void ifEnteredN3FromN1_thenGoToN11() {
        HistoryProbabilityProvider provider = new GraphHistoryProbabilityProvider();
        RandomProvider rnd = new SequenceRandomProvider(
                0.10, // N0 -> N1
                0.10, // N1 -> N3
                0.99  // N3 -> N11 (провайдер делает выбор детерминированным)
        );
        CoreProbabilisticAutomaton a = buildAutomaton(provider, rnd);

        a.step("tick", new SimpleInputMessage("start", java.util.Map.of()));
        Assertions.assertEquals("N1", a.getCurrentStateName());

        a.step("tick", new SimpleInputMessage("next", java.util.Map.of()));
        Assertions.assertEquals("N3", a.getCurrentStateName());

        a.step("tick", new SimpleInputMessage("route", java.util.Map.of()));
        Assertions.assertEquals("N11", a.getCurrentStateName());
        Assertions.assertTrue(a.isInFinalState());
    }

    @Test
    void ifEnteredN3FromN5_thenGoToN12() {
        HistoryProbabilityProvider provider = new GraphHistoryProbabilityProvider();
        RandomProvider rnd = new SequenceRandomProvider(
                0.90, // N0 -> N2
                0.10, // N2 -> N5
                0.90, // N5 -> N3
                0.01  // N3 -> N12 (провайдер делает выбор детерминированным)
        );
        CoreProbabilisticAutomaton a = buildAutomaton(provider, rnd);

        a.step("tick", new SimpleInputMessage("start", java.util.Map.of()));
        Assertions.assertEquals("N2", a.getCurrentStateName());

        a.step("tick", new SimpleInputMessage("next", java.util.Map.of()));
        Assertions.assertEquals("N5", a.getCurrentStateName());

        a.step("tick", new SimpleInputMessage("next", java.util.Map.of()));
        Assertions.assertEquals("N3", a.getCurrentStateName());

        a.step("tick", new SimpleInputMessage("route", java.util.Map.of()));
        Assertions.assertEquals("N12", a.getCurrentStateName());
        Assertions.assertTrue(a.isInFinalState());
    }
}
