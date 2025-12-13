package com.shell.parser;

import java.util.ArrayList;
import java.util.List;

public class Parser {
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

    public static RedirectionCommand parseRedirection(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("empty command tokens");
        }

        RedirectionCommand rc = new RedirectionCommand();
        List<String> args = new ArrayList<>();

        // First token is the executable
        rc.setExecutable(tokens.get(0));

        // pendingOperator holds one of ">", ">>", "2>" when we are expecting a filename next
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
            if (">".equals(tok) || ">>".equals(tok) || "2>".equals(tok)) {
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
