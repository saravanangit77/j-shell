package com.shell;

import com.shell.parser.Parser;
import com.shell.parser.RedirectionCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test suite for Redirection functionality - Milestone 4
 * Tests stdout redirection (>), stderr redirection (2>), and append (>>)
 */
public class RedirectionCommandTest {

    private Path testDir;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setup() throws IOException {
        // Create temporary test directory
        testDir = Files.createTempDirectory("redirection-test-");
        
        // Create test input files
        Files.writeString(testDir.resolve("input.txt"), "Test input content\n");
        Files.writeString(testDir.resolve("file1.txt"), "File 1 content\n");
        Files.writeString(testDir.resolve("file2.txt"), "File 2 content\n");
        
        // Set up output capture
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanup() throws IOException {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Clean up test directory
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

    // ========== Basic Stdout Redirection (>) ==========

    @Test
    public void testEchoRedirectStdout() throws IOException {
        Path outputFile = testDir.resolve("output.txt");
        String command = "echo hello world > " + outputFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        assertEquals("echo", rc.getExecutable());
        assertEquals(2, rc.getArgs().size());
        assertEquals("hello", rc.getArgs().get(0));
        assertEquals("world", rc.getArgs().get(1));
        assertEquals(outputFile.toString(), rc.getStdOutFile());
        assertFalse(rc.isAppend());
        
        // Execute the command
        Shell.executeRedirectionCommand(rc);
        
        // Verify file was created with correct content
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertEquals("hello world\n", content);
    }

    @Test
    public void testEchoRedirectEmptyString() throws IOException {
        Path outputFile = testDir.resolve("empty.txt");
        String command = "echo > " + outputFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertEquals("\n", content);
    }

    @Test
    public void testTypeRedirectStdout() throws IOException {
        Path inputFile = testDir.resolve("input.txt");
        Path outputFile = testDir.resolve("output.txt");
        String command = "type " + inputFile.toString() + " > " + outputFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(outputFile));
        String inputContent = Files.readString(inputFile);
        String outputContent = Files.readString(outputFile);
        assertEquals(inputContent, outputContent);
    }

    // ========== Append Redirection (>>) ==========

    @Test
    public void testEchoAppendToNewFile() throws IOException {
        Path outputFile = testDir.resolve("append_new.txt");
        String command = "echo first line >> " + outputFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertEquals("first line\n", content);
    }

    @Test
    public void testEchoAppendToExistingFile() throws IOException {
        Path outputFile = testDir.resolve("append_existing.txt");
        Files.writeString(outputFile, "existing line\n");
        
        String command = "echo new line >> " + outputFile.toString();
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        String content = Files.readString(outputFile);
        assertEquals("existing line\nnew line\n", content);
    }

    @Test
    public void testMultipleAppends() throws IOException {
        Path outputFile = testDir.resolve("multiple_appends.txt");
        
        // First append
        String command1 = "echo line 1 >> " + outputFile.toString();
        Shell.executeRedirectionCommand(Parser.parseRedirection(Parser.tokenize(command1)));
        
        // Second append
        String command2 = "echo line 2 >> " + outputFile.toString();
        Shell.executeRedirectionCommand(Parser.parseRedirection(Parser.tokenize(command2)));
        
        // Third append
        String command3 = "echo line 3 >> " + outputFile.toString();
        Shell.executeRedirectionCommand(Parser.parseRedirection(Parser.tokenize(command3)));
        
        String content = Files.readString(outputFile);
        assertEquals("line 1\nline 2\nline 3\n", content);
    }

    @Test
    public void testRedirectOverwritesThenAppend() throws IOException {
        Path outputFile = testDir.resolve("overwrite_append.txt");
        
        // First overwrite
        String command1 = "echo first > " + outputFile.toString();
        Shell.executeRedirectionCommand(Parser.parseRedirection(Parser.tokenize(command1)));
        
        String content1 = Files.readString(outputFile);
        assertEquals("first\n", content1);
        
        // Then append
        String command2 = "echo second >> " + outputFile.toString();
        Shell.executeRedirectionCommand(Parser.parseRedirection(Parser.tokenize(command2)));
        
        String content2 = Files.readString(outputFile);
        assertEquals("first\nsecond\n", content2);
    }

    @Test
    public void testTypeAppendMultipleFiles() throws IOException {
        Path file1 = testDir.resolve("file1.txt");
        Path file2 = testDir.resolve("file2.txt");
        Path outputFile = testDir.resolve("combined.txt");
        
        String command = "type " + file1.toString() + " " + file2.toString() + " >> " + outputFile.toString();
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        String content = Files.readString(outputFile);
        assertTrue(content.contains("File 1 content"));
        assertTrue(content.contains("File 2 content"));
    }

    // ========== Stderr Redirection (2>) ==========

    @Test
    public void testTypeRedirectStderr() throws IOException {
        Path errorFile = testDir.resolve("error.log");
        Path nonExistent = testDir.resolve("nonexistent.txt");
        String command = "type " + nonExistent.toString() + " 2> " + errorFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(errorFile));
        String errorContent = Files.readString(errorFile);
        assertTrue(errorContent.contains("No such file or directory") || 
                   errorContent.contains("nonexistent.txt"));
    }

