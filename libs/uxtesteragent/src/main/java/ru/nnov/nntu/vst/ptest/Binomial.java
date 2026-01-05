package ru.nnov.nntu.vst.ptest;

import javax.swing.JFrame;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import org.apache.commons.math3.distribution.BinomialDistribution;

public class Binomial {
    public static void main(String[] args) {

        //-----------------------попытка 2 (Симуляция кликов)---------------------------------------

        int trials = 1000; // количество пользователей
        double clickProbability = 0.3; // 30% вероятность, что пользователь кликнет на кнопку на сайте

        // Распределение Бернулли через биномиальное распределение (1 испытание = 1 пользователь)
        BinomialDistribution bernoulli = new BinomialDistribution(1, clickProbability);

        int clicked = 0;
        int notClicked = 0;

        for (int i = 0; i < trials; i++) {
            int result = bernoulli.sample();
            if (result == 1) clicked++;
            else notClicked++;
        }

        System.out.println("Clicked: " + clicked);
        System.out.println("Not Clicked: " + notClicked);

        // --- Визуализация с использованием JFreeChart ---

        // 1. Создание набора данных для столбчатой диаграммы
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // Добавляем значения: значение, ключ ряда (серии), ключ категории (столбца)
        dataset.addValue((Number)clicked, "Users", "Clicked");
        dataset.addValue((Number)notClicked, "Users", "Not Clicked");

        // 2. Создание столбчатой диаграммы (Bar Chart)
        JFreeChart barChart = ChartFactory.createBarChart(
                "User Click Simulation (p = 0.3)", // Заголовок графика
                "Event",                         // Название оси категорий (X)
                "Number of users",                 // Название оси значений (Y)
                dataset,                         // Наш набор данных
                PlotOrientation.VERTICAL,        // Ориентация графика (вертикальные столбцы)
                false,                           // Отображать ли легенду (не нужна для одного ряда)
                true,                            // Включить подсказки (tooltips)
                false                            // Включить генерацию URL (не нужна)
        );

        // 3. Создание панели для отображения графика
        // ChartPanel - это компонент Swing, который можно добавить в окно
        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(560, 370)); // Задаем предпочтительный размер панели

        // 4. Создание окна (JFrame) для отображения панели с графиком
        JFrame frame = new JFrame("Simulation Results"); // Заголовок окна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Закрытие окна завершает программу
        frame.setContentPane(chartPanel); // Устанавливаем панель с графиком как содержимое окна
        frame.pack(); // Устанавливаем размер окна по размеру содержимого (chartPanel)
        frame.setLocationRelativeTo(null); // Центрируем окно на экране
        frame.setVisible(true); // Делаем окно видимым

    }

}
