package automaton.random;
import java.util.Random;
public class JavaRandomProvider implements RandomProvider {
    private final Random random;
    public JavaRandomProvider() { this(new Random()); }
    public JavaRandomProvider(Random random) { this.random = random; }
    public double nextUnit() { return random.nextDouble(); }
}
