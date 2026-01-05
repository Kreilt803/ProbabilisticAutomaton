package automaton.commands;

import automaton.context.Context;
import automaton.state.State;

public class ClearNextStatesCommand implements Command {
    @Override
    public void execute(Context context, State currentState) {
        System.out.println("  -> Clear next states");
        currentState.clearNextStates();
    }

    @Override
    public String getName() {
        return "clear_next_states";
    }
}