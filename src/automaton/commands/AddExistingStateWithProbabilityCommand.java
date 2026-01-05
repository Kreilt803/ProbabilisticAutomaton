package automaton.commands;

import automaton.context.Context;
import automaton.state.State;

/**
 * Добавляет переход к уже существующему экземпляру состояния.
 *
 * Используйте эту команду, когда алгоритмы должны быть привязаны к конкретным состояниям
 * (объекты состояний создаются один раз, настраиваются и затем переиспользуются).
 */
public class AddExistingStateWithProbabilityCommand implements Command {

    private final State targetState;
    private final double probability;

    public AddExistingStateWithProbabilityCommand(State targetState, double probability) {
        if (targetState == null) {
            throw new IllegalArgumentException("targetState is null");
        }
        this.targetState = targetState;
        this.probability = probability;
    }

    @Override
    public void execute(Context context, State currentState) {
        currentState.addNextState(targetState, probability);
        System.out.println("  -> Add existing next state: " + targetState.getName() +
                " (probability: " + probability + ")");
    }

    @Override
    public String getName() {
        return "add_existing_state_with_probability_" + targetState.getName();
    }
}
