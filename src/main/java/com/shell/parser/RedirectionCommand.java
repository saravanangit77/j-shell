package com.shell.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a command with I/O redirection.
 * Example: echo hello > output.txt
 * Example: ls 2> errors.txt
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedirectionCommand extends Command {
    private String stdInFile;   // Input redirection: < file.txt
    private String stdOutFile;  // Output redirection: > file.txt
    private String stdErrorFile; // Error redirection: 2> file.txt
    private boolean append;

    @Override
    public CommandType getType() {
        return CommandType.REDIRECTION;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public void setStdErrorFile(String stdErrorFile) {
        this.stdErrorFile = stdErrorFile;
    }

    public void setStdOutFile(String stdOutFile) {
        this.stdOutFile = stdOutFile;
    }

    public void setStdInFile(String stdInFile) {
        this.stdInFile = stdInFile;
    }

    @Override
    public String toString() {
       return "RedirectionCommand{" +
               "executable='" + getExecutable() + '\'' +
               ", args=" + getArgs() +
               ", stdInFile='" + stdInFile + '\'' +
               ", stdOutFile='" + stdOutFile + '\'' +
               ", stdErrorFile='" + stdErrorFile + '\'' +
               ", append=" + append +
               '}';
    }
}


