package ru.nnov.nntu.vst.ptest;
// --- Импорты для Apache Commons Math ---
import org.apache.commons.math3.distribution.NormalDistribution; // Класс для нормального распределения

// --- Импорты для JFreeChart (используются для обоих графиков) ---
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
// --- Импорты для Гистограммы ---
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
// --- Импорты для Графика PDF (XY-линейный) ---
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
// --- Импорты для Swing (отображение окон) ---
import javax.swing.JFrame;
import java.awt.Dimension;

public class Normal {

    public static void main(String[] args) {

        /// Сценарий: Представим, что мы измеряем IQ (коэффициент интеллекта) у большой группы людей.
        ///Известно, что IQ обычно распределяется нормально. Установим среднее значение (μ) равным 100 и стандартное отклонение (σ) равным 15.
        ///Стандартное отклонение показывает, насколько сильно значения разбросаны вокруг среднего.
        /// Задачи:
        /// Сгенерировать 5000 случайных значений IQ из этого нормального распределения с помощью Apache Commons Math.
        /// Построить гистограмму этих 5000 значений с помощью JFreeChart, чтобы увидеть эмпирическое распределение.
        /// Построить график теоретической функции плотности вероятности (PDF) для этого нормального распределения (знаменитый "колокол Гаусса") с помощью JFreeChart.

        // --- Настройка параметров нормального распределения ---
        // задаем параметры согласно нашему сценарию (IQ)
        double meanIQ = 100.0; // Среднее значение (μ)
        double stdDeviationIQ = 15.0; // Стандартное отклонение (σ)

        // Конструктор принимает среднее и стандартное отклонение.
        NormalDistribution normalDist = new NormalDistribution(meanIQ, stdDeviationIQ);

        System.out.println("--- Настройка Распределения ---");
        System.out.println("Тип: Нормальное (Гауссово)");
        System.out.println("Среднее (μ): " + meanIQ);
        System.out.println("Стандартное отклонение (σ): " + stdDeviationIQ);


        // =======================================================================
        // ЧАСТЬ 1: ГИСТОГРАММА СГЕНЕРИРОВАННЫХ ДАННЫХ
        // =======================================================================

        // --- Генерация выборки данных (значений IQ) ---
        int numberOfSamples = 5000; // Количество симулированных IQ
        double[] iqSamples = new double[numberOfSamples]; // Массив для хранения результатов

        System.out.println("\n--- ЧАСТЬ 1: Гистограмма ---");
        System.out.println("Генерация " + numberOfSamples + " случайных значений IQ...");
        for (int i = 0; i < numberOfSamples; i++) {
            // Метод sample() генерирует одно случайное значение из распределения.
            iqSamples[i] = normalDist.sample();
            // Ожидаем, что большинство значений будет около 100,
            // примерно 68% в диапазоне [85, 115] (μ ± σ),
            // примерно 95% в диапазоне [70, 130] (μ ± 2σ).
        }
        System.out.println("Генерация завершена.");


        // --- Подготовка данных для гистограммы ---
        HistogramDataset histogramDataset = new HistogramDataset();
        histogramDataset.setType(HistogramType.FREQUENCY); // Показываем абсолютные частоты

        // Определяем количество столбцов (bins) для гистограммы.
        int numberOfBins = 40;

        // Добавляем наши сгенерированные IQ в набор данных гистограммы.
        // "Simulated IQ" - название серии.
        // iqSamples - массив данных.
        // numberOfBins - количество столбцов.
        // JFreeChart сам определит диапазон значений (min/max IQ) и разобьет его на bins.
        histogramDataset.addSeries("Simulated IQ", iqSamples, numberOfBins);
        System.out.println("Данные для гистограммы подготовлены (" + numberOfBins + " столбцов).");


        // --- Создание гистограммы ---
        String histogramTitle = "Гистограмма симулированных значений IQ";
        String histogramXAxisLabel = "Значение IQ";
        String histogramYAxisLabel = "Частота (Количество)";

        JFreeChart histogramChart = ChartFactory.createHistogram(
                histogramTitle,
                histogramXAxisLabel,
                histogramYAxisLabel,
                histogramDataset,
                PlotOrientation.VERTICAL,
                false, // Легенда не нужна (одна серия)
                true,  // Включить подсказки
                false  // URL не нужны
        );
        System.out.println("Объект гистограммы JFreeChart создан.");


        // --- Отображение гистограммы в окне ---
        displayChartInFrame(histogramChart, "Гистограмма IQ", 800, 600);
        System.out.println("Окно с гистограммой отображено.");


        // =======================================================================
        // ЧАСТЬ 2: ГРАФИК ТЕОРЕТИЧЕСКОЙ PDF
        // =======================================================================
        System.out.println("\n--- ЧАСТЬ 2: График PDF ---");

        // --- Генерация точек для графика PDF ---
        // Нам нужно рассчитать значения функции плотности вероятности (PDF)
        // для ряда значений X (IQ) в интересующем нас диапазоне.

        XYSeries pdfSeries = new XYSeries("Теоретическая PDF (μ=100, σ=15)");

        // Определяем диапазон X для построения графика.
        // Обычно достаточно взять μ ± 3σ или μ ± 4σ. Возьмем ±4σ.
        double rangeMinX = meanIQ - 4.0 * stdDeviationIQ; // 100 - 4*15 = 40
        double rangeMaxX = meanIQ + 4.0 * stdDeviationIQ; // 100 + 4*15 = 160
        int numberOfPoints = 200; // Количество точек для плавной кривой
        double step = (rangeMaxX - rangeMinX) / (numberOfPoints - 1); // Шаг по оси X

        System.out.println("Расчет точек для графика PDF...");
        System.out.println("Диапазон X (IQ): от " + String.format("%.1f", rangeMinX) + " до " + String.format("%.1f", rangeMaxX));

        for (int i = 0; i < numberOfPoints; i++) {
            double x = rangeMinX + i * step; // Текущее значение X (IQ)
            // Используем метод density() объекта NormalDistribution для вычисления значения PDF в точке x.
            double y = normalDist.density(x);
            // Добавляем точку (x, y) в серию данных.
            pdfSeries.add(x, y);
        }
        System.out.println("Расчет " + numberOfPoints + " точек PDF завершен.");


        // --- Создание набора данных для XY-графика ---
        XYSeriesCollection pdfDataset = new XYSeriesCollection();
        pdfDataset.addSeries(pdfSeries);
        System.out.println("Данные для графика PDF подготовлены.");


        // --- Создание линейного графика PDF ---
        String pdfTitle = "Функция плотности вероятности (PDF) нормального распределения";
        String pdfXAxisLabel = "Значение IQ (x)";
        String pdfYAxisLabel = "Плотность вероятности f(x)";

        JFreeChart pdfChart = ChartFactory.createXYLineChart(
                pdfTitle,
                pdfXAxisLabel,
                pdfYAxisLabel,
                pdfDataset,
                PlotOrientation.VERTICAL,
                false, // Легенда не нужна
                true,  // Включить подсказки
                false  // URL не нужны
        );
        System.out.println("Объект графика PDF JFreeChart создан.");


        // --- Отображение графика PDF в отдельном окне ---
        displayChartInFrame(pdfChart, "График PDF IQ", 800, 600);
        System.out.println("Окно с графиком PDF отображено.");

    } // Конец main метода

    /**
     * Вспомогательный метод для отображения графика JFreeChart в окне JFrame.
     * chart Объект JFreeChart для отображения.
     * title Заголовок окна.
     * width Ширина окна.
     * height Высота окна.
     */
    private static void displayChartInFrame(JFreeChart chart, String title, int width, int height) {
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(width, height));

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null); // Центрировать окно
        frame.setVisible(true);
    }

} // Конец класса