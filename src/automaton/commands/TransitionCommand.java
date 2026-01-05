package automaton.commands;

import automaton.context.Context;
import automaton.state.State;
import java.util.*;

public class TransitionCommand implements Command {
    private final int stateIndex;

    public TransitionCommand(int stateIndex) {
        this.stateIndex = stateIndex;
    }

    @Override
    public void execute(Context context, State currentState) {
        List<State> nextStates = currentState.getNextStates();
        if (nextStates.isEmpty()) {
            System.out.println("  -> No next states, staying in: " + currentState.getName());
            return;
        }

        int idx = stateIndex;
        if (stateIndex == -1) { // Маркер: последний
            idx = nextStates.size() - 1;
        } else if (stateIndex == -2) { // Маркер: первый
            idx = 0;
        }

        if (idx >= 0 && idx < nextStates.size()) {
            State nextState = nextStates.get(idx);
            context.setState(nextState);
            System.out.println("  -> Deterministic transition to: " + nextState.getName());
        } else {
            System.out.println("  -> Error: invalid state index " + stateIndex);
        }
    }

    @Override
    public String getName() {
        return "transition_to_" + stateIndex;
    }
}