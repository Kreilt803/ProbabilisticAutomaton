# Probabilistic Automaton (KA)

Мини-библиотека для моделирования поведения агентов (например, «тестировщиков») через **вероятностный конечный автомат**.

Идея простая:
- у нас есть **состояния** (State);
- у каждого состояния есть **обработчик(и)** — *алгоритмы* (Algorithm) в виде последовательности **команд** (Command);
- команды могут:
  - делать переходы (детерминированные/вероятностные);
  - выполнять прикладные действия (кликнуть, сохранить в «память», отправить сообщение актору и т.п.) — это вы добавляете своими командами;
- вероятностный переход выбирает следующее состояние по весам/вероятностям;
- можно задавать источник случайности через разные распределения (uniform/normal/beta/exponential/gamma);
- можно грузить автомат из JSON.

> Автомат — это «дирижёр». Он выбирает, *в какое состояние перейти*, а уже состояние через алгоритм решает, *что делать в мире*.

---

## Быстрый старт

### Требования
- Java 21
- Gradle (в проекте есть wrapper, но в оффлайне он может пытаться скачать дистрибутив)

### Запуск демо
В проекте есть примеры:
- `Main.java` — демонстрации распределений и вероятностного выбора
- `JsonDemo.java` — загрузка автомата из `automaton_config.json`
- `HistoryDemo.java` — пример учёта истории и входного сообщения

---

## Основные сущности и как они связаны

### `CoreProbabilisticAutomaton`
Фасад/движок, который хранит `Context` и текущий `State`.

Ключевые методы:
- `executeCurrentStateAlgorithm(String algorithmName)` — выполнить алгоритм в текущем состоянии (без ресета).
- `step(String algorithmName, InputMessage msg)` — **один шаг** автомата с сохранением текущего состояния и истории.
- `run(String algorithmName, InputMessage msg)` — **одноразовый запуск**: перед выполнением делает `reset()` в initialState.
- `reset()` / `resetTo(State)` — управлять начальным состоянием.

### `State`
Состояние автомата:
- `name`, `isFinal`
- список `nextStates` + их **веса переходов**
- `algorithms`: `Map<String, List<Command>>`

Важно: веса переходов **нормализуются лениво** при выборе следующего состояния.

### `Context`
Контекст выполнения:
- `currentState`
- список уникальных состояний, встреченных в процессе
- **полная история** посещений `stateHistory`
- последнее входное сообщение `InputMessage`

Это то, что вы можете читать/менять в командах.

### `Command`
Команда — атомарный шаг алгоритма.

Интерфейс:
```java
void execute(Context context, State currentState);
String getName();
```

Встроенные команды:
- `ClearNextStatesCommand` — очистить список nextStates
- `AddStateWithProbabilityCommand` — создать *новое* состояние и добавить как nextState
- `AddExistingStateWithProbabilityCommand` — добавить nextState, **ссылаясь на уже созданный State**
- `TransitionCommand` — переход по индексу nextStates
- `ProbabilisticTransitionCommand` — вероятностный переход
- `HistoryBasedTransitionCommand` — вероятностный переход, где вероятности вычисляет ваш `HistoryProbabilityProvider`

### `RandomProvider` и распределения
`RandomProvider` выдаёт число `u ∈ [0,1)`.
Реализации:
- `JavaRandomProvider` — равномерное
- `RealDistributionProvider` — берёт `dist.sample()` и переводит в [0,1] через CDF или sigmoid

### `InputMessage`
Единый контейнер для входа (событие UI, текст, JSON и т.д.):
- `getRaw()` — строковый payload
- `getAttributes()` — карта фич/метаданных

Есть простой вариант: `SimpleInputMessage.of("text")`.

---

## Как собрать автомат в коде (ручная сборка)

Ниже пример с 3 состояниями: **SCAN_UI → CHOOSE_ACTION → EXECUTE_ACTION**.

Идея:
- в каждом состоянии алгоритм называется одинаково, например `"handle"`;
- агент при каждом входном событии вызывает `automaton.step("handle", msg)`.

