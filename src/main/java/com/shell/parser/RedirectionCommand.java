package com.shell.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor

public class RedirectionCommand extends Command{
    String stdOutFile;
    String stdErrorFile;
    boolean append;

    public void setAppend(boolean append) {
        this.append = append;
    }

    public void setStdErrorFile(String stdErrorFile) {
        this.stdErrorFile = stdErrorFile;
    }

    public void setStdOutFile(String stdOutFile) {
        this.stdOutFile = stdOutFile;
    }

    @Override
    public String toString() {
       return "RedirectionCommand{" +
               "executable='" + getExecutable() + '\'' +
               ", args=" + getArgs() +
               ", stdOutFile='" + stdOutFile + '\'' +
               ", stdErrorFile='" + stdErrorFile + '\'' +
               ", append=" + append +
               '}';
    }
}

