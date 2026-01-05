package automaton.config;

import automaton.random.JavaRandomProvider;
import automaton.random.RandomProvider;
import automaton.random.RealDistributionProvider;
import org.apache.commons.math3.distribution.*;

import java.util.Locale;

/**
 * Фабрика источников случайности на основе JSON-конфигурации.
 * Позволяет задавать распределения без изменения кода автомата.
 */
public final class JsonRandomProviderFactory {

    private JsonRandomProviderFactory() {
        // Утилитарный класс
    }

    /** Создаёт RandomProvider из JSON-конфигурации. */
    public static RandomProvider fromConfig(AutomatonConfig.RandomConfig cfg) {
        if (cfg == null || cfg.type == null) {
            return new JavaRandomProvider();
        }

        String t = cfg.type.toLowerCase(Locale.ROOT);
        // По умолчанию используем CDF, если явно указано "sigmoid" — используем сигмоиду
        boolean useCdf = cfg.mode == null || !"sigmoid".equalsIgnoreCase(cfg.mode);

        switch (t) {
            case "uniform":
                return new JavaRandomProvider();

            case "normal": {
                double mean = cfg.mean != null ? cfg.mean : 0.0;
                double sd   = cfg.sd   != null ? cfg.sd   : 1.0;
                return new RealDistributionProvider(
                        new NormalDistribution(mean, sd),
                        useCdf
                );
            }

            case "beta": {
                double alpha = cfg.alpha != null ? cfg.alpha : 2.0;
                double beta  = cfg.beta  != null ? cfg.beta  : 5.0;
                return new RealDistributionProvider(
                        new BetaDistribution(alpha, beta),
                        useCdf
                );
            }

            case "exponential": {
                if (cfg.mean != null) {
                    return new RealDistributionProvider(
                            new ExponentialDistribution(cfg.mean),
                            useCdf
                    );
                }
                double lambda = cfg.lambda != null ? cfg.lambda : 1.0;
                double mean   = 1.0 / lambda;
                return new RealDistributionProvider(
                        new ExponentialDistribution(mean),
                        useCdf
                );
            }

            case "gamma": {
                double shape = cfg.shape != null ? cfg.shape : 2.0;
                double scale = cfg.scale != null ? cfg.scale : 1.0;
                return new RealDistributionProvider(
                        new GammaDistribution(shape, scale),
                        useCdf
                );
            }

            default:
                return new JavaRandomProvider();
        }
    }
}