```java
import automaton.core.CoreProbabilisticAutomaton;
import automaton.builder.AlgorithmBuilder;
import automaton.state.State;
import automaton.input.SimpleInputMessage;

public class Example {
  public static void main(String[] args) {
    State scan   = new State("SCAN_UI", false);
    State choose = new State("CHOOSE_ACTION", false);
    State exec   = new State("EXECUTE_ACTION", false);
    State done   = new State("DONE", true);

    // Алгоритм состояния SCAN_UI
    scan.addAlgorithm("handle", new AlgorithmBuilder("handle")
        // ... здесь могли бы быть ваши команды: распарсить UI, положить в память и т.д.
        .clearNextStates()
        .addExistingState(choose, false, 1.0)
        .probabilisticTransition()
        .build());

    // CHOOSE_ACTION: выбираем следующую ветку (например, 70% -> EXECUTE_ACTION, 30% -> DONE)
    choose.addAlgorithm("handle", new AlgorithmBuilder("handle")
        .clearNextStates()
        .addExistingState(exec, false, 0.7)
        .addExistingState(done, true, 0.3)
        .probabilisticTransition()
        .build());

    // EXECUTE_ACTION: сделали действие и вернулись сканировать
    exec.addAlgorithm("handle", new AlgorithmBuilder("handle")
        // ... ваши команды: «кликнуть», «ввести текст», «проверить assert» и т.п.
        .clearNextStates()
        .addExistingState(scan, false, 1.0)
        .probabilisticTransition()
        .build());

    CoreProbabilisticAutomaton automaton = new CoreProbabilisticAutomaton(scan);

    // Агентный цикл: пришло событие/сообщение -> сделали шаг
    for (int i = 0; i < 10 && !automaton.isInFinalState(); i++) {
      automaton.step("handle", SimpleInputMessage.of("ui_event"));
      System.out.println("Now in: " + automaton.getCurrentStateName());
    }
  }
}
```

> Аналогия: `step()` — это «один тик мозга агента».

---

## Вероятности переходов и распределения

### Веса/вероятности
Вы задаёте **веса** (weights). При выборе следующего состояния они нормализуются в вероятности.

Пример:
- веса `0.7` и `0.3` → вероятности `0.7` и `0.3`
- веса `2` и `1` → вероятности `0.666...` и `0.333...`

### Подмена источника случайности
```java
import automaton.random.RealDistributionProvider;
import org.apache.commons.math3.distribution.NormalDistribution;

new AlgorithmBuilder("handle")
  .clearNextStates()
  .addExistingState(a, false, 0.5)
  .addExistingState(b, false, 0.5)
  .probabilisticTransition(new RealDistributionProvider(new NormalDistribution(0, 1), true))
  .build();
```

`useCdf=true` — берём `u = CDF(x)`.
`useCdf=false` — берём `u = sigmoid(x)`.

---

## Учет истории (History-based переход)

Если вам нужно, чтобы вероятности зависели от:
- последних N состояний,
- входного сообщения,
- внутренней «памяти» агента,

…то используйте `HistoryBasedTransitionCommand` через `AlgorithmBuilder.historyBasedTransition(...)`.

Интерфейс:
```java
double[] computeProbabilities(List<State> history,
                              InputMessage input,
                              List<State> nextStates);
```

Пример есть в `HistoryDemo.java`.

---

## JSON-конфигурация

### Загрузка
```java
JsonAutomatonLoader loader = new JsonAutomatonLoader();
try (InputStream in = new FileInputStream("automaton_config.json")) {
  CoreProbabilisticAutomaton automaton = loader.load(in);
  automaton.executeCurrentStateAlgorithm("check");
}
```

### Формат (кратко)
- `initialState`
- `states[]`
  - `name`, `finalState`
  - `algorithms: { algoName: [ commands... ] }`

Поддержанные типы команд в JSON:
- `clear_next_states`
- `add_state` (target=имя состояния, probability=вес)
- `transition` (target="first"|"last"|"0"|"1"...)
- `probabilistic_transition` (random=описание распределения)

Пример файла см. `automaton_config.json`.

---

## Как расширять

1) Добавить новое состояние:
- создать `new State("NAME", isFinal)`
- прописать алгоритм `state.addAlgorithm("handle", builder.build())`
- добавить его в переходы других состояний.

2) Добавить новую команду:
- создать класс `implements Command`
- в `execute(...)` сделать нужное действие (или сложить действие в память)
- добавить её в `AlgorithmBuilder.addCommand(new YourCommand(...))`

3) Сделать вероятности умнее:
- реализовать `HistoryProbabilityProvider`
- использовать `historyBasedTransition(provider)`.

---

## Частые грабли (важно прочитать)

1) **Многошаговый автомат требует ссылок на существующие состояния.**
Если вы хотите, чтобы после перехода у состояния был свой алгоритм — используйте `AddExistingStateWithProbabilityCommand` (через `AlgorithmBuilder.addExistingState(...)`).

2) **Команды получают параметром `currentState`, который равен состоянию, где запущен алгоритм.**
Если вы делаете переход в середине списка команд — последующие команды всё равно увидят старый `currentState`. Поэтому:
- либо ставьте переход последней командой,
- либо в командах, которым важен актуальный state, берите `context.getCurrentState()`.

3) JSON сейчас не поддерживает history-based переходы (можно расширить `JsonAutomatonLoader`).
