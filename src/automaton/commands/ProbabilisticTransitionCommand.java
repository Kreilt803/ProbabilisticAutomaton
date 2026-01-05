package automaton.commands;
import automaton.context.Context;
import automaton.state.State;
import automaton.random.RandomProvider;
import automaton.random.JavaRandomProvider;
import java.util.*;

public class ProbabilisticTransitionCommand implements Command {
    private final RandomProvider provider;

    public ProbabilisticTransitionCommand() {
        this.provider = new JavaRandomProvider();
    }

    public ProbabilisticTransitionCommand(RandomProvider provider) {
        this.provider = provider;
    }

    @Override
    public void execute(Context context, State currentState) {
        if (currentState == null) return;
        if (!currentState.getNextStates().isEmpty()) {
            State nextState = currentState.selectNextState(provider);
            context.setState(nextState);
            System.out.println("  -> Probabilistic transition to: " + nextState.getName());
            Map<State, Double> probabilities = currentState.getTransitionProbabilities();
            if (!probabilities.isEmpty()) System.out.println("    Probabilities: " + probabilities);
        } else {
            System.out.println("  -> Stay in current state: " + currentState.getName());
        }
    }

    @Override
    public String getName() { return "probabilistic_transition"; }
}
