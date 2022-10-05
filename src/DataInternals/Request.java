package DataInternals;

import java.io.Serializable;

public record Request(Object TAG, Object request) implements Serializable {
}