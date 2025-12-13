package com.shell;

import com.shell.parser.Parser;
import com.shell.parser.RedirectionCommand;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Shell {
    private static final String PROMPT = "my-shell$ ";


    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        System.out.println("Welcome to MyShell — Milestone 1");
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        try {
            while (true) {
                System.out.print(PROMPT);
                line = reader.readLine();
                if (line == null) { // EOF (e.g., Ctrl-D)
                    System.out.println();
                    break;
                }


                line = line.trim();
                if (line.isEmpty()) continue; // ignore empty lines


                // simple tokenization by whitespace
                List<String> cmdArgs = Parser.tokenize(line);
                String cmd = cmdArgs.get(0);


                if(line.contains(">")){
                    RedirectionCommand redirectionCommand = Parser.parseRedirection(cmdArgs);
                    executeRedirectionCommand(redirectionCommand);
                   continue;
                }
                cmdArgs.remove(0);
                switch (cmd) {
                    case "exit":
                        handleExit(cmdArgs);
                        // handleExit will call System.exit(0) or break the loop
                        return;
                    case "echo":
                        handleEcho(cmdArgs);
                        break;
                    case "type":
                        handleType(cmdArgs);
                        break;
                    case "cd":
                        // simple cd implementation for now: handle cd with absolute/relative/~ (improve later)
                        currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
                        if (cmdArgs.isEmpty()) {
                            System.out.println("No directory specified");
                            currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
                        } else {
                            String target = cmdArgs.get(0);
                            System.out.println("Target :" + target);
                            if (target.equals("~") || target.equals("~/")) {
                                currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
                            } else {
                                Path candidate = currentDir.resolve(target).normalize();
                                if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                                    currentDir = candidate.toAbsolutePath();
                                    System.out.println(Paths.get(System.getProperty("user.dir")).toAbsolutePath());
                                } else {
                                    System.out.printf("cd: %s: No such file or directory%n", target);
                                }
                            }
                        }
                        break;
                    default:
                        String exePath = findExecutable(cmd); // call your PATH lookup here
                        System.out.println("ExePath :"+ exePath);
                        if (exePath == null) {
                            System.out.printf("%s: command not found%n", cmd);
                        } else {
                            // combine exePath + original args
                            String[] argv = new String[1 + cmdArgs.size()];
                            argv[0] = exePath;
                            System.arraycopy(cmdArgs.toArray(new String[0]), 0, argv, 1, cmdArgs.size());
                            try {
                                executeExternal(argv, currentDir);
                                // you can decide whether to print exit code or ignore
                            } catch (IOException | InterruptedException ex) {
                                System.err.println("Error running command: " + ex.getMessage());
                            }
                        }
                }
            }

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }


    private static void handleExit(List<String> args) {
        if (!args.isEmpty()) {
            // optional: allow `exit N` to set return code
            try {
                int code = Integer.parseInt(args.get(0));
                System.exit(code);
            } catch (NumberFormatException e) {
                System.err.println("exit: numeric argument required");
                System.exit(1);
            }
        }
        System.exit(0);
    }


    private static void handleEcho(List<String> args) {
        System.out.println(String.join(" ", args));
    }
    private static void handleEcho(List<String> args, String outFile, boolean isAppend, String errorFile) {
        String message = String.join(" ", args) + System.lineSeparator();

        // CASE 1: stdout redirected
        if (outFile != null) {
            try (FileWriter fw = new FileWriter(outFile, isAppend)) {
                fw.write(message);
            } catch (IOException e) {
                // writing stdout failed → report error
                writeError("echo: " + e.getMessage(), errorFile);
            }
        }
    }

    private static void writeError(String message, String errorFile) {
        if(errorFile != null){
            try (FileWriter fw = new FileWriter(errorFile)) {
                fw.append(message);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }


    private static void handleType(List<String> args) {
        // `type filename` prints file contents to stdout
        if (args.isEmpty()) {
            System.err.println("type: missing operand");
            return;
        }


        for (String filename : args) {
            File f = new File(filename);
            if (!f.exists()) {
                System.err.printf("type: %s: No such file or directory\n", filename);
                continue;
            }
            if (f.isDirectory()) {
                System.err.printf("type: %s: Is a directory\n", filename);
                continue;
            }
            try (BufferedReader fr = new BufferedReader(new FileReader(f))) {
                String l;
                while ((l = fr.readLine()) != null) {
                    System.out.println(l);
                }
            } catch (IOException e) {
                System.err.printf("type: %s: %s\n", filename, e.getMessage());
            }
        }
    }

    private static void handleType(List<String> args, String outFile, boolean isAppend, String errorFile) {
        // `type filename` prints file contents to stdout
        if (args.isEmpty()) {
            System.err.println("type: missing operand");
            return;
        }

        boolean wroteSomething = false;
        for (String filename : args) {
            File f = new File(filename);
            if (!f.exists()) {
                String errorMsg = "type:"+filename+": No such file or directory\n";
                writeError(errorMsg, errorFile);
                continue;
            }
            if (f.isDirectory()) {
                String errorMsg = "type:"+filename+": Is a directory\n";
                writeError(errorMsg,errorFile);
                continue;
            }
            StringBuilder message = new StringBuilder();
            try (BufferedReader fr = new BufferedReader(new FileReader(f))) {
                String l;
                while ((l = fr.readLine()) != null) {
                    message.append(l).append(System.lineSeparator());
                }

                if (outFile != null) {
                    boolean realAppend = isAppend || wroteSomething;
                    try (FileWriter fw = new FileWriter(outFile, realAppend)) {
                        fw.write(message.toString());
                        wroteSomething = true;
                    } catch (IOException e) {
                        // writing stdout failed → report error
                        writeError("echo: " + e.getMessage(), errorFile);
                    }
                }

            } catch (IOException e) {
                System.err.printf("type: %s: %s\n", filename, e.getMessage());
            }
        }
    }

    private static String findExecutable(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }

        // 1. If the command contains "/" → treat as absolute or relative path
        if (command.contains("/")) {
            File f = new File(command);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
            return null;
        }

        // 2. Otherwise, search through PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return null;
        }

        String[] paths = pathEnv.split(":");
        for (String dir : paths) {
            File f = new File(dir, command);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }

        return null; // Not found anywhere
    }

    private static int executeExternal(String[] argv, Path workingDir) throws IOException, InterruptedException {
        if (argv == null || argv.length == 0) throw new IllegalArgumentException("argv empty");

        ProcessBuilder pb = new ProcessBuilder(argv);
        if (workingDir != null) pb.directory(workingDir.toFile());
        // Inherit IO so child process prints to the same stdout/stderr as the shell
        pb.inheritIO();

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            // Distinguish common errors if you want:
            // e.g., "Permission denied" vs "No such file"
            System.out.println("Error : "+e.getMessage());
            throw e;
        }

        int exitCode = p.waitFor();
        return exitCode;
    }


    public static void executeRedirectionCommand(RedirectionCommand rc){
        String executable = rc.getExecutable();
        if(isBuiltIn(executable)){
//            switch bases on methods
            switch (executable){
                case "echo": handleEcho(rc.getArgs(), rc.getStdOutFile(),rc.isAppend(), rc.getStdErrorFile()); break;
                case "type" :handleType(rc.getArgs(), rc.getStdOutFile(),rc.isAppend(), rc.getStdErrorFile()); break;
            }
            return;
        }
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        executeExternal(rc, currentDir);
        }

    private static boolean isBuiltIn(String executable) {
        if(executable == null || executable.isBlank())
            throw new IllegalArgumentException("empty command");

        Set<String> builtIns = Set.of("cd","echo", "type");

        return builtIns.contains(executable);
    }

    private static void executeExternal(RedirectionCommand rc, Path workingDir) {
        List<String> cmd = new ArrayList<>();
        cmd.add(rc.getExecutable());
        cmd.addAll(rc.getArgs());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir.toFile());

        // STDOUT redirection
        if (rc.getStdOutFile() != null) {
            if (rc.isAppend()) {
                pb.redirectOutput(
                        ProcessBuilder.Redirect.appendTo(new File(rc.getStdOutFile()))
                );
            } else {
                pb.redirectOutput(new File(rc.getStdOutFile()));
            }
        }

        // STDERR redirection
        if (rc.getStdErrorFile() != null) {
            pb.redirectError(new File(rc.getStdErrorFile()));
        }

        try {
            Process p = pb.start();
            p.waitFor();
        } catch (IOException e) {
            // executable not found / permission denied
            writeError(
                    rc.getExecutable() + ": " + e.getMessage(),
                    rc.getStdErrorFile()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(
                    "process interrupted",
                    rc.getStdErrorFile()
            );
        }
    }


}
