package automaton.builder;

import automaton.commands.*;
import automaton.probability.HistoryProbabilityProvider;
import automaton.random.RandomProvider;
import java.util.*;

public class AlgorithmBuilder {
    private final List<Command> commands = new ArrayList<>();
    private final String algorithmName;

    public AlgorithmBuilder(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public AlgorithmBuilder addCommand(Command command) {
        commands.add(command);
        return this;
    }

    public AlgorithmBuilder clearNextStates() {
        return addCommand(new ClearNextStatesCommand());
    }

    public AlgorithmBuilder addNewState(String stateName, boolean isFinal, double probability) {
        return addCommand(new AddStateWithProbabilityCommand(stateName, isFinal, probability));
    }

    public AlgorithmBuilder addExistingState(automaton.state.State state, boolean isFinal, double probability) {
        // Переход к уже существующему экземпляру состояния
        return addCommand(new AddExistingStateWithProbabilityCommand(state, probability));
    }

    public AlgorithmBuilder transitionTo(int index) {
        return addCommand(new TransitionCommand(index));
    }

    public AlgorithmBuilder transitionToFirst() {
        return addCommand(new TransitionCommand(0));
    }

    public AlgorithmBuilder transitionToLast() {
        // -1 используется как маркер для "последнего" (разрешается во время выполнения)
        return addCommand(new TransitionCommand(-1));
    }

    /** Вероятностный переход с источником случайности по умолчанию. */
    public AlgorithmBuilder probabilisticTransition() {
        return addCommand(new ProbabilisticTransitionCommand());
    }

    /** Вероятностный переход с заданным источником случайности. */
    public AlgorithmBuilder probabilisticTransition(RandomProvider provider) {
        return addCommand(new ProbabilisticTransitionCommand(provider));
    }

    /**
     * Вероятностный переход на основе истории.
     * Распределение вероятностей вычисляется HistoryProbabilityProvider,
     * который может анализировать последние N состояний, всю историю и входное сообщение.
     */
    public AlgorithmBuilder historyBasedTransition(HistoryProbabilityProvider provider) {
        return addCommand(new HistoryBasedTransitionCommand(provider));
    }

    /**
     * Вероятностный переход на основе истории с явно заданным RandomProvider
     * (для детерминированных тестов или кастомных источников случайности).
     */
    public AlgorithmBuilder historyBasedTransition(HistoryProbabilityProvider provider,
                                                   RandomProvider randomProvider) {
        return addCommand(new HistoryBasedTransitionCommand(provider, randomProvider));
    }


    public List<Command> build() {
        System.out.println("Built algorithm '" + algorithmName + "' with " + commands.size() + " commands");
        return new ArrayList<>(commands);
    }

    public String getAlgorithmName() {
        return algorithmName;
    }
}
