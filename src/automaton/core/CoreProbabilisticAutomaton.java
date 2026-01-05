package automaton.core;

import automaton.context.Context;
import automaton.input.InputMessage;
import automaton.output.OutputMessage;
import automaton.state.State;
import java.util.*;

public class CoreProbabilisticAutomaton {
    private final Context context;
    private final Random random = new Random();
    private final State initialState;

    public CoreProbabilisticAutomaton(State initialState) {
        this.initialState = initialState;
        this.context = new Context(initialState);
    }

    public void processInput(String input) {
        System.out.println("Processing input: '" + input + "' in state " + getCurrentStateName());

        State currentState = context.getCurrentState();
        Set<String> algorithms = currentState.getAlgorithmNames();

        if (algorithms.isEmpty()) {
            System.out.println("No algorithms for input '" + input + "'");
            return;
        }

        String selectedAlgorithm = algorithms.contains(input) ? input :
                new ArrayList<>(algorithms).get(random.nextInt(algorithms.size()));

        currentState.executeAlgorithm(context, selectedAlgorithm);
    }

    public void executeCurrentStateAlgorithm(String algorithmName) {
        State currentState = context.getCurrentState();
        currentState.executeAlgorithm(context, algorithmName);
    }

    public void addTransition(State from, State to, double probability) {
        from.addNextState(to, probability);
        System.out.println("Added transition: " + from.getName() + " -> " + to.getName() + " (probability: " + probability + ")");
    }

    public void addTransition(State from, State to) {
        addTransition(from, to, 1.0);
    }

    public Map<State, Double> getTransitionProbabilities(State state) {
        return state.getTransitionProbabilities();
    }

    public void reset() {
        context.reset(initialState);
        System.out.println("Automaton reset to initial state: " + initialState.getName());
    }

    public void resetTo(State state) {
        context.setState(state);
        System.out.println("Automaton reset to state: " + state.getName());
    }

    public String getCurrentStateName() {
        return context.getCurrentState().getName();
    }

    public State getCurrentState() {
        return context.getCurrentState();
    }

    public boolean isInFinalState() {
        return context.getCurrentState().isFinal();
    }

    public List<String> getCurrentStateAlgorithms() {
        return new ArrayList<>(context.getCurrentState().getAlgorithmNames());
    }


    /**
     * Высокоуровневый API: обрабатывает одно входное сообщение,
     * выполняя указанный алгоритм с начального состояния.
     *
     * Автомат сбрасывается перед обработкой, поэтому каждый вызов run()
     * независим. История посещённых состояний сохраняется в AutomatonResult.
     */
    public AutomatonResult run(String algorithmName, InputMessage inputMessage) {
        // Сбрасываем контекст и историю
        context.reset(initialState);
        if (inputMessage != null) {
            context.setInputMessage(inputMessage);
        }
        // Выполняем выбранный алгоритм с начального состояния
        State current = context.getCurrentState();
        current.executeAlgorithm(context, algorithmName);

        State finalState = context.getCurrentState();
        return new AutomatonResult(finalState,
                context.getStateHistory(),
                finalState.isFinal(),
                inputMessage);
    }

    /**
     * API с сохранением состояния: обрабатывает один шаг без сброса автомата.
     *
     * Типичный цикл агента:
     *  - automaton.step("handle", message);
     *  - проверка текущего состояния и/или истории;
     *  - повторять, пока не достигнуто конечное состояние.
     */
    public AutomatonResult step(String algorithmName, InputMessage inputMessage) {
        if (inputMessage != null) {
            context.setInputMessage(inputMessage);
        }
        State current = context.getCurrentState();
        current.executeAlgorithm(context, algorithmName);

        State newCurrent = context.getCurrentState();
        return new AutomatonResult(newCurrent,
                context.getStateHistory(),
                newCurrent.isFinal(),
                inputMessage);
    }

    /** Очищает и возвращает все выходные сообщения, отправленные за последний шаг/запуск. */
    public List<OutputMessage> drainOutbox() {
        return context.drainOutbox();
    }

    /** Сохраняет значение в рабочей памяти автомата. */
    public void put(String key, Object value) {
        context.put(key, value);
    }

    /** Читает значение из рабочей памяти автомата. */
    public Object get(String key) {
        return context.get(key);
    }

    /** Только для чтения: текущая рабочая память. */
    public Map<String, Object> memoryView() {
        return context.memoryView();
    }

public void printStatus() {
        State current = context.getCurrentState();
        System.out.println("Automaton status:");
        System.out.println("  Current state: " + current.getName());
        System.out.println("  Final state: " + (current.isFinal() ? "Yes" : "No"));
        System.out.println("  Available algorithms: " + current.getAlgorithmNames());
        System.out.println("  Next states count: " + current.getNextStates().size());

        Map<State, Double> probabilities = current.getTransitionProbabilities();
        if (!probabilities.isEmpty()) {
            System.out.println("  Transition probabilities: " + probabilities);
        }
    }
}