import automaton.core.CoreProbabilisticAutomaton;
import automaton.state.State;
import automaton.builder.AlgorithmBuilder;
import automaton.random.RealDistributionProvider;
import automaton.random.JavaRandomProvider;
import org.apache.commons.math3.distribution.*;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class Main {
    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Не удалось установить кодировку UTF-8: " + e.getMessage());
        }

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  ДЕМОНСТРАЦИЯ ВЕРОЯТНОСТНОГО АВТОМАТА");
        System.out.println("  с различными распределениями случайных величин");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Демонстрация 1: Бета-распределение
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ ДЕМОНСТРАЦИЯ 1: Бета-распределение (α=2, β=5)          │");
        System.out.println("│ Используется для моделирования вероятностей            │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        demonstrateBetaDistribution();

        System.out.println("\n");

        // Демонстрация 2: Нормальное распределение
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ ДЕМОНСТРАЦИЯ 2: Нормальное распределение (μ=0, σ=1)    │");
        System.out.println("│ Используется CDF для преобразования в [0,1]            │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        demonstrateNormalDistribution();

        System.out.println("\n");

        // Демонстрация 3: Экспоненциальное распределение
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ ДЕМОНСТРАЦИЯ 3: Экспоненциальное распределение (λ=2)   │");
        System.out.println("│ Моделирует время между событиями                       │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        demonstrateExponentialDistribution();

        System.out.println("\n");

        // Демонстрация 4: Гамма-распределение
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ ДЕМОНСТРАЦИЯ 4: Гамма-распределение (α=2, β=1)        │");
        System.out.println("│ Используется для моделирования времени ожидания        │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        demonstrateGammaDistribution();

        System.out.println("\n");

        // Демонстрация 5: Равномерное распределение
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ ДЕМОНСТРАЦИЯ 5: Равномерное распределение (Java Random) │");
        System.out.println("│ Базовое распределение по умолчанию                     │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        demonstrateUniformDistribution();

        System.out.println("\n");

        // Демонстрация 6: Сравнение CDF и Sigmoid преобразований
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ ДЕМОНСТРАЦИЯ 6: Сравнение CDF и Sigmoid преобразований  │");
        System.out.println("│ для одного и того же распределения                      │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        demonstrateCdfVsSigmoid();

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  Демонстрация завершена!");
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    private static void demonstrateBetaDistribution() {
        State start = new State("START", false);
        State state1 = new State("Состояние_1", false);
        State state2 = new State("Состояние_2", false);
        State finalState = new State("ФИНАЛ", true);

        // Бета-распределение смещено влево, больше вероятность меньших значений
        AlgorithmBuilder algo = new AlgorithmBuilder("beta_transition")
                .clearNextStates()
                .addExistingState(state1, false, 0.3)
                .addExistingState(state2, false, 0.3)
                .addExistingState(finalState, true, 0.4)
                .probabilisticTransition(new RealDistributionProvider(new BetaDistribution(2, 5), false));

        start.addAlgorithm("beta_transition", algo.build());
        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        runAutomaton(automaton, "beta_transition", 3);
    }

    private static void demonstrateNormalDistribution() {
        State start = new State("START", false);
        State low = new State("НИЗКОЕ", false);
        State medium = new State("СРЕДНЕЕ", false);
        State high = new State("ВЫСОКОЕ", true);

        // Нормальное распределение с преобразованием через CDF
        AlgorithmBuilder algo = new AlgorithmBuilder("normal_transition")
                .clearNextStates()
                .addExistingState(low, false, 0.25)
                .addExistingState(medium, false, 0.5)
                .addExistingState(high, true, 0.25)
                .probabilisticTransition(new RealDistributionProvider(new NormalDistribution(0, 1), true));

        start.addAlgorithm("normal_transition", algo.build());
        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        runAutomaton(automaton, "normal_transition", 3);
    }

    private static void demonstrateExponentialDistribution() {
        State start = new State("START", false);
        State fast = new State("БЫСТРО", false);
        State slow = new State("МЕДЛЕННО", true);

        // Экспоненциальное распределение
        AlgorithmBuilder algo = new AlgorithmBuilder("exponential_transition")
                .clearNextStates()
                .addExistingState(fast, false, 0.7)
                .addExistingState(slow, true, 0.3)
                .probabilisticTransition(new RealDistributionProvider(new ExponentialDistribution(0.5), true));

        start.addAlgorithm("exponential_transition", algo.build());
        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        runAutomaton(automaton, "exponential_transition", 3);
    }

    private static void demonstrateGammaDistribution() {
        State start = new State("START", false);
        State stateA = new State("A", false);
        State stateB = new State("B", false);
        State stateC = new State("C", true);

        // Гамма-распределение
        AlgorithmBuilder algo = new AlgorithmBuilder("gamma_transition")
                .clearNextStates()
                .addExistingState(stateA, false, 0.4)
                .addExistingState(stateB, false, 0.4)
                .addExistingState(stateC, true, 0.2)
                .probabilisticTransition(new RealDistributionProvider(new GammaDistribution(2, 1), true));

        start.addAlgorithm("gamma_transition", algo.build());
        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        runAutomaton(automaton, "gamma_transition", 3);
    }

    private static void demonstrateUniformDistribution() {
        State start = new State("START", false);
        State option1 = new State("Вариант_1", false);
        State option2 = new State("Вариант_2", false);
        State option3 = new State("Вариант_3", true);

        // Равномерное распределение через стандартный генератор Java
        AlgorithmBuilder algo = new AlgorithmBuilder("uniform_transition")
                .clearNextStates()
                .addExistingState(option1, false, 0.33)
                .addExistingState(option2, false, 0.33)
                .addExistingState(option3, true, 0.34)
                .probabilisticTransition(new JavaRandomProvider());

        start.addAlgorithm("uniform_transition", algo.build());
        CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(start);

        runAutomaton(automaton, "uniform_transition", 3);
    }

    private static void demonstrateCdfVsSigmoid() {
        State start1 = new State("START_CDF", false);
        State start2 = new State("START_SIGMOID", false);
        State state1 = new State("Состояние_1", false);
        State state2 = new State("Состояние_2", true);

        // С преобразованием через CDF
        AlgorithmBuilder algoCdf = new AlgorithmBuilder("with_cdf")
                .clearNextStates()
                .addExistingState(state1, false, 0.6)
                .addExistingState(state2, true, 0.4)
                .probabilisticTransition(new RealDistributionProvider(new NormalDistribution(0, 1), true));

        // С преобразованием через сигмоиду
        AlgorithmBuilder algoSigmoid = new AlgorithmBuilder("with_sigmoid")
                .clearNextStates()
                .addExistingState(state1, false, 0.6)
                .addExistingState(state2, true, 0.4)
                .probabilisticTransition(new RealDistributionProvider(new NormalDistribution(0, 1), false));

        start1.addAlgorithm("with_cdf", algoCdf.build());
        start2.addAlgorithm("with_sigmoid", algoSigmoid.build());

        System.out.println("  С CDF преобразованием:");
        CoreProbabilisticAutomaton automaton1 = new CoreProbabilisticAutomaton(start1);
        runAutomaton(automaton1, "with_cdf", 2);

        System.out.println("\n  С Sigmoid преобразованием:");
        CoreProbabilisticAutomaton automaton2 = new CoreProbabilisticAutomaton(start2);
        runAutomaton(automaton2, "with_sigmoid", 2);
    }

    private static void runAutomaton(CoreProbabilisticAutomaton automaton, String algorithmName, int maxSteps) {
        for (int i = 0; i < maxSteps; i++) {
            System.out.println("  Шаг " + (i + 1) + ":");
            automaton.executeCurrentStateAlgorithm(algorithmName);
            automaton.printStatus();
            if (automaton.getCurrentState().isFinal()) {
                System.out.println("  ✓ Достигнуто конечное состояние!");
                break;
            }
            System.out.println();
        }
    }
}
