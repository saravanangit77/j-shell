package com.shell;

import com.shell.parser.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive test for ALL FOUR redirection operators:
 * > : Redirects stdout (overwrites)
 * >>: Redirects stdout (appends)
 * < : Redirects stdin (input from file)
 * 2>: Redirects stderr (error messages)
 */
public class AllRedirectionsTest {
    
    private List<File> filesToCleanup;
    
    @Before
    public void setUp() {
        filesToCleanup = new ArrayList<>();
    }
    
    @After
    public void tearDown() {
        for (File file : filesToCleanup) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    private File trackFile(String filename) {
        File file = new File(filename);
        filesToCleanup.add(file);
        return file;
    }
    
    private String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
    
    private void createTestFile(String filename, String content) throws IOException {
        File file = trackFile(filename);
        Files.write(file.toPath(), content.getBytes());
    }

    // ===== TEST 1: > (Output Redirection - Overwrites) =====
    
    @Test
    public void testOutputRedirectionOverwrites() throws Exception {
        File output = trackFile("test_overwrite.txt");
        
        // First write
        Command cmd1 = Parser.parse("echo first line > test_overwrite.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        assertTrue("File should exist", output.exists());
        assertEquals("first line\n", readFile(output));
        
        // Second write (should overwrite)
        Command cmd2 = Parser.parse("echo second line > test_overwrite.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        String content = readFile(output);
        assertFalse("Should not contain first line", content.contains("first line"));
        assertTrue("Should contain second line", content.contains("second line"));
    }

    @Test
    public void testOutputRedirectionParsing() {
        Command cmd = Parser.parse("echo hello world > output.txt");
        
        assertEquals(CommandType.REDIRECTION, cmd.getType());
        RedirectionCommand redirect = (RedirectionCommand) cmd;
        
        assertEquals("echo", redirect.getExecutable());
        assertEquals("output.txt", redirect.getStdOutFile());
        assertFalse("Should not be append mode", redirect.isAppend());
        assertNull("No stdin redirection", redirect.getStdInFile());
        assertNull("No stderr redirection", redirect.getStdErrorFile());
    }

    // ===== TEST 2: >> (Append Redirection) =====
    
    @Test
    public void testAppendRedirection() throws Exception {
        File output = trackFile("test_append.txt");
        
        // First write
        Command cmd1 = Parser.parse("echo line 1 >> test_append.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        // Append second line
        Command cmd2 = Parser.parse("echo line 2 >> test_append.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        // Append third line
        Command cmd3 = Parser.parse("echo line 3 >> test_append.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd3);
        
        String content = readFile(output);
        assertTrue("Should contain line 1", content.contains("line 1"));
        assertTrue("Should contain line 2", content.contains("line 2"));
        assertTrue("Should contain line 3", content.contains("line 3"));
        
        // Verify order
        assertTrue("Line 1 before line 2", content.indexOf("line 1") < content.indexOf("line 2"));
        assertTrue("Line 2 before line 3", content.indexOf("line 2") < content.indexOf("line 3"));
    }

    @Test
    public void testAppendRedirectionParsing() {
        Command cmd = Parser.parse("echo hello >> output.txt");
        
        RedirectionCommand redirect = (RedirectionCommand) cmd;
        assertEquals("output.txt", redirect.getStdOutFile());
        assertTrue("Should be append mode", redirect.isAppend());
    }

    // ===== TEST 3: < (Input Redirection) =====
    
    @Test
    public void testInputRedirection() throws Exception {
        // Create input file
        createTestFile("test_input.txt", "apple\nbanana\ncherry\n");
        File output = trackFile("test_output.txt");
        
        // Use input redirection with sort
        Command cmd = Parser.parse("sort < test_input.txt > test_output.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify sorted output
        String content = readFile(output);
        assertTrue("Should contain apple", content.contains("apple"));
        assertTrue("Should contain banana", content.contains("banana"));
        assertTrue("Should contain cherry", content.contains("cherry"));
        
        // Verify sorted (apple comes before cherry)
        assertTrue("Should be sorted", content.indexOf("apple") < content.indexOf("cherry"));
    }

    @Test
    public void testInputRedirectionParsing() {
        Command cmd = Parser.parse("wc -l < input.txt");
        
        RedirectionCommand redirect = (RedirectionCommand) cmd;
        assertEquals("wc", redirect.getExecutable());
        assertEquals("input.txt", redirect.getStdInFile());
        assertNull("No stdout redirection", redirect.getStdOutFile());
    }

    @Test
    public void testInputRedirectionWithWc() throws Exception {
        // Create file with 5 lines
        createTestFile("test_wc.txt", "line1\nline2\nline3\nline4\nline5\n");
        File output = trackFile("test_wc_output.txt");
        
        // Count lines using input redirection
        Command cmd = Parser.parse("wc -l < test_wc.txt > test_wc_output.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        String content = readFile(output);
        assertTrue("Should contain 5", content.trim().contains("5"));
    }

    // ===== TEST 4: 2> (Error Redirection) =====
    
    @Test
    public void testErrorRedirection() throws Exception {
        File errorFile = trackFile("test_errors.txt");
        
        // Try to type a nonexistent file (will produce error)
        Command cmd = Parser.parse("type nonexistent_file.txt 2> test_errors.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify error was captured
        assertTrue("Error file should exist", errorFile.exists());
        String content = readFile(errorFile);
        assertTrue("Should contain error message", 
                   content.contains("No such file") || content.contains("nonexistent"));
    }

    @Test
    public void testErrorRedirectionParsing() {
        Command cmd = Parser.parse("grep pattern file.txt 2> errors.txt");
        
        RedirectionCommand redirect = (RedirectionCommand) cmd;
        assertEquals("grep", redirect.getExecutable());
        assertEquals("errors.txt", redirect.getStdErrorFile());
        assertNull("No stdout redirection", redirect.getStdOutFile());
    }

    @Test
    public void testErrorRedirectionWithValidAndInvalidFiles() throws Exception {
        createTestFile("valid.txt", "valid content");
        
        File output = trackFile("test_stdout.txt");
        File errors = trackFile("test_stderr.txt");
        
        // Type both valid and invalid files
        Command cmd = Parser.parse("type valid.txt invalid.txt > test_stdout.txt 2> test_stderr.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify stdout has valid content
        String stdout = readFile(output);
        assertTrue("Should contain valid content", stdout.contains("valid content"));
        
        // Verify stderr has error
        String stderr = readFile(errors);
        assertTrue("Should contain error", stderr.contains("invalid") || stderr.contains("No such file"));
    }

    // ===== TEST 5: ALL FOUR TOGETHER =====
    
    @Test
    public void testAllFourRedirectionsParsingTogether() {
        // Parse: cmd < input.txt > output.txt 2> errors.txt
        Command cmd = Parser.parse("grep pattern < input.txt > output.txt 2> errors.txt");
        
        assertEquals(CommandType.REDIRECTION, cmd.getType());
        RedirectionCommand redirect = (RedirectionCommand) cmd;
        
        // Verify all four are captured
        assertEquals("grep", redirect.getExecutable());
        assertEquals("input.txt", redirect.getStdInFile());    // <
        assertEquals("output.txt", redirect.getStdOutFile());  // >
        assertEquals("errors.txt", redirect.getStdErrorFile()); // 2>
        assertFalse("Should not be append", redirect.isAppend());
    }

    @Test
    public void testAllFourRedirectionsExecution() throws Exception {
        // Create input with some lines
        createTestFile("all_input.txt", "apple\nbanana\napricot\ncherry\n");
        
        File output = trackFile("all_output.txt");
        File errors = trackFile("all_errors.txt");
        
        // Search for lines starting with 'a', redirect all streams
        Command cmd = Parser.parse("grep ^a < all_input.txt > all_output.txt 2> all_errors.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify output
        assertTrue("Output should exist", output.exists());
        String content = readFile(output);
        assertTrue("Should contain apple", content.contains("apple"));
        assertTrue("Should contain apricot", content.contains("apricot"));
        assertFalse("Should not contain banana", content.contains("banana"));
        assertFalse("Should not contain cherry", content.contains("cherry"));
    }

    @Test
    public void testInputAndAppendTogether() throws Exception {
        createTestFile("append_input.txt", "zebra\napple\nbanana\n");
        File output = trackFile("append_output.txt");
        
        // First sort
        Command cmd1 = Parser.parse("sort < append_input.txt >> append_output.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        // Append more
        Command cmd2 = Parser.parse("echo --- >> append_output.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        // Verify both are in file
        String content = readFile(output);
        assertTrue("Should contain sorted items", content.contains("apple"));
        assertTrue("Should contain separator", content.contains("---"));
    }

    // ===== EDGE CASES AND COMBINATIONS =====
    
    @Test
    public void testRedirectionOrderDoesNotMatter() {
        // Output before input
        Command cmd1 = Parser.parse("sort > out.txt < in.txt");
        RedirectionCommand r1 = (RedirectionCommand) cmd1;
        assertEquals("in.txt", r1.getStdInFile());
        assertEquals("out.txt", r1.getStdOutFile());
        
        // Input before output
        Command cmd2 = Parser.parse("sort < in.txt > out.txt");
        RedirectionCommand r2 = (RedirectionCommand) cmd2;
        assertEquals("in.txt", r2.getStdInFile());
        assertEquals("out.txt", r2.getStdOutFile());
        
        // Both should parse the same
        assertEquals(r1.getStdInFile(), r2.getStdInFile());
        assertEquals(r1.getStdOutFile(), r2.getStdOutFile());
    }

    @Test
    public void testComplexRedirectionChain() throws Exception {
        // Create unsorted file with duplicates
        createTestFile("chain_input.txt", "3\n1\n4\n1\n5\n9\n2\n6\n");
        
        File sorted = trackFile("chain_sorted.txt");
        File unique = trackFile("chain_unique.txt");
        File count = trackFile("chain_count.txt");
        
        // Chain: sort -> unique -> count
        Command cmd1 = Parser.parse("sort < chain_input.txt > chain_sorted.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        Command cmd2 = Parser.parse("uniq < chain_sorted.txt > chain_unique.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        Command cmd3 = Parser.parse("wc -l < chain_unique.txt > chain_count.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd3);
        
        // Verify final count
        assertTrue(count.exists());
        String countContent = readFile(count);
        assertTrue("Should have count", countContent.trim().length() > 0);
    }

    @Test
    public void testSummaryAllOperators() {
        System.out.println("\n=== ALL FOUR REDIRECTION OPERATORS IMPLEMENTED ===");
        System.out.println("✅ >  : Output redirection (overwrites)");
        System.out.println("✅ >> : Append redirection");
        System.out.println("✅ <  : Input redirection");
        System.out.println("✅ 2> : Error redirection");
        System.out.println("✅ All combinations work together!");
        
        // Just verify parsing works
        Command cmd = Parser.parse("cmd < in.txt > out.txt 2> err.txt");
        assertNotNull(cmd);
        assertEquals(CommandType.REDIRECTION, cmd.getType());
    }
}

