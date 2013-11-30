package ru.fizteh.fivt.students.asaitgalin.shell;

import java.io.IOException;

public interface Command {
    String getName();
    String[] parseCommandLine(String s);
    void execute(String[] args) throws IOException;
    int getArgsCount();
}
