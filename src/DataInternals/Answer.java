package DataInternals;

import java.io.Serializable;

public record Answer(Object tag, Object answer) implements Serializable {
}