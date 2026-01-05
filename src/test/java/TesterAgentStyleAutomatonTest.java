import automaton.builder.AlgorithmBuilder;
import automaton.commands.ClearNextStatesCommand;
import automaton.commands.Command;
import automaton.commands.ProbabilisticTransitionCommand;
import automaton.context.Context;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.InputMessage;
import automaton.input.SimpleInputMessage;
import automaton.output.OutputMessage;
import automaton.output.SimpleOutputMessage;
import automaton.random.RandomProvider;
import automaton.state.State;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Тесты для сети состояний в стиле тестового агента:
 * SCAN_UI -> CHOOSE_TARGET -> CLICK -> VERIFY -> (SCAN_UI | REPORT)
 *            \-> WAIT -> SCAN_UI
 */
public class TesterAgentStyleAutomatonTest {

    // Детерминированный источник случайности для стабильных тестов
    static class SequenceRandomProvider implements RandomProvider {
        private final double[] seq;
        private int i = 0;
        SequenceRandomProvider(double... seq) { this.seq = seq; }
        @Override public double nextUnit() {
            if (seq.length == 0) return 0.0;
            double v = seq[i % seq.length];
            i++;
            if (v < 0.0) return 0.0;
            if (v > 1.0) return 1.0;
            return v;
        }
    }

    // Команды (внутри теста для демонстрации идеи "подключи свою логику")
    static class EmitCommand implements Command {
        private final String type;
        private final Map<String, Object> extra;
        EmitCommand(String type, Map<String, Object> extra) { this.type = type; this.extra = extra; }
        @Override public void execute(Context context, State currentState) {
            Map<String, Object> attrs = new HashMap<>(extra);
            attrs.put("state", currentState.getName());
            if (context.getInputMessage() != null) attrs.put("input", context.getInputMessage().getRaw());
            context.emit(new SimpleOutputMessage(type, attrs));
        }
        @Override public String getName() { return "emit_" + type; }
    }

