package automaton.random;
import java.util.Map;
import org.apache.commons.math3.distribution.*;
public class SimpleRandomProviderFactory implements RandomProviderFactory {
    public RandomProvider create(String name, Map<String, Object> p) {
        String n = name.toLowerCase();
        switch (n) {
            case "uniform": return new JavaRandomProvider();
            case "normal": {
                double mean = getD(p,"mean",0.0), sd = getD(p,"sd",1.0);
                boolean cdf = getB(p,"cdf",true);
                return new RealDistributionProvider(new NormalDistribution(mean, sd), cdf);
            }
            case "exponential": {
                double mean = getD(p,"mean",1.0);
                boolean cdf = getB(p,"cdf",true);
                return new RealDistributionProvider(new ExponentialDistribution(mean), cdf);
            }
            case "beta": {
                double a = getD(p,"alpha",2.0), b = getD(p,"beta",5.0);
                boolean cdf = getB(p,"cdf",false);
                return new RealDistributionProvider(new BetaDistribution(a, b), cdf);
            }
            default: return new JavaRandomProvider();
        }
    }
    private static double getD(Map<String,Object> m, String k, double d){ Object v=m.get(k); return v instanceof Number ? ((Number)v).doubleValue():d; }
    private static boolean getB(Map<String,Object> m, String k, boolean d){ Object v=m.get(k); return v instanceof Boolean ? (Boolean)v:d; }
}
