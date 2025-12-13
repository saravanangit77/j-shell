package com.shell.parser;

import lombok.Data;

import java.util.List;

@Data
public class Command {
    String executable;
    List<String> args;

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
