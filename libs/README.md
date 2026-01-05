# Локальные библиотеки

## uxtesteragent

Библиотека из репозитория https://gitverse.ru/verlioka/uxtesteragent

### Описание
Библиотека содержит примеры работы с распределениями из Apache Commons Math и их визуализацией с помощью JFreeChart.

### Классы:
- `ru.nnov.nntu.vst.ptest.Normal` - работа с нормальным распределением
- `ru.nnov.nntu.vst.ptest.Exponential` - работа с экспоненциальным распределением
- `ru.nnov.nntu.vst.ptest.Binomial` - работа с биномиальным распределением
- `ru.nnov.nntu.vst.ptest.ExpGraphics` - графики экспоненциального распределения

### Сборка
Библиотека автоматически собирается при сборке основного проекта через задачу `buildLocalLibrary` в `build.gradle`.

Для ручной сборки:
```bash
cd libs/uxtesteragent
./gradlew build
```

### Использование
Библиотека подключена как локальная зависимость в `build.gradle`:
```gradle
implementation files('libs/uxtesteragent/build/libs/uxtesteragent.jar')
```

### Зависимости
- Apache Commons Math 3.6.1
- JFreeChart 1.5.4
- SLF4J API 1.7.32

