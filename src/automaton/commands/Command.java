package automaton.commands;

import automaton.context.Context;
import automaton.state.State;

public interface Command {
    void execute(Context context, State currentState);
    String getName();
}