    static class ScanUiCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            InputMessage msg = context.getInputMessage();
            Map<String, Object> attrs = msg != null ? msg.getAttributes() : Collections.emptyMap();
            context.put("targets", attrs.getOrDefault("targets", Collections.emptyList()));
            context.put("bugFound", Boolean.TRUE.equals(attrs.get("bugFound")));
        }
        @Override public String getName() { return "scan_ui"; }
    }

    static class BuildScanTransitionsCommand implements Command {
        private final State choose;
        private final State wait;
        private final State report;
        BuildScanTransitionsCommand(State choose, State wait, State report) {
            this.choose = choose;
            this.wait = wait;
            this.report = report;
        }
        @Override public void execute(Context context, State currentState) {
            boolean bugFound = Boolean.TRUE.equals(context.get("bugFound"));
            Object t = context.get("targets");
            int targetsSize = (t instanceof List<?> list) ? list.size() : 0;

            if (bugFound) {
                currentState.addNextState(report, 1.0);
                return;
            }
            if (targetsSize > 0) {
                currentState.addNextState(choose, 0.85);
                currentState.addNextState(wait, 0.15);
                return;
            }
            currentState.addNextState(wait, 1.0);
        }
        @Override public String getName() { return "build_scan_transitions"; }
    }

    static class ChooseTargetCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            Object t = context.get("targets");
            if (t instanceof List<?> list && !list.isEmpty()) {
                context.put("selectedTarget", list.get(0));
            } else {
                context.put("selectedTarget", null);
            }
        }
        @Override public String getName() { return "choose_target"; }
    }

    static class ClickCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            Object target = context.get("selectedTarget");
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("state", currentState.getName());
            attrs.put("target", target);
            context.emit(new SimpleOutputMessage("UI_CLICK", attrs));
        }
        @Override public String getName() { return "click"; }
    }

    static class BuildVerifyTransitionsCommand implements Command {
        private final State scan;
        private final State report;
        BuildVerifyTransitionsCommand(State scan, State report) { this.scan = scan; this.report = report; }
        @Override public void execute(Context context, State currentState) {
            InputMessage msg = context.getInputMessage();
            boolean ok = msg != null && Boolean.TRUE.equals(msg.getAttributes().get("clickOk"));
            if (!ok) {
                currentState.addNextState(report, 1.0);
                return;
            }
            currentState.addNextState(scan, 0.9);
            currentState.addNextState(report, 0.1);
        }
        @Override public String getName() { return "build_verify_transitions"; }
    }

    static class WaitCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            context.emit(new SimpleOutputMessage("SLEEP", Map.of("ms", 250, "state", currentState.getName())));
        }
        @Override public String getName() { return "wait"; }
    }

    static class ReportCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            context.emit(new SimpleOutputMessage("REPORT", Map.of(
                    "state", currentState.getName(),
                    "bugFound", context.get("bugFound"),
                    "selectedTarget", context.get("selectedTarget")
            )));
        }
        @Override public String getName() { return "report"; }
    }

    private static Map<String, Object> target(int x, int y) {
        Map<String, Object> t = new HashMap<>();
        t.put("x", x);
        t.put("y", y);
        return t;
    }

    private static InputMessage scanMsg(List<Map<String, Object>> targets, boolean bugFound) {
        return new SimpleInputMessage("scan", Map.of("targets", targets, "bugFound", bugFound));
    }

    private static InputMessage verifyMsg(boolean clickOk) {
        return new SimpleInputMessage("verify", Map.of("clickOk", clickOk));
    }

    private static CoreProbabilisticAutomaton build(RandomProvider provider) {
        State SCAN_UI = new State("SCAN_UI", false);
        State CHOOSE_TARGET = new State("CHOOSE_TARGET", false);
        State CLICK = new State("CLICK", false);
        State VERIFY = new State("VERIFY", false);
        State WAIT = new State("WAIT", false);
        State REPORT = new State("REPORT", true);

        // Алгоритм SCAN_UI
        AlgorithmBuilder scan = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "scan_enter")))
                .addCommand(new ScanUiCommand())
                .addCommand(new ClearNextStatesCommand())
                .addCommand(new BuildScanTransitionsCommand(CHOOSE_TARGET, WAIT, REPORT))
                .addCommand(new ProbabilisticTransitionCommand(provider));
        SCAN_UI.addAlgorithm("tick", scan.build());

        // Алгоритм CHOOSE_TARGET
        AlgorithmBuilder choose = new AlgorithmBuilder("tick")
                .addCommand(new ChooseTargetCommand())
                .clearNextStates()
                .addExistingState(CLICK, false, 1.0)
                .probabilisticTransition(provider);
        CHOOSE_TARGET.addAlgorithm("tick", choose.build());

        // CLICK
        AlgorithmBuilder click = new AlgorithmBuilder("tick")
                .addCommand(new ClickCommand())
                .clearNextStates()
                .addExistingState(VERIFY, false, 1.0)
                .probabilisticTransition(provider);
        CLICK.addAlgorithm("tick", click.build());

        // VERIFY
        AlgorithmBuilder verify = new AlgorithmBuilder("tick")
                .addCommand(new ClearNextStatesCommand())
                .addCommand(new BuildVerifyTransitionsCommand(SCAN_UI, REPORT))
                .addCommand(new ProbabilisticTransitionCommand(provider));
        VERIFY.addAlgorithm("tick", verify.build());

        // Алгоритм WAIT
        AlgorithmBuilder wait = new AlgorithmBuilder("tick")
                .addCommand(new WaitCommand())
                .clearNextStates()
                .addExistingState(SCAN_UI, false, 1.0)
                .probabilisticTransition(provider);
        WAIT.addAlgorithm("tick", wait.build());

        // REPORT
        AlgorithmBuilder report = new AlgorithmBuilder("tick")
                .addCommand(new ReportCommand());
        REPORT.addAlgorithm("tick", report.build());

        return new CoreProbabilisticAutomaton(SCAN_UI);
    }

    @Test
    public void happyPath_reachesReport_andEmitsClickAndReport() {
        // Последовательность переходов
        RandomProvider provider = new SequenceRandomProvider(0.10, 0.05);
        CoreProbabilisticAutomaton a = build(provider);

        Assertions.assertEquals("SCAN_UI", a.getCurrentStateName());

        // SCAN_UI -> CHOOSE_TARGET
        a.step("tick", scanMsg(List.of(target(10, 20)), false));
        Assertions.assertEquals("CHOOSE_TARGET", a.getCurrentStateName());
        a.drainOutbox();

        // CHOOSE_TARGET -> CLICK
        a.step("tick", SimpleInputMessage.of("choose"));
        Assertions.assertEquals("CLICK", a.getCurrentStateName());
        a.drainOutbox();

        // CLICK -> VERIFY и отправка UI_CLICK
        a.step("tick", SimpleInputMessage.of("click"));
        Assertions.assertEquals("VERIFY", a.getCurrentStateName());
        List<OutputMessage> outClick = a.drainOutbox();
        Assertions.assertEquals(1, outClick.size());
        Assertions.assertEquals("UI_CLICK", outClick.get(0).getType());
        Assertions.assertTrue(outClick.get(0).getAttributes().containsKey("target"));

        // VERIFY (ok) -> SCAN_UI
        a.step("tick", verifyMsg(true));
        Assertions.assertEquals("SCAN_UI", a.getCurrentStateName());
        a.drainOutbox();

        // SCAN_UI (bugFound) -> REPORT
        a.step("tick", scanMsg(Collections.emptyList(), true));
        Assertions.assertEquals("REPORT", a.getCurrentStateName());
        a.drainOutbox();

        // REPORT отправляет отчёт
        a.step("tick", SimpleInputMessage.of("report"));
        List<OutputMessage> outReport = a.drainOutbox();
        Assertions.assertEquals(1, outReport.size());
        Assertions.assertEquals("REPORT", outReport.get(0).getType());
        Assertions.assertTrue(a.isInFinalState());
    }

    @Test
    public void scanWithNoTargets_goesToWait_thenBackToScan_andEmitsSleep() {
        RandomProvider provider = new SequenceRandomProvider(0.5);
        CoreProbabilisticAutomaton a = build(provider);

        a.step("tick", scanMsg(Collections.emptyList(), false));
        Assertions.assertEquals("WAIT", a.getCurrentStateName());
        a.drainOutbox();

        a.step("tick", SimpleInputMessage.of("wait"));
        Assertions.assertEquals("SCAN_UI", a.getCurrentStateName());
        List<OutputMessage> out = a.drainOutbox();
        Assertions.assertEquals(1, out.size());
        Assertions.assertEquals("SLEEP", out.get(0).getType());
    }
}
