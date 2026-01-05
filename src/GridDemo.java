
import automaton.builder.AlgorithmBuilder;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.SimpleInputMessage;
import automaton.output.OutputMessage;
import automaton.output.SimpleOutputMessage;
import automaton.random.RandomProvider;
import automaton.state.State;
import automaton.commands.Command;
import automaton.context.Context;

import java.util.*;

/**
 * Демонстрация: сетка 3x3 состояний с вероятностными переходами.
 */
public class GridDemo {

    static class SequenceRandomProvider implements RandomProvider {
        private final double[] seq; private int i=0;
        SequenceRandomProvider(double... seq) { this.seq = seq; }
        public double nextUnit() {
            double v = seq[i % seq.length]; i++;
            if (v < 0) return 0; if (v > 1) return 1;
            return v;
        }
    }

    private static class Emit implements Command {
        private final String type;
        Emit(String type) { this.type = type; }
        public void execute(Context context, State currentState) {
            Map<String,Object> a = new HashMap<>();
            a.put("state", currentState.getName());
            context.emit(new SimpleOutputMessage(type, a));
        }
        public String getName() { return "emit_" + type; }
    }

    public static CoreProbabilisticAutomaton build(RandomProvider provider) {
        Map<String, State> s = new HashMap<>();
        for (int r=0;r<3;r++) for (int c=0;c<3;c++) {
            boolean fin = (r==2 && c==2);
            s.put("r"+r+"c"+c, new State("r"+r+"c"+c, fin));
        }

        for (int r=0;r<3;r++) for (int c=0;c<3;c++) {
            State cur = s.get("r"+r+"c"+c);
            if (cur.isFinal()) continue;

            AlgorithmBuilder b = new AlgorithmBuilder("tick")
                    .clearNextStates()
                    .addCommand(new Emit("entered"));

            if (c+1<3) b.addExistingState(s.get("r"+r+"c"+(c+1)), false, 0.6);
            if (r+1<3) b.addExistingState(s.get("r"+(r+1)+"c"+c), false, 0.4);

            b.probabilisticTransition(provider);
            cur.addAlgorithm("tick", b.build());
        }

        return new CoreProbabilisticAutomaton(s.get("r0c0"));
    }

    public static void main(String[] args) {
        CoreProbabilisticAutomaton a = build(new SequenceRandomProvider(0.1, 0.9, 0.1, 0.5));
        System.out.println("Start: " + a.getCurrentStateName());

        for (int i=1; i<=4; i++) {
            a.step("tick", SimpleInputMessage.of("event_"+i));
            System.out.println("Step " + i + " -> " + a.getCurrentStateName() + (a.getCurrentState().isFinal() ? " (FINAL)" : ""));
            for (OutputMessage m : a.drainOutbox()) System.out.println("  outbox: " + m);
        }
    }
}
