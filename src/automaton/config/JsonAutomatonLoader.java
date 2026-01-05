package automaton.config;

import automaton.builder.AlgorithmBuilder;
import automaton.commands.Command;
import automaton.core.CoreProbabilisticAutomaton;
import automaton.random.RandomProvider;
import automaton.state.State;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Загрузчик вероятностного автомата из JSON-конфигурации.
 * Позволяет собирать CoreProbabilisticAutomaton без написания Java-кода.
 */
public class JsonAutomatonLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    /** Загружает автомат из JSON-потока. */
    public CoreProbabilisticAutomaton load(InputStream in) throws IOException {
        AutomatonConfig cfg = mapper.readValue(in, AutomatonConfig.class);

        if (cfg.states == null || cfg.states.isEmpty()) {
            throw new IllegalArgumentException("Config.states is empty");
        }

        // Создаём все состояния заранее, чтобы переходы могли ссылаться по имени
        Map<String, State> stateMap = new LinkedHashMap<>();
        for (AutomatonConfig.StateConfig sc : cfg.states) {
            State st = new State(sc.name, sc.finalState);
            stateMap.put(sc.name, st);
        }

        // Для каждого состояния собираем алгоритмы из команд
        for (AutomatonConfig.StateConfig sc : cfg.states) {
            State st = stateMap.get(sc.name);
            if (sc.algorithms == null || sc.algorithms.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, java.util.List<AutomatonConfig.CommandConfig>> entry
                    : sc.algorithms.entrySet()) {

                String algoName = entry.getKey();
                java.util.List<AutomatonConfig.CommandConfig> commandsCfg = entry.getValue();

                AlgorithmBuilder builder = new AlgorithmBuilder(algoName);

                for (AutomatonConfig.CommandConfig cc : commandsCfg) {
                    if (cc.type == null) {
                        throw new IllegalArgumentException("Command type is null in algorithm " + algoName);
                    }

                    String type = cc.type.toLowerCase(Locale.ROOT);

                    switch (type) {
                        case "clear_next_states":
                            builder.clearNextStates();
                            break;

                        case "add_state": {
                            if (cc.target == null) {
                                throw new IllegalArgumentException("add_state: target is null");
                            }
                            State target = stateMap.get(cc.target);
                            if (target == null) {
                                throw new IllegalArgumentException("Unknown state in add_state: " + cc.target);
                            }
                            double prob = cc.probability != null ? cc.probability : 1.0;
                            builder.addExistingState(target, target.isFinal(), prob);
                            break;
                        }

                        case "transition": {
                            if (cc.target == null) {
                                throw new IllegalArgumentException("transition: target is null");
                            }
                            String t = cc.target.toLowerCase(Locale.ROOT);
                            if ("first".equals(t)) {
                                builder.transitionToFirst();
                            } else if ("last".equals(t)) {
                                builder.transitionToLast();
                            } else {
                                int index = Integer.parseInt(cc.target);
                                builder.transitionTo(index);
                            }
                            break;
                        }

                        case "probabilistic_transition": {
                            RandomProvider rp = JsonRandomProviderFactory.fromConfig(cc.random);
                            builder.probabilisticTransition(rp);
                            break;
                        }

                        default:
                            throw new IllegalArgumentException("Unsupported command type: " + cc.type);
                    }
                }

                java.util.List<Command> algoCommands = builder.build();
                st.addAlgorithm(algoName, algoCommands);
            }
        }

        // Устанавливаем стартовое состояние
        State initial = stateMap.get(cfg.initialState);
        if (initial == null) {
            throw new IllegalArgumentException("Unknown initialState: " + cfg.initialState);
        }

        return new CoreProbabilisticAutomaton(initial);
    }
}
