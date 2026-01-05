package automaton.config;

import java.util.List;
import java.util.Map;

/**
 * Корневой объект конфигурации автомата из JSON.
 * Используется JsonAutomatonLoader для построения CoreProbabilisticAutomaton.
 */
public class AutomatonConfig {

    /** Имя начального состояния. */
    public String initialState;

    /** Список описаний состояний. */
    public List<StateConfig> states;

    public static class StateConfig {
        /** Имя состояния. */
        public String name;

        /** Признак конечного состояния. */
        public boolean finalState;

        /** Алгоритмы состояния: ключ — имя алгоритма, значение — список команд. */
        public Map<String, java.util.List<CommandConfig>> algorithms;
    }

    public static class CommandConfig {
        /** Тип команды: clear_next_states, add_state, transition, probabilistic_transition */
        public String type;

        /** Для add_state: имя целевого состояния; для transition: "first"/"last" или индекс. */
        public String target;

        /** Для add_state: финальность состояния (опционально, обычно задаётся в StateConfig). */
        public Boolean finalState;

        /** Для add_state: вес (вероятность) перехода. */
        public Double probability;

        /** Для probabilistic_transition: описание источника случайности. */
        public RandomConfig random;
    }

    public static class RandomConfig {
        /** Тип распределения: uniform, normal, beta, exponential, gamma */
        public String type;

        /** Режим: "cdf" (по умолчанию) или "sigmoid" */
        public String mode;

        public Double mean;    // для normal и exponential
        public Double sd;      // для normal
        public Double alpha;   // для beta
        public Double beta;    // для beta
        public Double lambda;  // для exponential
        public Double shape;   // для gamma
        public Double scale;   // для gamma
    }
}
