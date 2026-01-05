
import automaton.builder.AlgorithmBuilder;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.SimpleInputMessage;
import automaton.output.OutputMessage;
import automaton.output.SimpleOutputMessage;
import automaton.random.RandomProvider;
import automaton.state.State;
import automaton.commands.Command;
import automaton.context.Context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.*;

/**
 * Тесты: сетка 3x3 (9 состояний) с вероятностными переходами.
 * Глубина от (0,0) до (2,2) составляет 4 перехода.
 */
public class GridAutomatonTest {

    /** Детерминированный источник случайности для тестов: возвращает числа из фиксированной последовательности. */
    static class SequenceRandomProvider implements RandomProvider {
        private final double[] seq;
        private int i = 0;
        SequenceRandomProvider(double... seq) {
            this.seq = seq;
        }
        @Override
        public double nextUnit() {
            if (seq.length == 0) return 0.0;
            double v = seq[i % seq.length];
            i++;
            // Ограничиваем диапазон [0,1]
            if (v < 0.0) return 0.0;
            if (v > 1.0) return 1.0;
            return v;
        }
    }

    private static class EmitStateCommand implements Command {
        private final String type;
        EmitStateCommand(String type) { this.type = type; }

        @Override
        public void execute(Context context, State currentState) {
            Map<String,Object> attrs = new HashMap<>();
            attrs.put("state", currentState.getName());
            if (context.getInputMessage() != null) attrs.put("input", context.getInputMessage().getRaw());
            context.emit(new SimpleOutputMessage(type, attrs));
        }

        @Override
        public String getName() { return "emit_state_" + type; }
    }

    /** Строит автомат-сетку 3x3 с общим RandomProvider. */
    private static Map<String, Object> buildGridAutomaton(RandomProvider provider) {
        // Создаём все состояния один раз (важно: переиспользуем экземпляры)
        Map<String, State> s = new HashMap<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                boolean fin = (r == 2 && c == 2);
                String name = "r" + r + "c" + c;
                s.put(name, new State(name, fin));
            }
        }

        // Настраиваем алгоритмы
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                State cur = s.get("r" + r + "c" + c);
                if (cur.isFinal()) continue;

                AlgorithmBuilder b = new AlgorithmBuilder("tick")
                        .clearNextStates()
                        // Отправляем сообщение для тестирования outbox
                        .addCommand(new EmitStateCommand("entered"));

                // Переходы вправо и вниз
                if (c + 1 < 3) b.addExistingState(s.get("r" + r + "c" + (c + 1)), false, 0.6);
                if (r + 1 < 3) b.addExistingState(s.get("r" + (r + 1) + "c" + c), false, 0.4);

                // Если только одно следующее состояние, веса не важны — нормализация сделает вероятность 1
                b.probabilisticTransition(provider);

                cur.addAlgorithm(b.getAlgorithmName(), b.build());
            }
        }

        State start = s.get("r0c0");
        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        Map<String, Object> result = new HashMap<>();
        result.put("states", s);
        result.put("automaton", automaton);
        return result;
    }

    @Test
    public void reachesFinalStateWithDeterministicSequence() {
        // Последовательность: r0c0 -> r0c1 -> r1c1 -> r1c2 -> r2c2
        RandomProvider provider = new SequenceRandomProvider(0.1, 0.9, 0.1, 0.5);

        Map<String, Object> built = buildGridAutomaton(provider);
        CoreProbabilisticAutomaton a = (CoreProbabilisticAutomaton) built.get("automaton");
        @SuppressWarnings("unchecked")
        Map<String, State> states = (Map<String, State>) built.get("states");

        Assertions.assertEquals("r0c0", a.getCurrentStateName());

        a.step("tick", SimpleInputMessage.of("ui_event_1"));
        Assertions.assertSame(states.get("r0c1"), a.getCurrentState());

        a.step("tick", SimpleInputMessage.of("ui_event_2"));
        Assertions.assertSame(states.get("r1c1"), a.getCurrentState());

        a.step("tick", SimpleInputMessage.of("ui_event_3"));
        Assertions.assertSame(states.get("r1c2"), a.getCurrentState());

        a.step("tick", SimpleInputMessage.of("ui_event_4"));
        Assertions.assertSame(states.get("r2c2"), a.getCurrentState());
        Assertions.assertTrue(a.getCurrentState().isFinal());
    }

    @Test
    public void transitionProbabilitiesAreNormalizedToOne() {
        RandomProvider provider = new SequenceRandomProvider(0.2);

        Map<String, Object> built = buildGridAutomaton(provider);
        CoreProbabilisticAutomaton a = (CoreProbabilisticAutomaton) built.get("automaton");

        // Проверяем нормализацию вероятностей после выполнения алгоритма
        a.step("tick", SimpleInputMessage.of("x"));

        // Пересоздаём автомат для проверки нормализации вероятностей
        RandomProvider provider2 = new SequenceRandomProvider(0.2);
        Map<String, Object> built2 = buildGridAutomaton(provider2);
        @SuppressWarnings("unchecked")
        Map<String, State> states2 = (Map<String, State>) built2.get("states");
        State start = states2.get("r0c0");

        Map<State, Double> probs = start.getTransitionProbabilities();
        Assertions.assertTrue(probs.isEmpty());

        // Запускаем алгоритм один раз — вероятности всё ещё читаемы
        Context tmp = new Context(start);
        start.executeAlgorithm(tmp, "tick");

        Map<State, Double> probsAfter = start.getTransitionProbabilities();
        double sum = 0.0;
        for (double p : probsAfter.values()) sum += p;
        Assertions.assertEquals(1.0, sum, 1e-9);
    }

    @Test
    public void outboxCollectsMessagesFromCommands() {
        RandomProvider provider = new SequenceRandomProvider(0.1, 0.1, 0.1, 0.1);

        Map<String, Object> built = buildGridAutomaton(provider);
        CoreProbabilisticAutomaton a = (CoreProbabilisticAutomaton) built.get("automaton");

        a.step("tick", SimpleInputMessage.of("hello"));
        List<OutputMessage> out1 = a.drainOutbox();
        Assertions.assertEquals(1, out1.size());
        Assertions.assertEquals("entered", out1.get(0).getType());
        Assertions.assertTrue(out1.get(0).getAttributes().containsKey("state"));
        Assertions.assertEquals("hello", out1.get(0).getAttributes().get("input"));

        a.step("tick", SimpleInputMessage.of("world"));
        List<OutputMessage> out2 = a.drainOutbox();
        Assertions.assertEquals(1, out2.size());

        // drain очищает outbox
        Assertions.assertTrue(a.drainOutbox().isEmpty());
    }
}
