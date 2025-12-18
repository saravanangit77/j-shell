package com.shell;

import com.shell.parser.*;

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

                // Parse the command - automatically detects type (Simple, Redirection, or Pipeline)
                Command command;
                try {
                    command = Parser.parse(line);
                } catch (Exception e) {
                    System.err.println("Parse error: " + e.getMessage());
                    continue;
                }

                // Handle based on command type
                switch (command.getType()) {
                    case SIMPLE:
                        handleSimpleCommand((SimpleCommand) command, currentDir);
                        break;

                    case REDIRECTION:
                        handleRedirectionCommand((RedirectionCommand) command, currentDir);
                        break;

                    case PIPELINE:
                        handlePipelineCommand((PipelineCommand) command, currentDir);
                        break;

                    default:
                        System.err.println("Unknown command type");
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


    /**
     * Handles simple commands (no redirection or piping).
     */
    private static void handleSimpleCommand(SimpleCommand cmd, Path currentDir) {
        String executable = cmd.getExecutable();
        List<String> args = cmd.getArgs();

        switch (executable) {
            case "exit":
                handleExit(args);
                return;
            case "echo":
                handleEcho(args);
                break;
            case "type":
                handleType(args);
                break;
            case "cd":
                handleCd(args);
                break;
            default:
                // External command
                String exePath = findExecutable(executable);
                System.out.println("ExePath: " + exePath);
                if (exePath == null) {
                    System.out.printf("%s: command not found%n", executable);
                } else {
                    // Combine exePath + args
                    String[] argv = new String[1 + args.size()];
                    argv[0] = exePath;
                    System.arraycopy(args.toArray(new String[0]), 0, argv, 1, args.size());
                    try {
                        executeExternal(argv, currentDir);
                    } catch (IOException | InterruptedException ex) {
                        System.err.println("Error running command: " + ex.getMessage());
                    }
                }
        }
    }

    /**
     * Handles commands with I/O redirection.
     */
    private static void handleRedirectionCommand(RedirectionCommand rc, Path currentDir) {
        executeRedirectionCommand(rc, currentDir);
    }

    /**
     * Handles pipeline commands (commands connected with |).
     */
    private static void handlePipelineCommand(PipelineCommand pipelineCmd, Path currentDir) {
        executePipelineWithRedirections(pipelineCmd.getCommands(), currentDir);
    }

    /**
     * Handles the 'cd' command.
     */
    private static void handleCd(List<String> args) {
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        if (args.isEmpty()) {
            System.out.println("No directory specified");
        } else {
            String target = args.get(0);
            System.out.println("Target: " + target);
            if (target.equals("~") || target.equals("~/")) {
                currentDir = Paths.get(System.getProperty("user.home")).toAbsolutePath();
                try {
                    System.setProperty("user.dir", currentDir.toString());
                } catch (Exception e) {
                    System.err.println("cd: " + e.getMessage());
                }
            } else {
                Path candidate = currentDir.resolve(target).normalize();
                if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                    currentDir = candidate.toAbsolutePath();
                    try {
                        System.setProperty("user.dir", currentDir.toString());
                        System.out.println(currentDir);
                    } catch (Exception e) {
                        System.err.println("cd: " + e.getMessage());
                    }
                } else {
                    System.out.printf("cd: %s: No such file or directory%n", target);
                }
            }
        }
    }

    /**
     * @deprecated Use handleRedirectionCommand(RedirectionCommand, Path) instead
     */
    @Deprecated
    public static void executeRedirectionCommand(RedirectionCommand rc){
        executeRedirectionCommand(rc, Paths.get(System.getProperty("user.dir")).toAbsolutePath());
    }

    private static void executeRedirectionCommand(RedirectionCommand rc, Path currentDir){
        String executable = rc.getExecutable();
        if(isBuiltIn(executable)){
            // Handle built-in commands with redirection
            switch (executable){
                case "echo": 
                    handleEcho(rc.getArgs(), rc.getStdOutFile(), rc.isAppend(), rc.getStdErrorFile()); 
                    break;
                case "type":
                    handleType(rc.getArgs(), rc.getStdOutFile(), rc.isAppend(), rc.getStdErrorFile()); 
                    break;
                case "cd":
                    // cd with redirection doesn't make much sense, but handle it
                    handleCd(rc.getArgs());
                    break;
                case "exit":
                    handleExit(rc.getArgs());
                    break;
            }
            return;
        }
        // External command with redirection
        executeExternal(rc, currentDir);
    }

    private static boolean isBuiltIn(String executable) {
        if(executable == null || executable.isBlank())
            throw new IllegalArgumentException("empty command");

        Set<String> builtIns = Set.of("cd", "echo", "type", "exit");

        return builtIns.contains(executable);
    }

    private static void executeExternal(RedirectionCommand rc, Path workingDir) {
        List<String> cmd = new ArrayList<>();
        cmd.add(rc.getExecutable());
        cmd.addAll(rc.getArgs());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir.toFile());

        // STDIN redirection (< input.txt)
        if (rc.getStdInFile() != null) {
            pb.redirectInput(new File(rc.getStdInFile()));
        }

        // STDOUT redirection (> output.txt or >> output.txt)
        if (rc.getStdOutFile() != null) {
            if (rc.isAppend()) {
                pb.redirectOutput(
                        ProcessBuilder.Redirect.appendTo(new File(rc.getStdOutFile()))
                );
            } else {
                pb.redirectOutput(new File(rc.getStdOutFile()));
            }
        }

        // STDERR redirection (2> errors.txt)
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


    /**
     * Execute a pipeline with proper redirection support for each command.
     * Handles commands that may have input/output/error redirection.
     */
    private static void executePipelineWithRedirections(List<Command> commands, Path workingDir) {
        InputStream prevOut = null;
        List<Process> processes = new ArrayList<>();
        
        for (int i = 0; i < commands.size(); i++) {
            Command cmd = commands.get(i);
            
            // Build command list
            List<String> cmdList = new ArrayList<>();
            cmdList.add(cmd.getExecutable());
            if (cmd.getArgs() != null) {
                cmdList.addAll(cmd.getArgs());
            }
            
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.directory(workingDir.toFile());
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            
            // Handle input redirection for first command
            if (i == 0 && cmd instanceof RedirectionCommand) {
                RedirectionCommand rc = (RedirectionCommand) cmd;
                if (rc.getStdInFile() != null) {
                    pb.redirectInput(new File(rc.getStdInFile()));
                }
            }
            
            // Handle output redirection for last command
            if (i == commands.size() - 1 && cmd instanceof RedirectionCommand) {
                RedirectionCommand rc = (RedirectionCommand) cmd;
                if (rc.getStdOutFile() != null) {
                    if (rc.isAppend()) {
                        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(rc.getStdOutFile())));
                    } else {
                        pb.redirectOutput(new File(rc.getStdOutFile()));
                    }
                }
                if (rc.getStdErrorFile() != null) {
                    pb.redirectError(new File(rc.getStdErrorFile()));
                }
            }
            
            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                System.err.println("Error starting process: " + e.getMessage());
                return;
            }
            processes.add(process);
            
            // If there is previous output, pipe it into this process
            if (prevOut != null) {
                InputStream src = prevOut;
                OutputStream dest = process.getOutputStream();
                
                new Thread(() -> {
                    try {
                        src.transferTo(dest);
                        dest.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else if (i == 0) {
                // First command with no input redirection and no previous output
                // Close stdin if not redirected from file to prevent hanging
                boolean hasInputRedirection = cmd instanceof RedirectionCommand && 
                                             ((RedirectionCommand) cmd).getStdInFile() != null;
                if (!hasInputRedirection) {
                    try {
                        process.getOutputStream().close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
            
            // Update prevOut for next command
            prevOut = process.getInputStream();
        }
        
        // If last command doesn't have output redirection, print to stdout
        Command lastCmd = commands.get(commands.size() - 1);
        boolean hasOutputRedirection = lastCmd instanceof RedirectionCommand && 
                                       ((RedirectionCommand) lastCmd).getStdOutFile() != null;
        
        if (prevOut != null && !hasOutputRedirection) {
            try {
                prevOut.transferTo(System.out);
            } catch (IOException e) {
                System.err.println("Error reading pipeline output: " + e.getMessage());
            }
        }
        
        // Wait for all processes
        for (Process p : processes) {
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Pipeline interrupted");
            }
        }
    }

    /**
     * @deprecated Use executePipelineWithRedirections instead
     */
    @Deprecated
    private static void executePipeline(List<List<String>> commands) {
        InputStream prevOut = null;
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            List<String> cmd = commands.get(i);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = null;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            processes.add(process);

            // 🔹 If there is previous output, pipe it into this process
            if (prevOut != null) {
                InputStream src = prevOut;
                OutputStream dest = process.getOutputStream();

                new Thread(() -> {
                    try {
                        src.transferTo(dest);
                        dest.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // 🔹 Update prevOut for next command
            prevOut = process.getInputStream();
        }

        if (prevOut != null) {
            try {
                prevOut.transferTo(System.out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (Process p : processes) {
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }


}
