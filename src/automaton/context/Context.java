package automaton.context;

import automaton.input.InputMessage;
import automaton.output.OutputMessage;
import automaton.state.State;
import java.util.*;

/**
 * Контекст выполнения вероятностного автомата.
 * Хранит текущее состояние, все известные состояния, полную историю посещённых состояний
 * и последнее входное сообщение, связанное с текущей обработкой.
 */
public class Context {
    /** Текущее состояние автомата. */
    private State currentState;

    /** Все уникальные состояния, которые когда-либо встречались в этом контексте. */
    private final List<State> states = new ArrayList<>();

    /** Полная хронологическая история посещённых состояний (включая повторения). */
    private final List<State> stateHistory = new ArrayList<>();

    /** Последнее входное сообщение, переданное автомату (может быть null). */
    private InputMessage inputMessage;


    /** Общая память ключ-значение для команд/алгоритмов состояний (рабочая память агента). */
    private final Map<String, Object> memory = new HashMap<>();

    /** Исходящие сообщения, созданные во время шага/запуска. Внешний агент может их получить и обработать. */
    private final List<OutputMessage> outbox = new ArrayList<>();
    public Context(State initialState) {
        reset(initialState);
    }

    /**
     * Сбрасывает контекст к заданному начальному состоянию:
     *  - текущее состояние устанавливается в initialState;
     *  - список известных состояний переинициализируется;
     *  - история очищается и начинается с initialState;
     *  - последнее входное сообщение очищается.
     */
    public void reset(State initialState) {
        this.currentState = initialState;
        states.clear();
        states.add(initialState);
        stateHistory.clear();
        stateHistory.add(initialState);
        inputMessage = null;
        memory.clear();
        outbox.clear();
    }

    public void setState(State state) {
        if (!state.equals(currentState)) {
            System.out.println("Context: transition from " + currentState.getName() + " to " + state.getName());
            this.currentState = state;
            if (!states.contains(state)) {
                states.add(state);
            }
            stateHistory.add(state);
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    /** Возвращает копию всех уникальных состояний, известных этому контексту. */
    public List<State> getAllStates() {
        return new ArrayList<>(states);
    }

    /** Возвращает копию хронологической истории (может быть пустой, но не null). */
    public List<State> getStateHistory() {
        return new ArrayList<>(stateHistory);
    }

    /** Выполняет алгоритм текущего состояния. */
    public void executeCurrentStateAlgorithm(String algorithmName) {
        currentState.executeAlgorithm(this, algorithmName);
    }

    public boolean containsState(State state) {
        return states.contains(state);
    }

    /** Устанавливает последнее входное сообщение для этого контекста. */
    public void setInputMessage(InputMessage message) {
        this.inputMessage = message;
    }

    /** Возвращает последнее входное сообщение (может быть null). */
    public InputMessage getInputMessage() {
        return inputMessage;
    }

    /** Сохраняет значение в общей памяти (рабочая память). */
    public void put(String key, Object value) {
        memory.put(key, value);
    }

    /** Получает значение из общей памяти (может быть null). */
    public Object get(String key) {
        return memory.get(key);
    }

    /** Только для чтения: представление памяти (изменяемые объекты внутри не копируются). */
    public Map<String, Object> memoryView() {
        return Collections.unmodifiableMap(memory);
    }

    /** Отправляет выходное сообщение из команды/алгоритма. */
    public void emit(OutputMessage message) {
        if (message != null) outbox.add(message);
    }

    /** Возвращает и очищает все отправленные сообщения с последнего вызова drain. */
    public List<OutputMessage> drainOutbox() {
        List<OutputMessage> copy = new ArrayList<>(outbox);
        outbox.clear();
        return copy;
    }

}
