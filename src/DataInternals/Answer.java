package DataInternals;

import java.io.Serializable;

public record Answer(Object TAG, Object answer) implements Serializable {
}