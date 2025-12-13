package com.shell.parser;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test suite for Parser - Milestone 3: Quoting rules
 * Tests tokenization with single quotes, double quotes, and backslash escaping
 */
public class ParserTest {

    // ========== Basic Tokenization Tests ==========
    
    @Test
    public void testSimpleTokenization() {
        List<String> tokens = Parser.tokenize("echo hello world");
        assertEquals(3, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello", tokens.get(1));
        assertEquals("world", tokens.get(2));
    }

    @Test
    public void testEmptyInput() {
        List<String> tokens = Parser.tokenize("");
        assertEquals(0, tokens.size());
    }

    @Test
    public void testMultipleSpaces() {
        List<String> tokens = Parser.tokenize("echo    hello     world");
        assertEquals(3, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello", tokens.get(1));
        assertEquals("world", tokens.get(2));
    }

    // ========== Single Quote Tests ==========
    
    @Test
    public void testSingleQuotes() {
        List<String> tokens = Parser.tokenize("echo 'hello world'");
        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello world", tokens.get(1));
    }

    @Test
    public void testSingleQuotesPreservesBackslash() {
        List<String> tokens = Parser.tokenize("echo 'hello\\nworld'");
        assertEquals(2, tokens.size());
        assertEquals("hello\\nworld", tokens.get(1));
    }

    @Test
    public void testSingleQuotesPreservesDoubleQuote() {
        List<String> tokens = Parser.tokenize("echo 'hello\"world'");
        assertEquals(2, tokens.size());
        assertEquals("hello\"world", tokens.get(1));
    }

    @Test
    public void testSingleQuotesWithSpecialChars() {
        List<String> tokens = Parser.tokenize("echo '$PATH'");
        assertEquals(2, tokens.size());
        assertEquals("$PATH", tokens.get(1));
    }

    @Test(expected = RuntimeException.class)
    public void testUnterminatedSingleQuote() {
        Parser.tokenize("echo 'hello world");
    }

    // ========== Double Quote Tests ==========
    
    @Test
    public void testDoubleQuotes() {
        List<String> tokens = Parser.tokenize("echo \"hello world\"");
        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello world", tokens.get(1));
    }

    @Test
    public void testDoubleQuotesWithEscape() {
        List<String> tokens = Parser.tokenize("echo \"hello\\nworld\"");
        assertEquals(2, tokens.size());
        // In shell parsing, \n is treated as literal 'n' (not converted to newline)
        // Escape sequences like \n are interpreted by commands, not the parser
        assertEquals("hellonworld", tokens.get(1));
    }

    @Test
    public void testDoubleQuotesEscapeQuote() {
        List<String> tokens = Parser.tokenize("echo \"hello\\\"world\"");
        assertEquals(2, tokens.size());
        assertEquals("hello\"world", tokens.get(1));
    }

    @Test
    public void testDoubleQuotesWithSingleQuote() {
        List<String> tokens = Parser.tokenize("echo \"hello'world\"");
        assertEquals(2, tokens.size());
        assertEquals("hello'world", tokens.get(1));
    }

    @Test(expected = RuntimeException.class)
    public void testUnterminatedDoubleQuote() {
        Parser.tokenize("echo \"hello world");
    }

    // ========== Backslash Escape Tests ==========
    
    @Test
    public void testBackslashEscape() {
        List<String> tokens = Parser.tokenize("echo hello\\ world");
        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello world", tokens.get(1));
    }

    @Test
    public void testBackslashEscapeSpecialChars() {
        List<String> tokens = Parser.tokenize("echo \\$PATH");
        assertEquals(2, tokens.size());
        assertEquals("$PATH", tokens.get(1));
    }

    @Test(expected = RuntimeException.class)
    public void testTrailingBackslash() {
        Parser.tokenize("echo hello\\");
    }

    @Test
    public void testBackslashBeforeQuote() {
        List<String> tokens = Parser.tokenize("echo \\\"hello\\\"");
        assertEquals(2, tokens.size());
        assertEquals("\"hello\"", tokens.get(1));
    }

    // ========== Mixed Quoting Tests ==========
    
    @Test
    public void testMixedQuotes() {
        List<String> tokens = Parser.tokenize("echo \"hello\" 'world'");
        assertEquals(3, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello", tokens.get(1));
        assertEquals("world", tokens.get(2));
    }

    @Test
    public void testAdjacentQuotes() {
        List<String> tokens = Parser.tokenize("echo \"hello\"'world'");
        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("helloworld", tokens.get(1));
    }

    @Test
    public void testQuotesInMiddle() {
        List<String> tokens = Parser.tokenize("echo he\"ll\"o");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(1));
    }

    // ========== Redirection Parsing Tests ==========
    
    @Test
    public void testSimpleStdoutRedirection() {
        List<String> tokens = Parser.tokenize("echo hello > output.txt");
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        assertEquals("echo", rc.getExecutable());
        assertEquals(1, rc.getArgs().size());
        assertEquals("hello", rc.getArgs().get(0));
        assertEquals("output.txt", rc.getStdOutFile());
        assertFalse(rc.isAppend());
        assertNull(rc.getStdErrorFile());
    }

    @Test
    public void testAppendRedirection() {
        List<String> tokens = Parser.tokenize("echo hello >> output.txt");
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        assertEquals("echo", rc.getExecutable());
        assertEquals(1, rc.getArgs().size());
        assertEquals("hello", rc.getArgs().get(0));
        assertEquals("output.txt", rc.getStdOutFile());
        assertTrue(rc.isAppend());
    }

    @Test
    public void testStderrRedirection() {
        List<String> tokens = Parser.tokenize("type missing.txt 2> error.log");
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        assertEquals("type", rc.getExecutable());
        assertEquals(1, rc.getArgs().size());
        assertEquals("missing.txt", rc.getArgs().get(0));
        assertEquals("error.log", rc.getStdErrorFile());
        assertNull(rc.getStdOutFile());
    }

    @Test
    public void testBothStdoutAndStderrRedirection() {
        List<String> tokens = Parser.tokenize("type file.txt > out.txt 2> err.txt");
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        assertEquals("type", rc.getExecutable());
        assertEquals(1, rc.getArgs().size());
        assertEquals("file.txt", rc.getArgs().get(0));
        assertEquals("out.txt", rc.getStdOutFile());
        assertEquals("err.txt", rc.getStdErrorFile());
    }

    @Test
    public void testRedirectionMultipleArgs() {
        List<String> tokens = Parser.tokenize("echo hello world test > output.txt");
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        assertEquals("echo", rc.getExecutable());
        assertEquals(3, rc.getArgs().size());
        assertEquals("hello", rc.getArgs().get(0));
        assertEquals("world", rc.getArgs().get(1));
        assertEquals("test", rc.getArgs().get(2));
        assertEquals("output.txt", rc.getStdOutFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRedirectionMissingFilename() {
        List<String> tokens = Parser.tokenize("echo hello >");
        Parser.parseRedirection(tokens);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRedirectionEmptyTokens() {
        Parser.parseRedirection(List.of());
    }

    // ========== Edge Cases ==========
    
    @Test
    public void testEmptyQuotes() {
        List<String> tokens = Parser.tokenize("echo \"\" ''");
        assertEquals(1, tokens.size());  // empty strings are not added as tokens
        assertEquals("echo", tokens.get(0));
    }

    @Test
    public void testTabsAndNewlines() {
        List<String> tokens = Parser.tokenize("echo\thello\tworld");
        assertEquals(3, tokens.size());
    }

    @Test
    public void testComplexQuotingScenario() {
        // Test: echo "foo bar"'baz qux'test
        List<String> tokens = Parser.tokenize("echo \"foo bar\"'baz qux'test");
        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("foo barbaz quxtest", tokens.get(1));
    }

    @Test
    public void testQuotedExecutable() {
        List<String> tokens = Parser.tokenize("'/bin/ls' -la");
        assertEquals(2, tokens.size());
        assertEquals("/bin/ls", tokens.get(0));
        assertEquals("-la", tokens.get(1));
    }
}

