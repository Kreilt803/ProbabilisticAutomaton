import automaton.builder.AlgorithmBuilder;
import automaton.commands.Command;
import automaton.commands.ClearNextStatesCommand;
import automaton.commands.ProbabilisticTransitionCommand;
import automaton.context.Context;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.input.InputMessage;
import automaton.input.SimpleInputMessage;
import automaton.output.OutputMessage;
import automaton.output.SimpleOutputMessage;
import automaton.random.RandomProvider;
import automaton.state.State;

import java.util.*;

/**
 * Демонстрация в стиле тестового агента.
 *
 * Автомат не знает о UI/Akka и т.п., он только:
 *  - обрабатывает входные сообщения,
 *  - переходит между состояниями с вероятностями,
 *  - отправляет выходные сообщения в outbox.
 */
public class TesterAgentStyleDemo {

    // Детерминированный источник случайности для воспроизводимости демо
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

    // Команды демонстрации (здесь разработчик подключает свою логику)

    /** Отправляет сообщение в outbox с информацией о состоянии и дополнительными атрибутами. */
    static class EmitCommand implements Command {
        private final String type;
        private final Map<String, Object> extra;
        EmitCommand(String type) { this(type, Collections.emptyMap()); }
        EmitCommand(String type, Map<String, Object> extra) {
            this.type = type;
            this.extra = extra;
        }
        @Override public void execute(Context context, State currentState) {
            Map<String, Object> attrs = new HashMap<>(extra);
            attrs.put("state", currentState.getName());
            InputMessage msg = context.getInputMessage();
            if (msg != null) attrs.put("input", msg.getRaw());
            context.emit(new SimpleOutputMessage(type, attrs));
        }
        @Override public String getName() { return "emit_" + type; }
    }

    /** Читает атрибуты входного сообщения и сохраняет их в память для последующих состояний. */
    static class ScanUiCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            InputMessage msg = context.getInputMessage();
            Map<String, Object> attrs = msg != null ? msg.getAttributes() : Collections.emptyMap();

            Object targets = attrs.getOrDefault("targets", Collections.emptyList());
            boolean bugFound = Boolean.TRUE.equals(attrs.get("bugFound"));

            context.put("targets", targets);
            context.put("bugFound", bugFound);

