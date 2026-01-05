package automaton.random;
import org.apache.commons.math3.distribution.RealDistribution;
public class RealDistributionProvider implements RandomProvider {
    private final RealDistribution dist;
    private final boolean useCdf;
    public RealDistributionProvider(RealDistribution dist, boolean useCdf) {
        this.dist = dist; this.useCdf = useCdf;
    }
    public double nextUnit() {
        double x = dist.sample();
        if (useCdf) {
            double u = dist.cumulativeProbability(x);
            if (u < 0) return 0; if (u > 1) return 1; return u;
        }
        double u = 1.0 / (1.0 + Math.exp(-x));
        if (u < 0) return 0; if (u > 1) return 1; return u;
    }
}
