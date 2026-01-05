package automaton.commands;

import automaton.context.Context;
import automaton.state.State;

public class AddStateWithProbabilityCommand implements Command {
    private final String stateName;
    private final boolean isFinal;
    private final double probability;

    public AddStateWithProbabilityCommand(String stateName, boolean isFinal, double probability) {
        this.stateName = stateName;
        this.isFinal = isFinal;
        this.probability = probability;
    }

    @Override
    public void execute(Context context, State currentState) {
        State newState = new State(stateName, isFinal);
        currentState.addNextState(newState, probability);
        System.out.println("  -> Add next state: " + stateName + " (probability: " + probability + ")");
    }

    @Override
    public String getName() {
        return "add_state_with_probability_" + stateName;
    }
}