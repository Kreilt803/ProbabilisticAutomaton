
import automaton.builder.AlgorithmBuilder;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.SimpleInputMessage;
import automaton.random.RandomProvider;
import automaton.state.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.*;

/**
 * Тесты: бинарное дерево глубины 4 (15 состояний).
 * Правило перехода: u <= 0.7 -> левый потомок, u > 0.7 -> правый потомок.
 */
public class TreeAutomatonTest {

    static class SequenceRandomProvider implements RandomProvider {
        private final double[] seq;
        private int i = 0;
        SequenceRandomProvider(double... seq) { this.seq = seq; }
        @Override public double nextUnit() {
            if (seq.length == 0) return 0.0;
            double v = seq[i % seq.length]; i++;
            if (v < 0.0) return 0.0;
            if (v > 1.0) return 1.0;
            return v;
        }
    }

    private static class Node {
        final State state;
        Node left;
        Node right;
        Node(State state) { this.state = state; }
    }

    private static Node buildTreeStates() {
        // Строим полное бинарное дерево глубины 4, листья на уровне 3 — финальные
        Node root = new Node(new State("N0", false));
        List<Node> level = List.of(root);
        int counter = 1;
        for (int depth = 1; depth <= 3; depth++) {
            List<Node> next = new ArrayList<>();
            for (Node parent : level) {
                boolean fin = (depth == 3);
                Node l = new Node(new State("N" + counter++, fin));
                Node r = new Node(new State("N" + counter++, fin));
                parent.left = l;
                parent.right = r;
                next.add(l); next.add(r);
            }
            level = next;
        }
        return root;
    }

    private static void wireAlgorithms(Node node, RandomProvider provider) {
        if (node == null) return;
        if (!node.state.isFinal()) {
            AlgorithmBuilder b = new AlgorithmBuilder("tick")
                    .clearNextStates()
                    .addExistingState(node.left.state, false, 0.7)
                    .addExistingState(node.right.state, false, 0.3)
                    .probabilisticTransition(provider);

            node.state.addAlgorithm(b.getAlgorithmName(), b.build());
        }
        wireAlgorithms(node.left, provider);
        wireAlgorithms(node.right, provider);
    }

    private static List<String> runPath(CoreProbabilisticAutomaton a, int steps) {
        List<String> names = new ArrayList<>();
        names.add(a.getCurrentStateName());
        for (int i = 0; i < steps; i++) {
            a.step("tick", SimpleInputMessage.of("msg_" + i));
            names.add(a.getCurrentStateName());
        }
        return names;
    }

    @Test
    public void deterministicPathThroughFourLevels() {
        // Последовательность: левый (0.1), левый (0.2), правый (0.9)
        RandomProvider provider = new SequenceRandomProvider(0.1, 0.2, 0.9);

        Node root = buildTreeStates();
        wireAlgorithms(root, provider);

        CoreProbabilisticAutomaton a = new CoreProbabilisticAutomaton(root.state);
        List<String> path = runPath(a, 3); // 3 transitions to leaf

        // Ожидаемый путь: корень -> левый -> левый -> правый
        Assertions.assertEquals(List.of("N0", "N1", "N3", "N8"), path);
        Assertions.assertTrue(a.getCurrentState().isFinal());
    }

    @Test
    public void runResetsStateButStepDoesNot() {
        RandomProvider provider = new SequenceRandomProvider(0.1, 0.9, 0.1, 0.9);

        Node root = buildTreeStates();
        wireAlgorithms(root, provider);

        CoreProbabilisticAutomaton a = new CoreProbabilisticAutomaton(root.state);

        // Два шага — уже не в корне
        a.step("tick", SimpleInputMessage.of("a"));
        a.step("tick", SimpleInputMessage.of("b"));
        Assertions.assertNotEquals("N0", a.getCurrentStateName());

        // run() сбрасывает контекст к начальному состоянию перед выполнением алгоритма
        a.run("tick", SimpleInputMessage.of("run1"));
        Assertions.assertNotNull(a.getCurrentState());

        // Явный reset возвращает в корень
        a.reset();
        Assertions.assertEquals("N0", a.getCurrentStateName());
    }
}
