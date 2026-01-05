package automaton.probability;

import automaton.input.InputMessage;
import automaton.state.State;

import java.util.List;

/**
 * Интерфейс стратегии для вычисления вероятностей переходов на основе
 * всей истории посещённых состояний и текущего входного сообщения.
 *
 * Реализации могут использовать последние N состояний, всю историю,
 * атрибуты сообщения, внешние модели и т.п.
 */
public interface HistoryProbabilityProvider {

    /**
     * Вычисляет вероятности переходов из текущего состояния в каждое из
     * кандидатских следующих состояний.
     *
     * @param history    хронологический список посещённых состояний (включая текущее в конце)
     * @param input      входное сообщение, обрабатываемое сейчас (может быть null)
     * @param nextStates список кандидатских следующих состояний для выбора
     * @return массив вероятностей той же длины, что и nextStates.
     *         Значения не обязательно нормализованы — они будут нормализованы внутри.
     */
    double[] computeProbabilities(List<State> history,
                                  InputMessage input,
                                  List<State> nextStates);
}
