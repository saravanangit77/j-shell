package com.shell.parser;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents a pipeline of commands connected by pipes (|).
 * Example: cat file.txt | grep pattern | wc -l
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineCommand extends Command {
    private List<Command> commands;

    @Override
    public CommandType getType() {
        return CommandType.PIPELINE;
    }

    @Override
    public String getExecutable() {
        return commands != null && !commands.isEmpty() ? commands.get(0).getExecutable() : null;
    }

    @Override
    public List<String> getArgs() {
        return commands != null && !commands.isEmpty() ? commands.get(0).getArgs() : null;
    }

    @Override
    public String toString() {
        return "PipelineCommand{" +
                "commands=" + commands +
                '}';
    }
}

