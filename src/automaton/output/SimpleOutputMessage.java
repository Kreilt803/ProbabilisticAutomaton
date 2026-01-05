package automaton.output;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleOutputMessage implements OutputMessage {

    private final String type;
    private final Map<String, Object> attributes;

    public SimpleOutputMessage(String type, Map<String, Object> attributes) {
        this.type = type;
        this.attributes = (attributes == null) ? new HashMap<>() : new HashMap<>(attributes);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String toString() {
        return "OutputMessage{type='" + type + "', attributes=" + attributes + "}";
    }
}
