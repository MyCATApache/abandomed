package io.mycat.backend;

import java.io.IOException;

@FunctionalInterface
public interface WriteCompleteListener {
    void wirteComplete() throws IOException;
}
