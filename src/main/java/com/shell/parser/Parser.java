package com.shell.parser;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    
    /**
     * Main entry point for parsing. Analyzes the input and returns the appropriate Command type.
     * @param input The raw command line input
     * @return Command object (SimpleCommand, RedirectionCommand, or PipelineCommand)
     */
    public static Command parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("empty command input");
        }

        // Check for pipeline first (contains |)
        if (input.contains("|")) {
            return parsePipeline(input);
        }

        // Tokenize the input
        List<String> tokens = tokenize(input);

        // Check for redirection operators
        if (containsRedirectionOperators(tokens)) {
            return parseRedirection(tokens);
        }

        // Otherwise, it's a simple command
        return parseSimple(tokens);
    }

    /**
     * Checks if the token list contains redirection operators.
     */
    private static boolean containsRedirectionOperators(List<String> tokens) {
        for (String token : tokens) {
            if ("<".equals(token) || ">".equals(token) || ">>".equals(token) || "2>".equals(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a simple command with no redirection or piping.
     */
    private static SimpleCommand parseSimple(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("empty command tokens");
        }

        String executable = tokens.get(0);
        
        // Validate that executable is not a redirection operator
        if ("<".equals(executable) || ">".equals(executable) || ">>".equals(executable) || "2>".equals(executable)) {
            throw new IllegalArgumentException("redirection operator '" + executable + "' cannot be used as command");
        }

        SimpleCommand cmd = new SimpleCommand();
        cmd.setExecutable(executable);
        
        // All remaining tokens are arguments
        List<String> args = new ArrayList<>();
        for (int i = 1; i < tokens.size(); i++) {
            args.add(tokens.get(i));
        }
        cmd.setArgs(args);
        
        return cmd;
    }

    /**
     * Parses a pipeline command (commands separated by |).
     */
    private static PipelineCommand parsePipeline(String input) {
        List<Command> commands = new ArrayList<>();
        // Use -1 to preserve trailing empty strings (for trailing pipe detection)
        String[] segments = input.split("\\|", -1);
        
        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("empty command segment in pipeline");
            }
            
            List<String> tokens = tokenize(segment);
            
            // Validate that we have at least one token
            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("empty command segment in pipeline");
            }
            
            // Each segment could be a simple command or a redirection command
            if (containsRedirectionOperators(tokens)) {
                commands.add(parseRedirection(tokens));
            } else {
                commands.add(parseSimple(tokens));
            }
        }
        
        PipelineCommand pipelineCmd = new PipelineCommand();
        pipelineCmd.setCommands(commands);
        return pipelineCmd;
    }

    public static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null || input.isEmpty()) return tokens;

        StringBuilder token = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        char[] inputArr = input.toCharArray();
        for (int i = 0; i < input.length(); i++) {
            char c = inputArr[i];

            // Handle escaping (outside quotes and inside double quotes)
            if (c == '\\') {
                if (inSingle) {
                    // inside single quotes backslash is literal
                    token.append('\\');
                } else {
                    // escape next char (if any)
                    i++;
                    if (i >= input.length()) {
                        throw new RuntimeException("Trailing backslash");
                    }
                    token.append(inputArr[i]);
                }
                continue;
            }

            // Toggle single quote (literal mode), only if not in double quotes
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue; // do not include the quote char
            }

            // Toggle double quote, only if not in single quotes
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue; // do not include the quote char
            }

            // Whitespace separates tokens only when not inside quotes
            if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                // skip the whitespace
                continue;
            }

            // Normal character
            token.append(c);
        }

        // After loop: error if still inside a quote
        if (inSingle || inDouble) {
            throw new RuntimeException("Unterminated quote");
        }

        // Add last token if present
        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        return tokens;
    }

    /**
     * Parses a command with redirection operators (>, >>, 2>).
     * @deprecated Use {@link #parse(String)} instead for automatic type detection
     */
    @Deprecated
    public static RedirectionCommand parseRedirection(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("empty command tokens");
        }

        RedirectionCommand rc = new RedirectionCommand();
        List<String> args = new ArrayList<>();

        // First token is the executable
        String executable = tokens.get(0);
        
        // Validate that executable is not a redirection operator
        if ("<".equals(executable) || ">".equals(executable) || ">>".equals(executable) || "2>".equals(executable)) {
            throw new IllegalArgumentException("redirection operator '" + executable + "' has no target filename");
        }
        
        rc.setExecutable(executable);

        // pendingOperator holds one of "<", ">", ">>", "2>" when we are expecting a filename next
        String pendingOperator = null;

        for (int i = 1; i < tokens.size(); i++) {
            String tok = tokens.get(i);

            // If we are currently expecting a filename for a redirection operator
            if (pendingOperator != null) {
                // tok is the filename for pendingOperator
                if (tok.isEmpty()) {
                    throw new IllegalArgumentException("redirection filename is empty");
                }

                switch (pendingOperator) {
                    case "<":
                        rc.setStdInFile(tok);
                        break;
                    case ">":
                        rc.setStdOutFile(tok);
                        rc.setAppend(false);
                        break;
                    case ">>":
                        rc.setStdOutFile(tok);
                        rc.setAppend(true);
                        break;
                    case "2>":
                        rc.setStdErrorFile(tok);
                        // append semantics for stderr not supported in this milestone
                        break;
                    default:
                        // shouldn't happen
                        throw new IllegalStateException("unknown pending operator: " + pendingOperator);
                }

                // clear pending operator after consuming filename
                pendingOperator = null;
                continue;
            }

            // Not currently expecting a filename: check if token is an operator
            if ("<".equals(tok) || ">".equals(tok) || ">>".equals(tok) || "2>".equals(tok)) {
                pendingOperator = tok;
                continue;
            }

            // Normal argument
            args.add(tok);
        }

        // If an operator was the last token with no filename, that's an error
        if (pendingOperator != null) {
            throw new IllegalArgumentException("redirection operator '" + pendingOperator + "' has no target filename");
        }

        rc.setArgs(args);
        return rc;
    }


}
