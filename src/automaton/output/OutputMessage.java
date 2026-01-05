package automaton.output;

import java.util.Map;

/**
 * Универсальное выходное сообщение, создаваемое автоматом.
 * Внешняя система (AI-агент/актор) решает, как его обработать.
 */
public interface OutputMessage {

    /** Логический тип сообщения, например "UI_CLICK", "MEM_WRITE", "LOG". */
    String getType();

    /** Структурированные данные/атрибуты. */
    Map<String, Object> getAttributes();
}
