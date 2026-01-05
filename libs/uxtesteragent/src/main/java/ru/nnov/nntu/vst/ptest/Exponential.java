package ru.nnov.nntu.vst.ptest;

import javax.swing.JFrame; // Для окна
import java.awt.Dimension; // Для задания размера панели

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.jfree.data.statistics.HistogramType;

public class Exponential {
    public static void main(String[] args) {
        //------------------------экспоненциалное распределение----------------------
        /// Сценарий: Представим, что мы моделируем время (в минутах), которое проходит между последовательными
        ///прибытиями клиентов в небольшой магазин. Мы предполагаем, что это время подчиняется экспоненциальному
        ///распределению. Мы знаем из наблюдений, что в среднем между клиентами проходит 5 минут.
        /// Наша задача:
        /// Сгенерировать 2000 симуляций этого времени между прибытиями, используя Apache Commons Math.
        /// Построить гистограмму этих 2000 значений с помощью JFreeChart, чтобы увидеть,
        ///как распределяется это время.
        // --- 1. Настройка параметров: Среднее время между прибытиями = 5 минут ---

        // Задаем СРЕДНЕЕ значение (mean) для экспоненциального распределения.
        // Это наш основной параметр, основанный на данных сценария.
        double meanTimeBetweenArrivals = 5.0; // Единицы измерения - минуты

        // Интенсивность (lambda, λ) прибытий была бы 1 / meanTimeBetweenArrivals = 1 / 5.0 = 0.2 клиента в минуту.
        // Но в конструктор ExponentialDistribution мы передаем именно СРЕДНЕЕ.
        ExponentialDistribution exponentialDist = new ExponentialDistribution(meanTimeBetweenArrivals);

        // Выводим информацию о созданном распределении в консоль.
        System.out.println("--- Настройка Распределения ---");
        System.out.println("Тип: Экспоненциальное");
        System.out.println("Параметр (Среднее время между событиями): " + meanTimeBetweenArrivals + " минут");
        System.out.println(" (Соответствующая интенсивность lambda = " + (1.0 / exponentialDist.getMean()) + " событий в минуту)");

        // --- 2. Генерация выборки: Симулируем 2000 интервалов времени ---

        // Определяем, сколько раз мы хотим симулировать время между прибытиями.
        int numberOfSimulations = 2000;

        // Создаем массив для хранения результатов каждой симуляции (каждого времени).
        double[] arrivalTimeIntervals = new double[numberOfSimulations];

        // Запускаем цикл для генерации 2000 случайных значений времени.
        System.out.println("\n--- Генерация Данных ---");
        System.out.println("Запускаем " + numberOfSimulations + " симуляций времени между прибытиями...");
        for (int i = 0; i < numberOfSimulations; i++) {
            // Метод sample() генерирует одно случайное число из заданного распределения.
            // Каждое число представляет собой возможное время (в минутах) до прибытия следующего клиента.
            arrivalTimeIntervals[i] = exponentialDist.sample();
            // Ожидаем, что большинство значений будет меньше 5, но некоторые могут быть значительно больше.
        }
        System.out.println("Генерация " + numberOfSimulations + " значений завершена.");

        // Выведем первые 10 сгенерированных значений, чтобы посмотреть на них.
        System.out.print("Пример сгенерированного времени (первые 10): ");
        for(int i = 0; i < Math.min(10, numberOfSimulations); i++) {
            // Форматируем вывод до 2 знаков после запятой для наглядности.
            System.out.printf("%.2f мин, ", arrivalTimeIntervals[i]);
        }
        System.out.println("...");
        // --- 3. Подготовка данных для гистограммы: Группируем время по интервалам ---

        // Создаем специальный объект JFreeChart для хранения данных гистограммы.
        HistogramDataset dataset = new HistogramDataset();

        // Устанавливаем тип гистограммы. FREQUENCY означает, что высота каждого столбца
        // будет показывать, сколько раз сгенерированное время попало в этот интервал.
        dataset.setType(HistogramType.FREQUENCY);

        // Определяем, на сколько столбцов (интервалов, "корзин", bins) мы хотим разбить
        // весь диапазон сгенерированных временных интервалов.
        int numberOfBins = 20; // Например, 20 столбцов

        // Добавляем наши данные в dataset.
        // "Время между прибытиями" - это имя серии данных (отобразится, если включить легенду).
        // arrivalTimeIntervals - массив с нашими 2000 значениями времени.
        // numberOfBins - количество столбцов для гистограммы.
        // JFreeChart автоматически определит минимальное и максимальное время в нашем массиве
        // и разделит этот диапазон на 20 равных интервалов.
        dataset.addSeries("Время между прибытиями", arrivalTimeIntervals, numberOfBins);

        System.out.println("\n--- Подготовка к Визуализации ---");
        System.out.println("Данные добавлены в HistogramDataset.");
        System.out.println("Количество столбцов (bins) гистограммы: " + numberOfBins);


        // --- 4. Создание гистограммы: Визуализируем распределение времени ---

        // Задаем текстовые элементы для графика.
        String plotTitle = "Распределение времени между прибытиями клиентов";
        String xAxisLabel = "Время между прибытиями (минуты)"; // Подпись оси X
        String yAxisLabel = "Количество случаев (Частота)";      // Подпись оси Y

        // Используем фабричный метод JFreeChart для создания объекта гистограммы.
        JFreeChart histogramChart = ChartFactory.createHistogram(
                plotTitle,      // Заголовок всего графика
                xAxisLabel,     // Подпись горизонтальной оси
                yAxisLabel,     // Подпись вертикальной оси
                dataset,        // Наши сгруппированные данные
                PlotOrientation.VERTICAL, // Столбцы будут вертикальными
                false,          // Легенду не показываем (т.к. всего одна серия данных)
                true,           // Включаем подсказки (показывают значения при наведении мыши)
                false           // URL-ссылки на столбцах нам не нужны
        );
        System.out.println("Объект гистограммы JFreeChart успешно создан.");

        // --- 5. Отображение в окне ---

        // Создаем панель Swing, которая умеет рисовать график JFreeChart.
        ChartPanel chartPanel = new ChartPanel(histogramChart);
        // Задаем желаемый размер для этой панели (и, соответственно, для окна).
        chartPanel.setPreferredSize(new Dimension(800, 600)); // Ширина 800, высота 600 пикселей

        // Создаем стандартное окно приложения Swing (JFrame).
        JFrame frame = new JFrame("Гистограмма времени прибытия"); // Заголовок окна
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
        System.out.println("Окно с гистограммой должно быть отображено на экране.");
    }
}
