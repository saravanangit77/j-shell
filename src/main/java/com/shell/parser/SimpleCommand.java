package com.shell.parser;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a simple command with no redirection or piping.
 * Example: echo hello world
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SimpleCommand extends Command {
    
    public SimpleCommand(String executable, List<String> args) {
        this.executable = executable;
        this.args = args;
    }

    @Override
    public CommandType getType() {
        return CommandType.SIMPLE;
    }

    @Override
    public String toString() {
        return "SimpleCommand{" +
                "executable='" + executable + '\'' +
                ", args=" + args +
                '}';
    }
}

