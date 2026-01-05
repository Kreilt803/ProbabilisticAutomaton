package automaton.random;
import java.util.Map;
public interface RandomProviderFactory {
    RandomProvider create(String name, Map<String, Object> params);
}
