import automaton.builder.AlgorithmBuilder;
import automaton.core.AutomatonResult;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.InputMessage;
import automaton.input.SimpleInputMessage;
import automaton.probability.HistoryProbabilityProvider;
import automaton.state.State;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Демонстрация работы с историей состояний и вероятностными переходами
 * на основе истории.
 */
public class HistoryDemo {

    /**
     * Пример провайдера вероятностей на основе истории:
     * вероятности зависят от текста сообщения и последних двух состояний.
     */
    public static class KeywordHistoryProbabilityProvider implements HistoryProbabilityProvider {

        @Override
        public double[] computeProbabilities(List<State> history,
                                             InputMessage input,
                                             List<State> nextStates) {

            double[] weights = new double[nextStates.size()];
            if (nextStates.isEmpty()) {
                return weights;
            }

            String text = input != null && input.getRaw() != null
                    ? input.getRaw().toLowerCase()
                    : "";

            boolean looksSuccessful =
                    text.contains("ok") ||
                    text.contains("good") ||
                    text.contains("success") ||
                    text.contains("успех") ||
                    text.contains("норм");

            int historyBonus = 1;
            if (history != null && history.size() >= 2) {
                State last = history.get(history.size() - 1);
                State prev = history.get(history.size() - 2);
                if (last.getName().equals(prev.getName())) {
                    historyBonus = 2;
                }
            }

            if (nextStates.size() == 2) {
                // nextStates[0] — успех, [1] — неудача
                if (looksSuccessful) {
                    weights[0] = 0.7 * historyBonus;
                    weights[1] = 0.3;
                } else {
                    weights[0] = 0.3;
                    weights[1] = 0.7 * historyBonus;
                }
            } else {
                for (int i = 0; i < nextStates.size(); i++) {
                    weights[i] = 1.0;
                }
            }

            return weights;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Failed to set UTF-8 encoding: " + e.getMessage());
        }

        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ HISTORY DEMO: automaton with state history             │");
        System.out.println("│ and custom HistoryProbabilityProvider                  │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        // Создаём автомат с помощью AlgorithmBuilder
        State start = new State("START", false);

        AlgorithmBuilder builder = new AlgorithmBuilder("check");
        builder
                .clearNextStates()
                .addNewState("SUCCESS", true, 1.0)
                .addNewState("FAILURE", true, 1.0)
                .historyBasedTransition(new KeywordHistoryProbabilityProvider());

        start.addAlgorithm(builder.getAlgorithmName(), builder.build());

        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        String[] messages = {
                "operation ok, everything is good",
                "critical error happened",
                "success but was error before",
                "еще одна ошибка",
                "всё норм, успех"
        };

        for (int i = 0; i < messages.length; i++) {
            String text = messages[i];
            System.out.println();
            System.out.println("==== RUN #" + (i + 1) + " ====");
            System.out.println("Input message: \"" + text + "\"");

            InputMessage msg = SimpleInputMessage.of(text);
            AutomatonResult result = automaton.run("check", msg);

            System.out.println("Final state: " + result.getFinalState().getName());
            System.out.println("Is final: " + result.isInFinalState());
            System.out.println("Visited states:");
            for (State s : result.getVisitedStates()) {
                System.out.println("  - " + s.getName());
            }
        }
    }
}
