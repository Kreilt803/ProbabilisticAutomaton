package automaton.commands;

import automaton.context.Context;
import automaton.input.InputMessage;
import automaton.probability.HistoryProbabilityProvider;
import automaton.random.JavaRandomProvider;
import automaton.random.RandomProvider;
import automaton.state.State;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда, выполняющая вероятностный переход на основе
 * всей истории посещённых состояний и текущего входного сообщения.
 *
 * Модель вероятностей предоставляется HistoryProbabilityProvider,
 * что делает команду гибкой и позволяет использовать любую пользовательскую логику.
 */
public class HistoryBasedTransitionCommand implements Command {

    private final HistoryProbabilityProvider probabilityProvider;
    private final RandomProvider randomProvider;

    /** Создаёт команду с равномерным RandomProvider по умолчанию. */
    public HistoryBasedTransitionCommand(HistoryProbabilityProvider probabilityProvider) {
        this(probabilityProvider, new JavaRandomProvider());
    }

    /** Создаёт команду с явно заданным RandomProvider (для детерминированных тестов или кастомного ГСЧ). */
    public HistoryBasedTransitionCommand(HistoryProbabilityProvider probabilityProvider,
                                         RandomProvider randomProvider) {
        this.probabilityProvider = probabilityProvider;
        this.randomProvider = randomProvider != null ? randomProvider : new JavaRandomProvider();
    }

    @Override
    public void execute(Context context, State currentState) {
        List<State> nextStates = currentState.getNextStates();

        if (nextStates.isEmpty()) {
            System.out.println("  -> History-based transition: no next states, staying in " + currentState.getName());
            return;
        }

        List<State> history = context.getStateHistory();
        InputMessage input = context.getInputMessage();

        double[] raw = probabilityProvider.computeProbabilities(history, input, nextStates);
        if (raw == null || raw.length != nextStates.size()) {
            throw new IllegalStateException(
                    "HistoryProbabilityProvider returned invalid probabilities: expected length " +
                            nextStates.size() + " but got " + (raw == null ? "null" : raw.length)
            );
        }

        // Нормализуем вероятности
        double sum = 0.0;
        for (double v : raw) {
            if (v > 0.0) {
                sum += v;
            }
        }
        if (sum <= 0.0) {
            // Если все вероятности нулевые или отрицательные, используем равномерное распределение
            sum = nextStates.size();
            raw = new double[nextStates.size()];
            for (int i = 0; i < raw.length; i++) {
                raw[i] = 1.0;
            }
        }

        double[] probs = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            probs[i] = Math.max(0.0, raw[i]) / sum;
        }

        // Выбираем следующее состояние
        double r = randomProvider.nextUnit();
        double cumulative = 0.0;
        State chosen = nextStates.get(nextStates.size() - 1); // Запасной вариант
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (r <= cumulative) {
                chosen = nextStates.get(i);
                break;
            }
        }

        context.setState(chosen);

        // Подготавливаем отладочную информацию
        List<String> probInfo = new ArrayList<>();
        for (int i = 0; i < nextStates.size(); i++) {
            probInfo.add(nextStates.get(i).getName() + "=" + probs[i]);
        }

        System.out.println("  -> History-based probabilistic transition to: " + chosen.getName());
        System.out.println("    Computed probabilities: " + probInfo);
    }

    @Override
    public String getName() {
        return "history_based_transition";
    }
}
