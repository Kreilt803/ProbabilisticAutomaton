package automaton.core;

import automaton.input.InputMessage;
import automaton.state.State;

import java.util.Collections;
import java.util.List;

/**
 * Неизменяемый результат одного запуска автомата для заданного входного сообщения.
 * Содержит конечное состояние, полный путь посещённых состояний и исходное входное сообщение.
 */
public class AutomatonResult {

    private final State finalState;
    private final List<State> visitedStates;
    private final boolean inFinalState;
    private final InputMessage inputMessage;

    public AutomatonResult(State finalState,
                           List<State> visitedStates,
                           boolean inFinalState,
                           InputMessage inputMessage) {
        this.finalState = finalState;
        this.visitedStates = visitedStates != null
                ? Collections.unmodifiableList(visitedStates)
                : Collections.emptyList();
        this.inFinalState = inFinalState;
        this.inputMessage = inputMessage;
    }

    /** Конечное состояние после выполнения выбранного алгоритма. */
    public State getFinalState() {
        return finalState;
    }

    /** Полная хронологическая последовательность состояний, посещённых во время обработки. */
    public List<State> getVisitedStates() {
        return visitedStates;
    }

    /** Является ли конечное состояние принимающим/финальным в модели автомата. */
    public boolean isInFinalState() {
        return inFinalState;
    }

    /** Исходное входное сообщение, которое было обработано. */
    public InputMessage getInputMessage() {
        return inputMessage;
    }

    @Override
    public String toString() {
        return "AutomatonResult{" +
                "finalState=" + finalState +
                ", inFinalState=" + inFinalState +
                ", visitedStates=" + visitedStates +
                '}';
    }
}
