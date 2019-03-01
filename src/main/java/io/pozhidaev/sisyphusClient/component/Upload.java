package io.pozhidaev.sisyphusClient.component;

import java.nio.file.Path;

public interface Upload {

    void setFile(Path path);
    void start();

}
//