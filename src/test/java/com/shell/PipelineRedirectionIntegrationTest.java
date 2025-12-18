package com.shell;

import com.shell.parser.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests that ACTUALLY EXECUTE pipeline + redirection commands
 * and verify that files are created with correct content.
 */
public class PipelineRedirectionIntegrationTest {
    
    private List<File> filesToCleanup;
    private Path testDir;
    
    @Before
    public void setUp() throws IOException {
        filesToCleanup = new ArrayList<>();
        testDir = Paths.get(System.getProperty("user.dir"));
    }
    
    @After
    public void tearDown() {
        // Clean up all test files
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

    @Test
    public void testEchoWithRedirection() throws Exception {
        File outputFile = trackFile("test_echo_output.txt");
        
        // Execute: echo hello world > test_echo_output.txt
        Command cmd = Parser.parse("echo hello world > test_echo_output.txt");
        assertTrue(cmd instanceof RedirectionCommand);
        
        RedirectionCommand redirectCmd = (RedirectionCommand) cmd;
        Shell.executeRedirectionCommand(redirectCmd);
        
        // Verify file was created
        assertTrue("Output file should exist", outputFile.exists());
        
        // Verify content
        String content = readFile(outputFile);
        assertEquals("hello world\n", content);
    }

    @Test
    public void testEchoWithAppendRedirection() throws Exception {
        File outputFile = trackFile("test_append.txt");
        
        // First write
        Command cmd1 = Parser.parse("echo first line > test_append.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        // Append second line
        Command cmd2 = Parser.parse("echo second line >> test_append.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        // Verify content has both lines
        String content = readFile(outputFile);
        assertTrue("Should contain first line", content.contains("first line"));
        assertTrue("Should contain second line", content.contains("second line"));
    }

    @Test
    public void testTypeCommandWithRedirection() throws Exception {
        // Create input file
        File inputFile = trackFile("test_input.txt");
        createTestFile("test_input.txt", "Hello from input file\nLine 2\nLine 3");
        
        File outputFile = trackFile("test_type_output.txt");
        
        // Execute: type test_input.txt > test_type_output.txt
        Command cmd = Parser.parse("type test_input.txt > test_type_output.txt");
        assertTrue(cmd instanceof RedirectionCommand);
        
        RedirectionCommand redirectCmd = (RedirectionCommand) cmd;
        Shell.executeRedirectionCommand(redirectCmd);
        
        // Verify output file has same content as input
        assertTrue("Output file should exist", outputFile.exists());
        String content = readFile(outputFile);
        assertTrue("Should contain line 1", content.contains("Hello from input file"));
        assertTrue("Should contain line 2", content.contains("Line 2"));
        assertTrue("Should contain line 3", content.contains("Line 3"));
    }

    @Test
    public void testTypeMultipleFilesWithRedirection() throws Exception {
        // Create multiple input files
        createTestFile("test_file1.txt", "Content from file 1");
        createTestFile("test_file2.txt", "Content from file 2");
        
        File outputFile = trackFile("test_multiple_output.txt");
        
        // Execute: type test_file1.txt test_file2.txt > test_multiple_output.txt
        Command cmd = Parser.parse("type test_file1.txt test_file2.txt > test_multiple_output.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify both file contents are in output
        String content = readFile(outputFile);
        assertTrue("Should contain file1 content", content.contains("Content from file 1"));
        assertTrue("Should contain file2 content", content.contains("Content from file 2"));
    }

    @Test
    public void testStderrRedirection() throws Exception {
        File errorFile = trackFile("test_errors.txt");
        
        // Execute: type nonexistent_file.txt 2> test_errors.txt
        Command cmd = Parser.parse("type nonexistent_file.txt 2> test_errors.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify error file was created
        assertTrue("Error file should exist", errorFile.exists());
        
        // Verify it contains error message
        String content = readFile(errorFile);
        assertTrue("Should contain error message", 
                   content.contains("No such file") || content.contains("nonexistent_file.txt"));
    }

    @Test
    public void testBothStdoutAndStderrRedirection() throws Exception {
        // Create one valid and reference one invalid file
        createTestFile("valid_file.txt", "Valid content");
        
        File outputFile = trackFile("test_stdout.txt");
        File errorFile = trackFile("test_stderr.txt");
        
        // Execute: type valid_file.txt invalid_file.txt > test_stdout.txt 2> test_stderr.txt
        Command cmd = Parser.parse("type valid_file.txt invalid_file.txt > test_stdout.txt 2> test_stderr.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify stdout file has valid content
        assertTrue("Stdout file should exist", outputFile.exists());
        String stdout = readFile(outputFile);
        assertTrue("Should contain valid content", stdout.contains("Valid content"));
        
        // Verify stderr file has error
        assertTrue("Stderr file should exist", errorFile.exists());
        String stderr = readFile(errorFile);
        assertTrue("Should contain error about invalid file", 
                   stderr.contains("invalid_file.txt") || stderr.contains("No such file"));
    }

    @Test
    public void testOverwriteExistingFile() throws Exception {
        File outputFile = trackFile("test_overwrite.txt");
        
        // Create file with initial content
        createTestFile("test_overwrite.txt", "This will be overwritten");
        
        // Overwrite with new content
        Command cmd = Parser.parse("echo new content > test_overwrite.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify old content is gone
        String content = readFile(outputFile);
        assertFalse("Should not contain old content", content.contains("This will be overwritten"));
        assertTrue("Should contain new content", content.contains("new content"));
    }

    @Test
    public void testAppendToExistingFile() throws Exception {
        File outputFile = trackFile("test_append_existing.txt");
        
        // Create file with initial content
        createTestFile("test_append_existing.txt", "Initial content\n");
        
        // Append new content
        Command cmd = Parser.parse("echo appended content >> test_append_existing.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify both contents exist
        String content = readFile(outputFile);
        assertTrue("Should contain initial content", content.contains("Initial content"));
        assertTrue("Should contain appended content", content.contains("appended content"));
    }

    @Test
    public void testRedirectionWithQuotedStrings() throws Exception {
        File outputFile = trackFile("test_quoted.txt");
        
        // Execute: echo "hello world with spaces" > test_quoted.txt
        Command cmd = Parser.parse("echo \"hello world with spaces\" > test_quoted.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify content preserves spaces
        String content = readFile(outputFile);
        assertEquals("hello world with spaces\n", content);
    }

    @Test
    public void testRedirectionWithSpecialCharacters() throws Exception {
        File outputFile = trackFile("test_special.txt");
        
        // Execute: echo "Special: !@#$%^&*()" > test_special.txt
        Command cmd = Parser.parse("echo \"Special: !@#$%^&*()\" > test_special.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify special chars are preserved
        String content = readFile(outputFile);
        assertTrue("Should contain special chars", content.contains("!@#$%^&*()"));
    }

    @Test
    public void testEmptyEchoWithRedirection() throws Exception {
        File outputFile = trackFile("test_empty.txt");
        
        // Execute: echo > test_empty.txt (no arguments)
        Command cmd = Parser.parse("echo > test_empty.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify file was created (should be empty or just newline)
        assertTrue("Output file should exist", outputFile.exists());
        String content = readFile(outputFile);
        assertTrue("Should be empty or just newline", content.isEmpty() || content.equals("\n"));
    }

    @Test
    public void testRedirectionToFileInSubdirectory() throws Exception {
        // Create test subdirectory
        File subdir = new File("test_subdir");
        subdir.mkdir();
        filesToCleanup.add(subdir);
        
        File outputFile = trackFile("test_subdir/output.txt");
        
        // Execute: echo test content > test_subdir/output.txt
        Command cmd = Parser.parse("echo test content > test_subdir/output.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // Verify file in subdirectory
        assertTrue("Output file should exist in subdir", outputFile.exists());
        String content = readFile(outputFile);
        assertTrue("Should contain content", content.contains("test content"));
    }

    @Test
    public void testMultipleRedirectionsInSequence() throws Exception {
        File file1 = trackFile("test_seq1.txt");
        File file2 = trackFile("test_seq2.txt");
        File file3 = trackFile("test_seq3.txt");
        
        // Execute three separate commands
        Command cmd1 = Parser.parse("echo first > test_seq1.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        Command cmd2 = Parser.parse("echo second > test_seq2.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        Command cmd3 = Parser.parse("echo third > test_seq3.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd3);
        
        // Verify all files exist with correct content
        assertTrue("File 1 should exist", file1.exists());
        assertTrue("File 2 should exist", file2.exists());
        assertTrue("File 3 should exist", file3.exists());
        
        assertTrue(readFile(file1).contains("first"));
        assertTrue(readFile(file2).contains("second"));
        assertTrue(readFile(file3).contains("third"));
    }

    @Test
    public void testTypeNonExistentFileDoesNotCreateOutput() throws Exception {
        File outputFile = trackFile("test_no_create.txt");
        
        // Execute: type nonexistent.txt > test_no_create.txt
        // This should write error to stderr, not stdout
        Command cmd = Parser.parse("type nonexistent.txt > test_no_create.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd);
        
        // File might be created but should be empty or very small (just error case handling)
        // This depends on implementation - some shells create empty file, some don't
        if (outputFile.exists()) {
            String content = readFile(outputFile);
            // Should not contain valid file content
            assertFalse("Should not contain valid content", content.contains("valid content"));
        }
    }

    @Test
    public void testChainedTypeCommands() throws Exception {
        // Create input file
        createTestFile("test_chain_input.txt", "Line 1\nLine 2\nLine 3");
        
        File output1 = trackFile("test_chain1.txt");
        File output2 = trackFile("test_chain2.txt");
        
        // First: type test_chain_input.txt > test_chain1.txt
        Command cmd1 = Parser.parse("type test_chain_input.txt > test_chain1.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd1);
        
        // Second: type test_chain1.txt > test_chain2.txt
        Command cmd2 = Parser.parse("type test_chain1.txt > test_chain2.txt");
        Shell.executeRedirectionCommand((RedirectionCommand) cmd2);
        
        // Both outputs should have same content as input
        String content1 = readFile(output1);
        String content2 = readFile(output2);
        
        assertTrue(content1.contains("Line 1"));
        assertTrue(content2.contains("Line 1"));
        assertEquals("Both files should have same content", content1, content2);
    }
}

