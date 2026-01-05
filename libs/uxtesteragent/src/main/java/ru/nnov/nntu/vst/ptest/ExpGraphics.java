package ru.nnov.nntu.vst.ptest;
// --- Импорты для Apache Commons Math (здесь не строго обязательна, но оставим для получения mean/rate) ---
import org.apache.commons.math3.distribution.ExponentialDistribution;

// --- Импорты для JFreeChart ---
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

// --- Импорты для Swing (отображение окна) ---
import javax.swing.JFrame;
import java.awt.Dimension;


import java.util.Arrays; // Для вывода части массива в консоль



public class ExpGraphics {

    public static void main(String[] args) {
        // --- 1. Настройка параметров распределения (как и раньше) ---
        double meanTimeBetweenArrivals = 5.0; // Среднее (μ)
        double rate = 1.0 / meanTimeBetweenArrivals; // Интенсивность (λ)
        ExponentialDistribution exponentialDist = new ExponentialDistribution(meanTimeBetweenArrivals); // Для проверки параметров

        System.out.println("--- Настройка Распределения ---");
        System.out.println("Тип: Экспоненциальное");
        System.out.println("Параметр (Среднее): " + meanTimeBetweenArrivals + " минут");
        System.out.println("Интенсивность (lambda): " + rate + " событий в минуту");


        // --- 2. Генерация точек для графика PDF (в Java) ---
        // Создаем серию данных для JFreeChart (XY-график).
        // "Плотность вероятности (PDF)" - это имя серии, которое может отображаться в легенде.
        XYSeries pdfSeries = new XYSeries("Плотность вероятности (PDF)");

        // Определяем диапазон X и количество точек
        double maxX = 4.0 * meanTimeBetweenArrivals; // Например, до 4 * среднее (20.0)
        int numberOfPoints = 200; // Количество точек для плавной кривой
        double step = maxX / (numberOfPoints - 1);

        System.out.println("\n--- Генерация точек для графика PDF ---");
        System.out.println("Диапазон X: от 0 до " + maxX);
        System.out.println("Количество точек: " + numberOfPoints);

        // Временные массивы для вывода примера в консоль (не обязательны для графика)
        double[] xValuesExample = new double[5];
        double[] yValuesExample = new double[5];

        // Генерируем точки (x, y) и сразу добавляем их в XYSeries
        for (int i = 0; i < numberOfPoints; i++) {
            double x = i * step;
            // Формула PDF: y = λ * exp(-λ * x)
            double y = rate * Math.exp(-rate * x);
            // Добавляем точку (x, y) в серию данных для JFreeChart
            pdfSeries.add(x, y);

            // Сохраняем первые 5 точек для примера в консоли
            if (i < 5) {
                xValuesExample[i] = x;
                yValuesExample[i] = y;
            }
        }
        System.out.println("Генерация точек (x, y) завершена.");
        System.out.println("Пример X: " + Arrays.toString(xValuesExample) + "...");
        System.out.println("Пример Y: " + Arrays.toString(yValuesExample) + "...");


        // --- 3. Создание набора данных для JFreeChart ---
        // Оборачиваем нашу серию (pdfSeries) в коллекцию, которую понимает JFreeChart.
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(pdfSeries);
        System.out.println("\n--- Подготовка к Визуализации (JFreeChart) ---");
        System.out.println("XYSeriesCollection с данными PDF создана.");


        // --- 4. Создание графика (линейного XY-графика) с помощью JFreeChart ---

        // Задаем текстовые элементы для графика.
        String plotTitle = String.format(
                "PDF экспоненциального распределения (μ=%.1f, λ=%.2f)",
                meanTimeBetweenArrivals, rate); // Форматируем заголовок
        String xAxisLabel = "Время (x, минуты)";
        String yAxisLabel = "Плотность вероятности f(x)";

        // Используем ChartFactory.createXYLineChart для создания линейного графика.
        JFreeChart lineChart = ChartFactory.createXYLineChart(
                plotTitle,      // Заголовок графика
                xAxisLabel,     // Подпись оси X
                yAxisLabel,     // Подпись оси Y
                dataset,        // Наш набор данных (точки x, y)
                PlotOrientation.VERTICAL, // Ориентация графика
                false,          // Легенду не показываем (одна кривая)
                true,           // Включаем подсказки
                false           // URL не нужны
        );
        System.out.println("Объект графика JFreeChart (XYLineChart) успешно создан.");


        // --- 5. Отображение в окне Swing ---

        // Создаем панель Swing, которая умеет рисовать график JFreeChart.
        ChartPanel chartPanel = new ChartPanel(lineChart);
        // Задаем желаемый размер для этой панели (и, соответственно, для окна).
        chartPanel.setPreferredSize(new Dimension(800, 600)); // Ширина 800, высота 600 пикселей

        // Создаем стандартное окно приложения Swing (JFrame).
        JFrame frame = new JFrame("График PDF (JFreeChart)"); // Заголовок окна
        // Устанавливаем операцию по умолчанию при закрытии окна - завершить приложение.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Помещаем панель с графиком внутрь окна.
        frame.setContentPane(chartPanel);
        // Устанавливаем размер окна так, чтобы он вмещал содержимое (нашу панель).
        frame.pack();
        // Располагаем окно по центру экрана.
        frame.setLocationRelativeTo(null);
        // Делаем окно видимым для пользователя.
        frame.setVisible(true);

        System.out.println("\n--- Результат ---");
        System.out.println("Окно JFreeChart с графиком PDF должно быть отображено на экране.");

    }
}