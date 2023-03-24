package DataInternals;

import java.io.Serializable;

public record Request(Object tag, Object request) implements Serializable {
}