            int scans = (int) context.memoryView().getOrDefault("scanCount", 0);
            context.put("scanCount", scans + 1);
        }
        @Override public String getName() { return "scan_ui"; }
    }

    /** На основе памяти (bugFound/targets) подготавливает следующие состояния для вероятностного перехода. */
    static class BuildScanTransitionsCommand implements Command {
        private final State scan;
        private final State choose;
        private final State wait;
        private final State report;

        BuildScanTransitionsCommand(State scan, State choose, State wait, State report) {
            this.scan = scan;
            this.choose = choose;
            this.wait = wait;
            this.report = report;
        }

        @Override public void execute(Context context, State currentState) {
            boolean bugFound = Boolean.TRUE.equals(context.get("bugFound"));
            Object t = context.get("targets");
            int targetsSize = 0;
            if (t instanceof List<?>) targetsSize = ((List<?>) t).size();

            // Если найден баг — переходим в REPORT с вероятностью 1
            if (bugFound) {
                currentState.addNextState(report, 1.0);
                return;
            }

            // Если есть цели — обычно выбираем цель, иногда ждём (моделируем задержки)
            if (targetsSize > 0) {
                currentState.addNextState(choose, 0.85);
                currentState.addNextState(wait, 0.15);
                return;
            }

            // Нет целей — ждём, затем повторное сканирование
            currentState.addNextState(wait, 1.0);
        }

        @Override public String getName() { return "build_scan_transitions"; }
    }

    /** Выбирает цель из памяти и сохраняет как "selectedTarget". */
    static class ChooseTargetCommand implements Command {
        @SuppressWarnings("unchecked")
        @Override public void execute(Context context, State currentState) {
            Object t = context.get("targets");
            if (!(t instanceof List<?> list) || list.isEmpty()) {
                context.put("selectedTarget", null);
                return;
            }
            Object first = list.get(0);
            context.put("selectedTarget", first);
        }
        @Override public String getName() { return "choose_target"; }
    }

    /** Отправляет действие "UI_CLICK" с координатами выбранной цели из памяти. */
    static class ClickCommand implements Command {
        @SuppressWarnings("unchecked")
        @Override public void execute(Context context, State currentState) {
            Object t = context.get("selectedTarget");
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("state", currentState.getName());

            if (t instanceof Map<?, ?> m) {
                Object x = m.get("x");
                Object y = m.get("y");
                attrs.put("x", x);
                attrs.put("y", y);
            }
            context.emit(new SimpleOutputMessage("UI_CLICK", attrs));
        }
        @Override public String getName() { return "click"; }
    }

    /**
     * Шаг проверки после клика.
     *
     * Атрибут входного сообщения: clickOk (boolean)
     * - если false — переходим в REPORT (1.0)
     * - если true — обычно возвращаемся в SCAN_UI, иногда в REPORT (моделируем случайное обнаружение бага)
     */
    static class BuildVerifyTransitionsCommand implements Command {
        private final State scan;
        private final State report;
        BuildVerifyTransitionsCommand(State scan, State report) {
            this.scan = scan;
            this.report = report;
        }

        @Override public void execute(Context context, State currentState) {
            InputMessage msg = context.getInputMessage();
            Map<String, Object> attrs = msg != null ? msg.getAttributes() : Collections.emptyMap();
            boolean ok = Boolean.TRUE.equals(attrs.get("clickOk"));

            if (!ok) {
                currentState.addNextState(report, 1.0);
                return;
            }
            currentState.addNextState(scan, 0.9);
            currentState.addNextState(report, 0.1);
        }

        @Override public String getName() { return "build_verify_transitions"; }
    }

    /** Отправляет отчёт, используя данные из памяти. */
    static class ReportCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("state", currentState.getName());
            attrs.put("scanCount", context.get("scanCount"));
            attrs.put("bugFound", context.get("bugFound"));
            attrs.put("selectedTarget", context.get("selectedTarget"));
            context.emit(new SimpleOutputMessage("REPORT", attrs));
        }

        @Override public String getName() { return "report"; }
    }

    /** Состояние WAIT отправляет запрос на задержку, затем возвращается в SCAN_UI. */
    static class WaitCommand implements Command {
        @Override public void execute(Context context, State currentState) {
            context.emit(new SimpleOutputMessage("SLEEP", Map.of("ms", 250, "state", currentState.getName())));
        }

        @Override public String getName() { return "wait"; }
    }

    public static void main(String[] args) {

        // Создаём состояния один раз
        State SCAN_UI = new State("SCAN_UI", false);
        State CHOOSE_TARGET = new State("CHOOSE_TARGET", false);
        State CLICK = new State("CLICK", false);
        State VERIFY = new State("VERIFY", false);
        State WAIT = new State("WAIT", false);
        State REPORT = new State("REPORT", true);

        // Общий детерминированный источник случайности для стабильности вывода
        RandomProvider provider = new SequenceRandomProvider(0.10, 0.95, 0.20, 0.05, 0.50, 0.99);

        // Алгоритмы ("tick")

        // SCAN_UI
        AlgorithmBuilder scan = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "scan_enter")))
                .addCommand(new ScanUiCommand())
                .addCommand(new ClearNextStatesCommand())
                .addCommand(new BuildScanTransitionsCommand(SCAN_UI, CHOOSE_TARGET, WAIT, REPORT))
                .addCommand(new ProbabilisticTransitionCommand(provider));
        SCAN_UI.addAlgorithm("tick", scan.build());

        // CHOOSE_TARGET
        AlgorithmBuilder choose = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "choose_enter")))
                .addCommand(new ChooseTargetCommand())
                .clearNextStates()
                .addExistingState(CLICK, false, 1.0)
                .probabilisticTransition(provider);
        CHOOSE_TARGET.addAlgorithm("tick", choose.build());

        // CLICK
        AlgorithmBuilder click = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "click_enter")))
                .addCommand(new ClickCommand())
                .clearNextStates()
                .addExistingState(VERIFY, false, 1.0)
                .probabilisticTransition(provider);
        CLICK.addAlgorithm("tick", click.build());

        // VERIFY
        AlgorithmBuilder verify = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "verify_enter")))
                .addCommand(new ClearNextStatesCommand())
                .addCommand(new BuildVerifyTransitionsCommand(SCAN_UI, REPORT))
                .addCommand(new ProbabilisticTransitionCommand(provider));
        VERIFY.addAlgorithm("tick", verify.build());

        // WAIT
        AlgorithmBuilder wait = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "wait_enter")))
                .addCommand(new WaitCommand())
                .clearNextStates()
                .addExistingState(SCAN_UI, false, 1.0)
                .probabilisticTransition(provider);
        WAIT.addAlgorithm("tick", wait.build());

        // REPORT (final)
        AlgorithmBuilder report = new AlgorithmBuilder("tick")
                .addCommand(new EmitCommand("LOG", Map.of("event", "report_enter")))
                .addCommand(new ReportCommand());
        REPORT.addAlgorithm("tick", report.build());

        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(SCAN_UI);

        // Сценарий: несколько шагов (цикл агента)
        System.out.println("\n=== Tester-agent style scenario ===");

        // Шаг 1: сканирование с целями
        automaton.step("tick", scanMsg(List.of(target(120, 80), target(200, 150)), false));
        printStep(automaton);

        // Шаг 2: выбор цели
        automaton.step("tick", SimpleInputMessage.of("choose"));
        printStep(automaton);

        // Шаг 3: клик отправляет действие UI_CLICK
        automaton.step("tick", SimpleInputMessage.of("click"));
        printStep(automaton);

        // Шаг 4: проверка OK — обычно возврат к сканированию
        automaton.step("tick", verifyMsg(true));
        printStep(automaton);

        // Шаг 5: сканирование без целей — переход в WAIT
        automaton.step("tick", scanMsg(Collections.emptyList(), false));
        printStep(automaton);

        // Шаг 6: WAIT отправляет SLEEP и возвращается к сканированию
        automaton.step("tick", SimpleInputMessage.of("wait"));
        printStep(automaton);

        // Шаг 7: сканирование находит баг — переход в REPORT
        automaton.step("tick", scanMsg(List.of(target(42, 24)), true));
        printStep(automaton);

        // Шаг 8: в REPORT выполняем алгоритм и отправляем отчёт
        automaton.step("tick", SimpleInputMessage.of("report"));
        printStep(automaton);

        System.out.println("Final state: " + automaton.getCurrentStateName());
    }

    private static Map<String, Object> target(int x, int y) {
        Map<String, Object> t = new HashMap<>();
        t.put("x", x);
        t.put("y", y);
        return t;
    }

    private static InputMessage scanMsg(List<Map<String, Object>> targets, boolean bugFound) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("targets", targets);
        attrs.put("bugFound", bugFound);
        return new SimpleInputMessage("scan", attrs);
    }

    private static InputMessage verifyMsg(boolean clickOk) {
        return new SimpleInputMessage("verify", Map.of("clickOk", clickOk));
    }

    private static void printStep(CoreProbabilisticAutomaton a) {
        System.out.println("-> current state: " + a.getCurrentStateName());
        List<OutputMessage> out = a.drainOutbox();
        for (OutputMessage m : out) {
            System.out.println("   OUT: " + m.getType() + " " + m.getAttributes());
        }
    }
}
