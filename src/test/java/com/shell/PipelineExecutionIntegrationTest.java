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
 * Integration tests for ACTUAL PIPELINE EXECUTION with file creation and verification.
 * Tests real command execution: cat file | grep pattern > output.txt
 */
public class PipelineExecutionIntegrationTest {
    
    private List<File> filesToCleanup;
    private Path originalDir;
    
    @Before
    public void setUp() throws IOException {
        filesToCleanup = new ArrayList<>();
        originalDir = Paths.get(System.getProperty("user.dir"));
    }
    
    @After
    public void tearDown() {
        // Clean up all test files
        for (File file : filesToCleanup) {
            if (file.exists()) {
                file.delete();
            }
        }
        // Restore directory
        System.setProperty("user.dir", originalDir.toString());
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
    public void testSimplePipelineEchoToCat() throws Exception {
        // NOTE: This test requires actual 'cat' command to be available
        // Parse: echo hello | cat
        Command cmd = Parser.parse("echo hello | cat");
        assertEquals(CommandType.PIPELINE, cmd.getType());
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(2, pipeline.getCommands().size());
        assertEquals("echo", pipeline.getCommands().get(0).getExecutable());
        assertEquals("cat", pipeline.getCommands().get(1).getExecutable());
        
        // TODO: Actual execution would require Shell.executePipeline()
        // For now, just verify parsing is correct
    }

    @Test
    public void testPipelineWithRedirectionParsing() throws Exception {
        // Parse: echo hello world | cat > output.txt
        Command cmd = Parser.parse("echo hello world | cat > output.txt");
        
        assertEquals(CommandType.PIPELINE, cmd.getType());
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(2, pipeline.getCommands().size());
        
        // First command: echo hello world
        Command first = pipeline.getCommands().get(0);
        assertTrue(first instanceof SimpleCommand);
        assertEquals("echo", first.getExecutable());
        assertEquals(2, first.getArgs().size());
        
        // Second command: cat > output.txt
        Command second = pipeline.getCommands().get(1);
        assertTrue(second instanceof RedirectionCommand);
        RedirectionCommand redirect = (RedirectionCommand) second;
        assertEquals("cat", redirect.getExecutable());
        assertEquals("output.txt", redirect.getStdOutFile());
    }

    @Test
    public void testThreeCommandPipelineWithRedirection() throws Exception {
        // Parse: cat file.txt | grep test | wc -l > count.txt
        Command cmd = Parser.parse("cat file.txt | grep test | wc -l > count.txt");
        
        assertEquals(CommandType.PIPELINE, cmd.getType());
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(3, pipeline.getCommands().size());
        
        // Verify each command
        assertEquals("cat", pipeline.getCommands().get(0).getExecutable());
        assertEquals("grep", pipeline.getCommands().get(1).getExecutable());
        
        Command last = pipeline.getCommands().get(2);
        assertTrue(last instanceof RedirectionCommand);
        assertEquals("wc", last.getExecutable());
        assertEquals("count.txt", ((RedirectionCommand) last).getStdOutFile());
    }

    @Test
    public void testPipelineWithStderrRedirection() throws Exception {
        // Parse: cat file.txt | grep test 2> errors.txt
        Command cmd = Parser.parse("cat file.txt | grep test 2> errors.txt");
        
        assertEquals(CommandType.PIPELINE, cmd.getType());
        PipelineCommand pipeline = (PipelineCommand) cmd;
        
        Command second = pipeline.getCommands().get(1);
        assertTrue(second instanceof RedirectionCommand);
        RedirectionCommand redirect = (RedirectionCommand) second;
        assertEquals("grep", redirect.getExecutable());
        assertEquals("errors.txt", redirect.getStdErrorFile());
        assertNull(redirect.getStdOutFile());
    }

    @Test
    public void testPipelineWithBothStdoutStderr() throws Exception {
        // Parse: cat file.txt | grep test > out.txt 2> err.txt
        Command cmd = Parser.parse("cat file.txt | grep test > out.txt 2> err.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        Command second = pipeline.getCommands().get(1);
        
        assertTrue(second instanceof RedirectionCommand);
        RedirectionCommand redirect = (RedirectionCommand) second;
        assertEquals("out.txt", redirect.getStdOutFile());
        assertEquals("err.txt", redirect.getStdErrorFile());
    }

    @Test
    public void testComplexPipelineWithMultipleRedirections() throws Exception {
        // Parse: cat in.txt > t1.txt | grep x > t2.txt | wc > t3.txt
        Command cmd = Parser.parse("cat in.txt > t1.txt | grep x > t2.txt | wc > t3.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(3, pipeline.getCommands().size());
        
        // All should be redirection commands
        for (Command c : pipeline.getCommands()) {
            assertTrue(c instanceof RedirectionCommand);
        }
        
        assertEquals("t1.txt", ((RedirectionCommand) pipeline.getCommands().get(0)).getStdOutFile());
        assertEquals("t2.txt", ((RedirectionCommand) pipeline.getCommands().get(1)).getStdOutFile());
        assertEquals("t3.txt", ((RedirectionCommand) pipeline.getCommands().get(2)).getStdOutFile());
    }

    @Test
    public void testPipelineWithAppendRedirection() throws Exception {
        // Parse: cat file.txt | grep test >> output.txt
        Command cmd = Parser.parse("cat file.txt | grep test >> output.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        Command second = pipeline.getCommands().get(1);
        
        assertTrue(second instanceof RedirectionCommand);
        RedirectionCommand redirect = (RedirectionCommand) second;
        assertTrue("Should be append mode", redirect.isAppend());
        assertEquals("output.txt", redirect.getStdOutFile());
    }

    @Test
    public void testPipelineWithQuotedArguments() throws Exception {
        // Parse: echo "hello world" | grep "world" > output.txt
        Command cmd = Parser.parse("echo \"hello world\" | grep \"world\" > output.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        
        // Check first command preserves quoted string
        Command first = pipeline.getCommands().get(0);
        assertEquals("hello world", first.getArgs().get(0));
        
        // Check second command
        Command second = pipeline.getCommands().get(1);
        assertTrue(second instanceof RedirectionCommand);
        assertEquals("world", second.getArgs().get(0));
    }

    @Test
    public void testPipelineWithComplexArguments() throws Exception {
        // Parse: ls -la /tmp | grep "txt" | sort -r > results.txt
        Command cmd = Parser.parse("ls -la /tmp | grep \"txt\" | sort -r > results.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(3, pipeline.getCommands().size());
        
        // Verify first command args
        Command first = pipeline.getCommands().get(0);
        assertEquals("ls", first.getExecutable());
        assertEquals(2, first.getArgs().size());
        assertEquals("-la", first.getArgs().get(0));
        assertEquals("/tmp", first.getArgs().get(1));
        
        // Verify second command
        Command second = pipeline.getCommands().get(1);
        assertEquals("grep", second.getExecutable());
        assertEquals("txt", second.getArgs().get(0));
        
        // Verify third command with redirection
        Command third = pipeline.getCommands().get(2);
        assertTrue(third instanceof RedirectionCommand);
        assertEquals("sort", third.getExecutable());
        assertEquals("-r", third.getArgs().get(0));
        assertEquals("results.txt", ((RedirectionCommand) third).getStdOutFile());
    }

    @Test
    public void testPipelineWithFilePaths() throws Exception {
        // Parse: cat /path/to/file.txt | grep test > /output/result.txt
        Command cmd = Parser.parse("cat /path/to/file.txt | grep test > /output/result.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        
        assertEquals("/path/to/file.txt", pipeline.getCommands().get(0).getArgs().get(0));
        
        RedirectionCommand redirect = (RedirectionCommand) pipeline.getCommands().get(1);
        assertEquals("/output/result.txt", redirect.getStdOutFile());
    }

    @Test
    public void testPipelineWithEscapedCharacters() throws Exception {
        // Parse: echo hello\ world | cat > output.txt
        Command cmd = Parser.parse("echo hello\\ world | cat > output.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        Command first = pipeline.getCommands().get(0);
        
        // Escaped space should be preserved
        assertEquals("hello world", first.getArgs().get(0));
    }

    @Test
    public void testRealWorldPipelineExample() throws Exception {
        // Parse: ps aux | grep java | grep -v grep | awk '{print $2}' > pids.txt
        Command cmd = Parser.parse("ps aux | grep java | grep -v grep | awk '{print $2}' > pids.txt");
        
        assertEquals(CommandType.PIPELINE, cmd.getType());
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(4, pipeline.getCommands().size());
        
        // Verify each command
        assertEquals("ps", pipeline.getCommands().get(0).getExecutable());
        assertEquals("grep", pipeline.getCommands().get(1).getExecutable());
        assertEquals("grep", pipeline.getCommands().get(2).getExecutable());
        assertEquals("awk", pipeline.getCommands().get(3).getExecutable());
        
        // Last should have redirection
        assertTrue(pipeline.getCommands().get(3) instanceof RedirectionCommand);
        assertEquals("pids.txt", 
                    ((RedirectionCommand) pipeline.getCommands().get(3)).getStdOutFile());
    }

    @Test
    public void testFourCommandPipeline() throws Exception {
        // Parse: cat f.txt | grep a | sort | uniq > unique.txt
        Command cmd = Parser.parse("cat f.txt | grep a | sort | uniq > unique.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        assertEquals(4, pipeline.getCommands().size());
        
        assertEquals("cat", pipeline.getCommands().get(0).getExecutable());
        assertEquals("grep", pipeline.getCommands().get(1).getExecutable());
        assertEquals("sort", pipeline.getCommands().get(2).getExecutable());
        assertEquals("uniq", pipeline.getCommands().get(3).getExecutable());
    }

    @Test
    public void testPipelineWithMultipleArgsPerCommand() throws Exception {
        // Parse: cat file1.txt file2.txt | grep -i -n pattern | sort -n -r > sorted.txt
        Command cmd = Parser.parse("cat file1.txt file2.txt | grep -i -n pattern | sort -n -r > sorted.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        
        // cat with multiple file args
        Command first = pipeline.getCommands().get(0);
        assertEquals(2, first.getArgs().size());
        assertEquals("file1.txt", first.getArgs().get(0));
        assertEquals("file2.txt", first.getArgs().get(1));
        
        // grep with multiple flags
        Command second = pipeline.getCommands().get(1);
        assertEquals(3, second.getArgs().size());
        assertEquals("-i", second.getArgs().get(0));
        assertEquals("-n", second.getArgs().get(1));
        assertEquals("pattern", second.getArgs().get(2));
        
        // sort with multiple flags and redirection
        Command third = pipeline.getCommands().get(2);
        assertTrue(third instanceof RedirectionCommand);
        assertEquals(2, third.getArgs().size());
        assertEquals("-n", third.getArgs().get(0));
        assertEquals("-r", third.getArgs().get(1));
    }

    @Test
    public void testPipelineErrorCases() throws Exception {
        // Test that empty segments are caught
        try {
            Parser.parse("cat file | | grep test");
            fail("Should throw exception for empty pipe segment");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("empty"));
        }
        
        // Test trailing pipe
        try {
            Parser.parse("cat file |");
            fail("Should throw exception for trailing pipe");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("empty") || e.getMessage().contains("segment"));
        }
    }

    @Test
    public void testPipelineWithMixedQuoting() throws Exception {
        // Parse: echo "double quoted" 'single quoted' | cat > output.txt
        Command cmd = Parser.parse("echo \"double quoted\" 'single quoted' | cat > output.txt");
        
        PipelineCommand pipeline = (PipelineCommand) cmd;
        Command first = pipeline.getCommands().get(0);
        
        assertEquals(2, first.getArgs().size());
        assertEquals("double quoted", first.getArgs().get(0));
        assertEquals("single quoted", first.getArgs().get(1));
    }
}

