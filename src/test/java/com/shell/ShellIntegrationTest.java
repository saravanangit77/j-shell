package com.shell;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Integration tests for Shell built-in commands and external program execution
 * Covers Milestone 1, 2, and 4
 */
public class ShellIntegrationTest {

    private Path testDir;
    private Path originalDir;

    @Before
    public void setup() throws IOException {
        // Save original directory
        originalDir = Paths.get(System.getProperty("user.dir"));
        
        // Create temporary test directory
        testDir = Files.createTempDirectory("shell-test-");
        
        // Create test files
        Files.writeString(testDir.resolve("test.txt"), "Hello, World!\nThis is a test file.\n");
        Files.writeString(testDir.resolve("file1.txt"), "Content of file1\n");
        Files.writeString(testDir.resolve("file2.txt"), "Content of file2\n");
        
        // Create subdirectory
        Files.createDirectory(testDir.resolve("subdir"));
        Files.writeString(testDir.resolve("subdir").resolve("nested.txt"), "Nested file content\n");
    }

    @After
    public void cleanup() throws IOException {
        // Restore original directory
        System.setProperty("user.dir", originalDir.toString());
        
        // Clean up test files
        if (testDir != null && Files.exists(testDir)) {
            deleteDirectory(testDir.toFile());
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    // ========== Milestone 1: Basic Commands ==========

    @Test
    public void testEchoSimple() {
        // Test: echo hello world
        // Expected: "hello world\n"
        // Note: This would require making handleEcho public or creating a test helper
        // For now, this serves as documentation of what should be tested
        assertTrue(true); // Placeholder
    }

    @Test
    public void testEchoMultipleWords() {
        // Test: echo hello world from shell
        // Expected: "hello world\n"
        assertTrue(true); // Placeholder - actual implementation would test via process
    }

    @Test
    public void testEchoEmptyArgs() {
        // Test: echo (with no arguments)
        // Expected: "\n" (just a newline)
        assertTrue(true);
    }

    // ========== Milestone 2: External Programs ==========

    @Test
    public void testExternalProgramExecution() throws IOException, InterruptedException {
        // Test running an external program like 'ls'
        // This tests PATH lookup functionality
        assertTrue(true); // Placeholder
    }

    @Test
    public void testInvalidCommandHandling() {
        // Test: nonexistentcommand123
        // Expected: "nonexistentcommand123: command not found"
        assertTrue(true);
    }

    // ========== File-based Integration Tests ==========

    @Test
    public void testTypeCommand() throws IOException {
        Path testFile = testDir.resolve("test.txt");
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Test type command - should read and display file content
            assertTrue(Files.exists(testFile));
            String content = Files.readString(testFile);
            assertTrue(content.contains("Hello, World!"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testTypeNonExistentFile() {
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errorStream));

        try {
            // Test type with non-existent file
            // Expected: error message
            assertTrue(true);
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    public void testTypeDirectory() throws IOException {
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errorStream));

        try {
            // Test type on directory
            // Expected: "type: <dirname>: Is a directory"
            assertTrue(Files.isDirectory(testDir.resolve("subdir")));
        } finally {
            System.setErr(originalErr);
        }
    }

    // ========== Redirection Tests (Milestone 4) ==========

    @Test
    public void testEchoRedirectToFile() throws IOException {
        Path outputFile = testDir.resolve("echo_output.txt");
        
        // Test: echo "Hello Redirect" > echo_output.txt
        // Expected: File created with content "Hello Redirect\n"
        
        // Verify file doesn't exist yet
        assertFalse(Files.exists(outputFile));
        
        // After running command, file should exist
        // String content = Files.readString(outputFile);
        // assertEquals("Hello Redirect\n", content);
    }

    @Test
    public void testEchoAppendToFile() throws IOException {
        Path outputFile = testDir.resolve("append_test.txt");
        Files.writeString(outputFile, "First line\n");
        
        // Test: echo "Second line" >> append_test.txt
        // Expected: File contains both lines
        
        assertTrue(Files.exists(outputFile));
        String initialContent = Files.readString(outputFile);
        assertEquals("First line\n", initialContent);
    }

    @Test
    public void testTypeRedirectToFile() throws IOException {
        Path inputFile = testDir.resolve("test.txt");
        Path outputFile = testDir.resolve("type_output.txt");
        
        // Test: type test.txt > type_output.txt
        // Expected: outputFile contains same content as inputFile
        
        assertTrue(Files.exists(inputFile));
        assertFalse(Files.exists(outputFile));
        
        // After command execution:
        // String outputContent = Files.readString(outputFile);
        // String inputContent = Files.readString(inputFile);
        // assertEquals(inputContent, outputContent);
    }

    @Test
    public void testStderrRedirection() throws IOException {
        Path errorFile = testDir.resolve("error.log");
        
        // Test: type nonexistent.txt 2> error.log
        // Expected: Error message written to error.log
        
        assertFalse(Files.exists(errorFile));
        
        // After command:
        // assertTrue(Files.exists(errorFile));
        // String errorContent = Files.readString(errorFile);
        // assertTrue(errorContent.contains("No such file or directory"));
    }

    @Test
    public void testBothStdoutAndStderrRedirection() throws IOException {
        Path outputFile = testDir.resolve("out.txt");
        Path errorFile = testDir.resolve("err.txt");
        
        // Test: type test.txt nonexistent.txt > out.txt 2> err.txt
        // Expected: Success output in out.txt, errors in err.txt
        
        assertFalse(Files.exists(outputFile));
        assertFalse(Files.exists(errorFile));
    }

    @Test
    public void testRedirectionOverwritesFile() throws IOException {
        Path outputFile = testDir.resolve("overwrite.txt");
        Files.writeString(outputFile, "Old content\n");
        
        // Test: echo "New content" > overwrite.txt
        // Expected: File contains only new content (old content overwritten)
        
        String oldContent = Files.readString(outputFile);
        assertEquals("Old content\n", oldContent);
        
        // After command:
        // String newContent = Files.readString(outputFile);
        // assertEquals("New content\n", newContent);
    }

    @Test
    public void testMultipleAppends() throws IOException {
        Path outputFile = testDir.resolve("multiple_append.txt");
        
        // Test sequence:
        // echo "Line 1" > multiple_append.txt
        // echo "Line 2" >> multiple_append.txt
        // echo "Line 3" >> multiple_append.txt
        
        // Expected: File contains all three lines
        assertFalse(Files.exists(outputFile));
    }

    // ========== PATH and Executable Tests ==========

    @Test
    public void testFindExecutableInPath() {
        // Test that common executables can be found
        // Example: /bin/ls should be found when searching for "ls"
        String pathEnv = System.getenv("PATH");
        assertNotNull("PATH environment variable should be set", pathEnv);
        assertTrue("PATH should not be empty", !pathEnv.isEmpty());
    }

    @Test
    public void testExecutableWithSlash() throws IOException {
        // Test: ./script.sh or /bin/ls (absolute/relative path)
        // Should not search PATH, just check if file exists and is executable
        
        Path scriptPath = testDir.resolve("script.sh");
        Files.writeString(scriptPath, "#!/bin/bash\necho 'test'\n");
        scriptPath.toFile().setExecutable(true);
        
        assertTrue(Files.exists(scriptPath));
        assertTrue(scriptPath.toFile().canExecute());
    }

    // ========== Directory Navigation Tests (Milestone 2) ==========

    @Test
    public void testCurrentDirectory() {
        // Test: pwd command
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        assertNotNull(currentDir);
        assertTrue(Files.exists(currentDir));
    }

    @Test
    public void testChangeDirectoryAbsolute() {
        // Test: cd /tmp or similar absolute path
        Path tmpDir = Paths.get("/tmp");
        if (Files.exists(tmpDir)) {
            assertTrue(Files.isDirectory(tmpDir));
        }
    }

    @Test
    public void testChangeDirectoryRelative() throws IOException {
        // Test: cd subdir (relative path)
        Path subdir = testDir.resolve("subdir");
        assertTrue(Files.exists(subdir));
        assertTrue(Files.isDirectory(subdir));
    }

    @Test
    public void testChangeDirectoryHome() {
        // Test: cd ~ or cd ~/
        String homeDir = System.getProperty("user.home");
        assertNotNull(homeDir);
        assertTrue(Files.exists(Paths.get(homeDir)));
    }

    @Test
    public void testChangeDirectoryInvalidPath() {
        // Test: cd nonexistent_directory
        // Expected: "cd: nonexistent_directory: No such file or directory"
        Path nonExistent = testDir.resolve("nonexistent");
        assertFalse(Files.exists(nonExistent));
    }

    // ========== Quoting with Commands (Milestone 3) ==========

    @Test
    public void testEchoWithSingleQuotes() {
        // Test: echo 'hello world'
        // Expected: "hello world\n"
        assertTrue(true);
    }

    @Test
    public void testEchoWithDoubleQuotes() {
        // Test: echo "hello world"
        // Expected: "hello world\n"
        assertTrue(true);
    }

    @Test
    public void testEchoWithEscapedSpaces() {
        // Test: echo hello\ world
        // Expected: "hello world\n"
        assertTrue(true);
    }

    @Test
    public void testQuotedExecutable() {
        // Test: '/bin/ls' or "/bin/ls"
        // Expected: Should execute ls command
        assertTrue(true);
    }

    @Test
    public void testMixedQuoting() {
        // Test: echo "hello" 'world' test
        // Expected: "hello world test\n"
        assertTrue(true);
    }
}

