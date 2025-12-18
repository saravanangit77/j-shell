package com.shell.parser;

import lombok.Data;

import java.util.List;

@Data
public abstract class Command {
    protected String executable;
    protected List<String> args;

    /**
     * Returns the type of this command.
     * Subclasses should override this to return their specific type.
     */
    public abstract CommandType getType();

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getExecutable() {
        return executable;
    }

    public List<String> getArgs() {
        return args;
    }
}
