package automaton.state;

import automaton.context.Context;
import automaton.commands.Command;
import automaton.random.RandomProvider;
import java.util.*;

public class State {
    private final String name;
    private final boolean isFinal;
    private final List<State> nextStates = new ArrayList<>();
    private final List<Double> transitionProbabilities = new ArrayList<>();
    private final Map<String, List<Command>> algorithms = new HashMap<>();

    public State(String name, boolean isFinal) {
        this.name = name;
        this.isFinal = isFinal;
    }

    /**
     * Добавляет переход к следующему состоянию.
     *
     * Важно: вероятности не нормализуются при каждом добавлении,
     * чтобы итоговое распределение не зависело от порядка добавления.
     * Вместо этого probability трактуется как вес и нормализуется лениво
     * при выборке или при возврате карты вероятностей.
     */
    public void addNextState(State state, double probability) {
        if (!nextStates.contains(state)) {
            nextStates.add(state);
            transitionProbabilities.add(probability);
        }
    }

    public void addNextState(State state) {
        addNextState(state, 1.0); // Вероятность по умолчанию
    }

    private double sumPositiveWeights() {
        double sum = 0.0;
        for (double w : transitionProbabilities) {
            if (w > 0.0) sum += w;
        }
        return sum;
    }

    private double[] normalisedProbabilitiesOrUniform() {
        int n = transitionProbabilities.size();
        double[] probs = new double[n];
        if (n == 0) return probs;

        double sum = sumPositiveWeights();
        if (sum <= 0.0) {
            // Если все веса <= 0, используем равномерное распределение
            double p = 1.0 / n;
            for (int i = 0; i < n; i++) probs[i] = p;
            return probs;
        }

        for (int i = 0; i < n; i++) {
            probs[i] = Math.max(0.0, transitionProbabilities.get(i)) / sum;
        }
        return probs;
    }

    public State selectNextState(Random random) {
        if (nextStates.isEmpty()) return this;

        return selectNextState(random.nextDouble());
    }

    public void clearNextStates() {
        nextStates.clear();
        transitionProbabilities.clear();
    }

    public void addAlgorithm(String algorithmName, List<Command> commands) {
        algorithms.put(algorithmName, new ArrayList<>(commands));
    }

    public void removeAlgorithm(String algorithmName) {
        algorithms.remove(algorithmName);
    }

    public void executeAlgorithm(Context context, String algorithmName) {
        List<Command> commands = algorithms.get(algorithmName);
        if (commands != null) {
            System.out.println("State " + name + ": executing algorithm '" + algorithmName + "'");
            for (Command command : commands) {
                command.execute(context, this);
            }
        } else {
            System.out.println("Algorithm '" + algorithmName + "' not found in state " + name);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public List<State> getNextStates() {
        return new ArrayList<>(nextStates);
    }

    public Set<String> getAlgorithmNames() {
        return algorithms.keySet();
    }

    public State selectNextState(double u) {
        if (nextStates.isEmpty()) return this;
        double[] probs = normalisedProbabilitiesOrUniform();

        double cumulative = 0.0;
        for (int i = 0; i < nextStates.size(); i++) {
            cumulative += probs[i];
            if (u <= cumulative) return nextStates.get(i);
        }
        return nextStates.get(nextStates.size() - 1);
    }

    public State selectNextState(automaton.random.RandomProvider provider) {
        return selectNextState(provider.nextUnit());
    }

    public Map<State, Double> getTransitionProbabilities() {
        Map<State, Double> probabilities = new HashMap<>();
        double[] probs = normalisedProbabilitiesOrUniform();
        for (int i = 0; i < nextStates.size(); i++) {
            probabilities.put(nextStates.get(i), probs[i]);
        }
        return probabilities;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        State state = (State) obj;
        return Objects.equals(name, state.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + (isFinal ? " (final)" : "");
    }
}