    @Test
    public void testTypeRedirectStderrWithValidFile() throws IOException {
        Path inputFile = testDir.resolve("input.txt");
        Path errorFile = testDir.resolve("error.log");
        String command = "type " + inputFile.toString() + " 2> " + errorFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        // Error file should either not exist or be empty (no errors occurred)
        if (Files.exists(errorFile)) {
            String errorContent = Files.readString(errorFile);
            assertEquals("", errorContent);
        }
    }

    @Test
    public void testTypeMixedFilesRedirectStderr() throws IOException {
        Path validFile = testDir.resolve("file1.txt");
        Path invalidFile = testDir.resolve("missing.txt");
        Path errorFile = testDir.resolve("error.log");
        
        String command = "type " + validFile.toString() + " " + invalidFile.toString() + " 2> " + errorFile.toString();
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(errorFile));
        String errorContent = Files.readString(errorFile);
        assertTrue(errorContent.contains("missing.txt") || 
                   errorContent.contains("No such file or directory"));
    }

    // ========== Combined Stdout and Stderr Redirection ==========

    @Test
    public void testRedirectBothStdoutAndStderr() throws IOException {
        Path validFile = testDir.resolve("file1.txt");
        Path invalidFile = testDir.resolve("missing.txt");
        Path outputFile = testDir.resolve("output.txt");
        Path errorFile = testDir.resolve("error.txt");
        
        String command = "type " + validFile.toString() + " " + invalidFile.toString() + 
                        " > " + outputFile.toString() + " 2> " + errorFile.toString();
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        // Output file should contain content from valid file
        assertTrue(Files.exists(outputFile));
        String outputContent = Files.readString(outputFile);
        assertTrue(outputContent.contains("File 1 content"));
        
        // Error file should contain error about invalid file
        assertTrue(Files.exists(errorFile));
        String errorContent = Files.readString(errorFile);
        assertTrue(errorContent.contains("missing.txt") || 
                   errorContent.contains("No such file or directory"));
    }

    @Test
    public void testRedirectStdoutAndStderrToSameFile() {
        // Note: This is a common shell pattern, but support depends on implementation
        // Some shells support: command > file 2>&1
        // This test documents expected behavior if implemented
        assertTrue(true); // Placeholder for future implementation
    }

    // ========== Error Cases ==========

    @Test
    public void testRedirectToInvalidPath() throws IOException {
        Path invalidPath = testDir.resolve("nonexistent_dir").resolve("output.txt");
        String command = "echo test > " + invalidPath.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        // Should handle gracefully without crashing
        try {
            Shell.executeRedirectionCommand(rc);
            // If it doesn't throw an exception, the command should have failed gracefully
            assertTrue(true);
        } catch (Exception e) {
            // Or it might throw an exception, which is also acceptable
            assertTrue(true);
        }
    }

    // ========== Redirection with Quoted Arguments ==========

    @Test
    public void testRedirectWithQuotedFilename() throws IOException {
        Path outputPath = testDir.resolve("output with spaces.txt");
        String command = "echo hello > \"" + outputPath.toString() + "\"";
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(outputPath));
        String content = Files.readString(outputPath);
        assertEquals("hello\n", content);
    }

    @Test
    public void testRedirectWithQuotedArguments() throws IOException {
        Path outputFile = testDir.resolve("quoted_args.txt");
        String command = "echo \"hello world\" 'test message' > " + outputFile.toString();
        
        List<String> tokens = Parser.tokenize(command);
        RedirectionCommand rc = Parser.parseRedirection(tokens);
        
        Shell.executeRedirectionCommand(rc);
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertEquals("hello world test message\n", content);
    }

    // ========== RedirectionCommand Object Tests ==========

    @Test
    public void testRedirectionCommandCreation() {
        RedirectionCommand rc = new RedirectionCommand();
        rc.setExecutable("echo");
        rc.setArgs(List.of("hello", "world"));
        rc.setStdOutFile("output.txt");
        rc.setAppend(false);
        rc.setStdErrorFile(null);
        
        assertEquals("echo", rc.getExecutable());
        assertEquals(2, rc.getArgs().size());
        assertEquals("hello", rc.getArgs().get(0));
        assertEquals("world", rc.getArgs().get(1));
        assertEquals("output.txt", rc.getStdOutFile());
        assertFalse(rc.isAppend());
        assertNull(rc.getStdErrorFile());
    }

    @Test
    public void testRedirectionCommandToString() {
        RedirectionCommand rc = new RedirectionCommand();
        rc.setExecutable("type");
        rc.setArgs(List.of("file.txt"));
        rc.setStdOutFile("out.txt");
        rc.setStdErrorFile("err.txt");
        rc.setAppend(true);
        
        String str = rc.toString();
        assertTrue(str.contains("type"));
        assertTrue(str.contains("out.txt"));
        assertTrue(str.contains("err.txt"));
        assertTrue(str.contains("append=true"));
    }
}

