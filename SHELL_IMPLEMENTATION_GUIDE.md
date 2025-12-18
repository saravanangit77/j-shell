# 📚 Build Your Own Shell (j-shell) - Complete Guide

## 🎓 Learning Objectives

By the end of this guide, you will understand:
- How a shell parses and executes commands
- Stream redirection (stdin, stdout, stderr)
- Pipeline implementation
- Command-line parsing with quotes and escaping
- Process management in Java
- Object-oriented design patterns for parsers

---

# Part 1: Understanding Shells

## 🤔 Question 1: What is a Shell?

A shell is a **command-line interpreter** that:
1. Reads user input
2. Parses the command
3. Executes programs
4. Displays output

### Examples:
- `bash`, `zsh`, `sh` (Unix/Linux)
- `cmd.exe`, `PowerShell` (Windows)
- Your custom `j-shell` (Java)

**Q: What are the three main responsibilities of a shell?**
<details>
<summary>Answer</summary>

1. **Parse** - Understand what the user wants
2. **Execute** - Run the appropriate program
3. **Display** - Show results back to user

</details>

---

## 🧩 Basic Shell Loop

```java
while (true) {
    // 1. Print prompt
    System.out.print("my-shell$ ");
    
    // 2. Read input
    String line = reader.readLine();
    
    // 3. Parse command
    Command cmd = Parser.parse(line);
    
    // 4. Execute
    execute(cmd);
}
```

**Q: What happens if readLine() returns null?**
<details>
<summary>Answer</summary>

`null` means EOF (End of File), typically when user presses Ctrl+D. The shell should exit gracefully.

```java
if (line == null) {
    System.out.println();
    break; // Exit the shell
}
```

</details>

---

# Part 2: Command Parsing

## 🔍 Tokenization

**Challenge:** Split this command into tokens:
```bash
echo "hello world" > output.txt
```

**Naive Approach (WRONG):**
```java
String[] tokens = line.split(" ");
// Result: ["echo", "\"hello", "world\"", ">", "output.txt"]
// ❌ Wrong! "hello world" should be ONE token
```

**Q: Why does naive splitting fail?**
<details>
<summary>Answer</summary>

It doesn't respect:
1. **Quotes** - Keep quoted strings together
2. **Escaping** - Handle backslash escapes
3. **Operators** - Separate `>`, `>>`, `<`, `|`, `2>`

</details>

---

## ✅ Proper Tokenization

```java
public static List<String> tokenize(String input) {
    List<String> tokens = new ArrayList<>();
    StringBuilder token = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    
    for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        
        // Handle escaping
        if (c == '\\' && !inSingleQuote) {
            i++; // Skip backslash, add next char
            token.append(input.charAt(i));
            continue;
        }
        
        // Toggle quotes
        if (c == '\'' && !inDoubleQuote) {
            inSingleQuote = !inSingleQuote;
            continue; // Don't include quote char
        }
        
        if (c == '"' && !inSingleQuote) {
            inDoubleQuote = !inDoubleQuote;
            continue;
        }
        
        // Split on whitespace (but not inside quotes)
        if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
            if (token.length() > 0) {
                tokens.add(token.toString());
                token.setLength(0);
            }
            continue;
        }
        
        token.append(c);
    }
    
    if (token.length() > 0) {
        tokens.add(token.toString());
    }
    
    return tokens;
}
```

**Q: What should this tokenize to?**
```bash
echo hello\ world "test 123" 'single quotes'
```

<details>
<summary>Answer</summary>

```
["echo", "hello world", "test 123", "single quotes"]
```

- `hello\ world` → `hello world` (escaped space)
- `"test 123"` → `test 123` (double quotes removed, space preserved)
- `'single quotes'` → `single quotes` (single quotes removed)

</details>

---

## 🎯 Command Types

Our shell supports different command types:

```
Command (abstract)
├── SimpleCommand       - echo hello
├── RedirectionCommand  - echo hello > file.txt
└── PipelineCommand     - cat file | grep test
```

**Q: What type is this command?**
```bash
cat file.txt | grep test > output.txt
```

<details>
<summary>Answer</summary>

**PipelineCommand** containing:
- SimpleCommand: `cat file.txt`
- RedirectionCommand: `grep test > output.txt`

The pipeline contains two commands, where the second has redirection.

</details>

---

# Part 3: I/O Redirection

## 📊 Understanding File Descriptors

Every process has three standard streams:

| FD | Name | Symbol | Purpose |
|----|------|--------|---------|
| 0 | stdin | `<` | Input |
| 1 | stdout | `>`, `>>` | Output |
| 2 | stderr | `2>` | Errors |

```
     ┌─────────┐
< ──→│ Process │──→ >
     │         │──→ 2>
     └─────────┘
```

---

## 🔄 Redirection Operators

### 1. `>` Output Redirection (Overwrite)

```bash
echo hello > file.txt
```

**What happens:**
1. Shell opens `file.txt` for writing (creates if doesn't exist)
2. Redirects stdout (FD 1) to file
3. Executes `echo hello`
4. Output goes to file instead of terminal

**Q: Run this twice. What's in the file?**
```bash
echo first > file.txt
echo second > file.txt
```

<details>
<summary>Answer</summary>

```
second
```

The `>` operator **overwrites** the file each time.

</details>

---

### 2. `>>` Append Redirection

```bash
echo hello >> file.txt
```

**Difference from `>`:**
- `>` = Open in write mode (truncate existing content)
- `>>` = Open in append mode (keep existing content)

**Q: What's in the file after these commands?**
```bash
echo line1 > file.txt
echo line2 >> file.txt
echo line3 >> file.txt
```

<details>
<summary>Answer</summary>

```
line1
line2
line3
```

First command overwrites, next two append.

</details>

---

### 3. `<` Input Redirection

```bash
wc -l < file.txt
```

**What happens:**
1. Shell opens `file.txt` for reading
2. Redirects stdin (FD 0) to file
3. Executes `wc -l`
4. `wc` reads from file instead of keyboard

**Java Implementation:**
```java
if (rc.getStdInFile() != null) {
    pb.redirectInput(new File(rc.getStdInFile()));
}
```

**Q: What's the difference?**
```bash
wc -l file.txt      # A
wc -l < file.txt    # B
```

<details>
<summary>Answer</summary>

**A:** `wc` opens the file itself and shows filename in output:
```
5 file.txt
```

**B:** Shell redirects file to stdin, `wc` doesn't know filename:
```
5
```

Both count lines, but output format differs.

</details>

---

### 4. `2>` Error Redirection

```bash
grep pattern file.txt 2> errors.txt
```

**What happens:**
- Normal output (stdout) goes to terminal
- Error messages (stderr) go to `errors.txt`

**Q: What goes where?**
```bash
cat valid.txt invalid.txt > output.txt 2> errors.txt
```

<details>
<summary>Answer</summary>

**output.txt:** Content of `valid.txt`  
**errors.txt:** Error message about `invalid.txt`  
**Terminal:** Nothing (both streams redirected)

</details>

---

## 🔀 Combining Redirections

**All four together:**
```bash
grep pattern < input.txt > output.txt 2> errors.txt
```

**Data flow:**
```
input.txt → [grep] → output.txt
              ↓
          errors.txt
```

**Q: Does the order matter?**
```bash
grep pattern > out.txt < in.txt     # A
grep pattern < in.txt > out.txt     # B
```

<details>
<summary>Answer</summary>

**No!** Both work the same. Shell processes all redirections before executing the command. Order doesn't matter.

</details>

---

# Part 4: Pipelines

## 🔗 What is a Pipeline?

A pipeline connects stdout of one command to stdin of the next:

```bash
cat file.txt | grep test | wc -l
```

**Data flow:**
```
cat → grep → wc → terminal
```

**Conceptual:**
```
cat file.txt produces: line1\nline2\ntest\nline3\n
                          ↓
grep test filters to:   test\n
                          ↓
wc -l counts:          1
```

---

## 🛠️ Pipeline Implementation

```java
private static void executePipeline(List<Command> commands) {
    List<Process> processes = new ArrayList<>();
    InputStream prevOut = null;
    
    for (int i = 0; i < commands.size(); i++) {
        Command cmd = commands.get(i);
        ProcessBuilder pb = new ProcessBuilder(getCommandList(cmd));
        
        // Start process
        Process process = pb.start();
        processes.add(process);
        
        // Connect previous output to this input
        if (prevOut != null) {
            connectStreams(prevOut, process.getOutputStream());
        }
        
        // Save output for next command
        prevOut = process.getInputStream();
    }
    
    // Final output to terminal
    prevOut.transferTo(System.out);
    
    // Wait for all processes
    for (Process p : processes) {
        p.waitFor();
    }
}
```

**Q: Why do we need threads when connecting streams?**
```java
new Thread(() -> {
    src.transferTo(dest);
    dest.close();
}).start();
```

<details>
<summary>Answer</summary>

**To prevent deadlocks!**

If we do it synchronously:
1. Process A waits to write output (buffer full)
2. Process B waits to read input (but we're stuck writing)
3. **Deadlock** - both wait forever

With threads:
- Writing happens asynchronously
- Both processes can run simultaneously
- Data flows smoothly

</details>

---

## 🧩 Pipeline + Redirection

```bash
cat < input.txt | grep test | wc -l > count.txt
```

**Challenge:** Where do redirections apply?

<details>
<summary>Answer</summary>

- `< input.txt` applies to **first** command (`cat`)
- `> count.txt` applies to **last** command (`wc`)
- `|` connects **between** commands

```
input.txt → cat → grep → wc → count.txt
```

Each command in the pipeline can have its own redirections!

</details>

---

## 🐛 Common Pipeline Bug: Input Redirection

**Problem:** This hangs!
```bash
cat < file.txt | wc -l
```

**Why?**
```java
// OLD CODE (WRONG)
List<String> cmd = ["cat"];  // Lost the input redirection!
ProcessBuilder pb = new ProcessBuilder(cmd);
Process p = pb.start();      // cat waits for stdin forever!
```

**Fix:**
```java
// NEW CODE (CORRECT)
if (cmd instanceof RedirectionCommand) {
    RedirectionCommand rc = (RedirectionCommand) cmd;
    if (rc.getStdInFile() != null) {
        pb.redirectInput(new File(rc.getStdInFile()));
    }
}
```

**Q: Why does `cat file.txt | wc -l` work without hanging?**

<details>
<summary>Answer</summary>

Because `cat file.txt` has the filename as an **argument**, not stdin redirection.

- `cat file.txt` - Opens file itself, no stdin needed
- `cat < file.txt` - Expects stdin, must be redirected

When there's no stdin and no redirection, we must close stdin:
```java
process.getOutputStream().close(); // Tell cat: no input coming!
```

</details>

---

# Part 5: Object-Oriented Design

## 🏗️ Command Pattern

**Before (Procedural):**
```java
if (line.contains("|")) {
    // Pipeline handling
} else if (line.contains(">")) {
    // Redirection handling
} else {
    // Simple command handling
}
```

**Problems:**
- ❌ String checking is fragile
- ❌ Hard to test
- ❌ Difficult to extend
- ❌ Mixed concerns

---

**After (Object-Oriented):**
```java
Command command = Parser.parse(line);

switch (command.getType()) {
    case SIMPLE:
        handleSimpleCommand((SimpleCommand) command);
        break;
    case REDIRECTION:
        handleRedirectionCommand((RedirectionCommand) command);
        break;
    case PIPELINE:
        handlePipelineCommand((PipelineCommand) command);
        break;
}
```

**Benefits:**
- ✅ Type-safe
- ✅ Easy to test
- ✅ Simple to extend
- ✅ Clear separation

---

## 🎨 Class Design

```java
// Base class
abstract class Command {
    protected String executable;
    protected List<String> args;
    
    public abstract CommandType getType();
}

// Concrete classes
class SimpleCommand extends Command {
    @Override
    public CommandType getType() {
        return CommandType.SIMPLE;
    }
}

class RedirectionCommand extends Command {
    private String stdInFile;   // <
    private String stdOutFile;  // >
    private String stdErrorFile; // 2>
    private boolean append;      // >>
    
    @Override
    public CommandType getType() {
        return CommandType.REDIRECTION;
    }
}

class PipelineCommand extends Command {
    private List<Command> commands;
    
    @Override
    public CommandType getType() {
        return CommandType.PIPELINE;
    }
}
```

**Q: Why use inheritance here?**

<details>
<summary>Answer</summary>

**Benefits:**
1. **Polymorphism** - Handle all commands uniformly
2. **Type Safety** - Compiler checks command types
3. **Extensibility** - Easy to add new types (e.g., `BackgroundCommand`)
4. **Code Reuse** - Common fields in base class

**Alternative:** Could use composition, but inheritance makes sense here because commands share behavior and identity.

</details>

---

## 🔍 Parser Design

```java
public class Parser {
    // Main entry point
    public static Command parse(String input) {
        if (input.contains("|")) {
            return parsePipeline(input);
        }
        
        List<String> tokens = tokenize(input);
        
        if (containsRedirectionOperators(tokens)) {
            return parseRedirection(tokens);
        }
        
        return parseSimple(tokens);
    }
    
    private static PipelineCommand parsePipeline(String input) {
        String[] segments = input.split("\\|", -1); // -1 to keep trailing empty
        List<Command> commands = new ArrayList<>();
        
        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("empty command segment");
            }
            
            List<String> tokens = tokenize(segment);
            
            if (containsRedirectionOperators(tokens)) {
                commands.add(parseRedirection(tokens));
            } else {
                commands.add(parseSimple(tokens));
            }
        }
        
        return new PipelineCommand(commands);
    }
    
    private static RedirectionCommand parseRedirection(List<String> tokens) {
        RedirectionCommand rc = new RedirectionCommand();
        rc.setExecutable(tokens.get(0));
        
        String pendingOperator = null;
        List<String> args = new ArrayList<>();
        
        for (int i = 1; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            
            if (pendingOperator != null) {
                // This token is a filename for the operator
                switch (pendingOperator) {
                    case "<":  rc.setStdInFile(tok); break;
                    case ">":  rc.setStdOutFile(tok); rc.setAppend(false); break;
                    case ">>": rc.setStdOutFile(tok); rc.setAppend(true); break;
                    case "2>": rc.setStdErrorFile(tok); break;
                }
                pendingOperator = null;
            } else if (isOperator(tok)) {
                pendingOperator = tok;
            } else {
                args.add(tok);
            }
        }
        
        rc.setArgs(args);
        return rc;
    }
}
```

**Q: Why do we use `split("\\|", -1)` instead of `split("\\|")`?**

<details>
<summary>Answer</summary>

**Without -1:**
```java
"cat file |".split("\\|")  → ["cat file "]
```
Trailing empty string is discarded!

**With -1:**
```java
"cat file |".split("\\|", -1)  → ["cat file ", ""]
```
Trailing empty string is preserved, so we can detect the error.

This helps catch invalid syntax like:
- `cat file |` (trailing pipe)
- `cat file | |` (empty segment)

</details>

---

# Part 6: Process Management

## 🚀 Executing External Commands

```java
private static int executeExternal(String[] argv, Path workingDir) 
        throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(argv);
    pb.directory(workingDir.toFile());
    pb.inheritIO(); // Use parent's stdin/stdout/stderr
    
    Process p = pb.start();
    return p.waitFor();
}
```

**Key Methods:**
- `pb.start()` - Start the process
- `p.waitFor()` - Wait for completion
- `pb.inheritIO()` - Share terminal with parent
- `pb.redirectInput/Output/Error()` - Redirect streams

**Q: What's the difference?**
```java
pb.inheritIO();              // A
pb.redirectOutput(file);     // B
```

<details>
<summary>Answer</summary>

**A:** Process shares the shell's terminal
- Output goes to user's screen
- User can see results immediately

**B:** Process output goes to file
- Nothing shown on screen
- Must read file to see results

Use A for interactive commands, B for redirection.

</details>

---

## ⚠️ Common Pitfalls

### 1. Forgetting to close streams
```java
// BAD
process.getOutputStream(); // Opens but never closes!

// GOOD
try (OutputStream out = process.getOutputStream()) {
    // Automatically closes
}
```

### 2. Not waiting for process
```java
// BAD
Process p = pb.start();
// Returns immediately, process still running!

// GOOD
Process p = pb.start();
p.waitFor(); // Wait until done
```

### 3. Deadlock in pipelines
```java
// BAD
prevOut.transferTo(process.getOutputStream()); // Blocks!
process.waitFor(); // Never reached!

// GOOD
new Thread(() -> {
    prevOut.transferTo(process.getOutputStream());
}).start();
process.waitFor(); // Works!
```

---

# Part 7: Testing

## 🧪 Test Strategy

### 1. Unit Tests (Parser)
```java
@Test
public void testParseSimpleCommand() {
    Command cmd = Parser.parse("echo hello");
    
    assertEquals(CommandType.SIMPLE, cmd.getType());
    SimpleCommand simple = (SimpleCommand) cmd;
    assertEquals("echo", simple.getExecutable());
    assertEquals(Arrays.asList("hello"), simple.getArgs());
}
```

### 2. Integration Tests (File I/O)
```java
@Test
public void testOutputRedirection() throws Exception {
    File output = new File("test_output.txt");
    
    Command cmd = Parser.parse("echo hello > test_output.txt");
    Shell.executeRedirectionCommand((RedirectionCommand) cmd);
    
    assertTrue(output.exists());
    String content = readFile(output);
    assertEquals("hello\n", content);
    
    output.delete(); // Cleanup
}
```

### 3. End-to-End Tests
```java
@Test
public void testPipelineWithRedirection() throws Exception {
    // Create input file
    createFile("input.txt", "apple\nbanana\napricot\n");
    
    // Execute: cat < input.txt | grep ^a | wc -l > count.txt
    String cmd = "cat < input.txt | grep ^a | wc -l > count.txt";
    Shell.executeCommand(cmd);
    
    // Verify
    String count = readFile("count.txt");
    assertEquals("2", count.trim()); // apple, apricot
    
    // Cleanup
    deleteFiles("input.txt", "count.txt");
}
```

**Q: What are you testing in each layer?**

<details>
<summary>Answer</summary>

1. **Unit Tests** - Parser logic only (no execution)
   - Does it extract the right information?
   - Does it handle edge cases?

2. **Integration Tests** - Execution with real files
   - Are files actually created?
   - Is content correct?
   - Do redirections work?

3. **End-to-End Tests** - Complete workflows
   - Do complex commands work?
   - Does data flow correctly?
   - Does cleanup work?

</details>

---

## 📊 Test Coverage

**Our shell has 247 tests covering:**
- ✅ 47 parser tests
- ✅ 31 integration tests
- ✅ 16 pipeline+redirection combination tests
- ✅ 16 comprehensive redirection tests
- ✅ 137 existing feature tests

**100% passing!** ✅

---

# Part 8: Advanced Topics

## 🎯 Mental Model: Data Flow

For command:
```bash
A < in.txt | B | C > out.txt 2> err.txt
```

Think of it as:
```
in.txt → A → B → C → out.txt
                  ↓
              err.txt
```

**Key insights:**
1. `<` applies to **first** command
2. `>` applies to **last** command
3. `|` connects commands **in between**
4. `2>` is **separate** from stdout

**Q: What if B also has redirection?**
```bash
A < in.txt | B > temp.txt | C > out.txt
```

<details>
<summary>Answer</summary>

```
in.txt → A → B → temp.txt
         ↓
         (B's stdout doesn't flow to C!)
         
         ∅ → C → out.txt
         (C gets empty input)
```

When a middle command redirects output, it **breaks the pipe**!

This is valid syntax but usually not what you want.

</details>

---

## 🔮 Future Enhancements

### 1. Background Jobs (`&`)
```bash
long_command &  # Run in background
```

**Implementation idea:**
```java
class BackgroundCommand extends Command {
    private Command innerCommand;
    
    @Override
    public CommandType getType() {
        return CommandType.BACKGROUND;
    }
}
```

### 2. Command Substitution (`` `cmd` `` or `$(cmd)`)
```bash
echo Today is `date`
echo Today is $(date)
```

### 3. Conditional Execution (`&&`, `||`)
```bash
mkdir dir && cd dir     # cd only if mkdir succeeds
rm file || echo failed  # echo only if rm fails
```

### 4. Environment Variables
```bash
export PATH=/usr/bin
echo $HOME
```

---

# Part 9: Practice Questions

## 🎓 Beginner

**Q1:** What does this command do?
```bash
echo hello > file.txt
```

**Q2:** How many tokens?
```bash
echo "hello world" test
```

**Q3:** What's wrong with this?
```bash
cat file.txt | > output.txt
```

---

## 🎯 Intermediate

**Q4:** Explain the data flow:
```bash
cat file.txt | grep test | wc -l > count.txt
```

**Q5:** Why do these produce different output?
```bash
wc -l file.txt    # Shows: "5 file.txt"
wc -l < file.txt  # Shows: "5"
```

**Q6:** What's in each file?
```bash
echo test > a.txt
echo test > a.txt
echo test >> a.txt
```

---

## 🚀 Advanced

**Q7:** Why does this hang and how to fix it?
```java
Process p = pb.start();
InputStream in = p.getInputStream();
in.transferTo(nextProcess.getOutputStream());
p.waitFor(); // Hangs!
```

**Q8:** Implement a parser for this:
```bash
grep pattern < in.txt > out.txt 2> err.txt
```

**Q9:** Design a class hierarchy for:
- Simple commands
- Pipelines
- Background jobs
- Command substitution

---

# Part 10: Summary

## ✅ What You've Learned

### Core Concepts
1. **Shell Architecture** - REPL loop, parsing, execution
2. **Tokenization** - Handling quotes, escaping, operators
3. **Command Types** - Simple, redirection, pipeline
4. **I/O Redirection** - stdin, stdout, stderr
5. **Pipelines** - Connecting processes
6. **Process Management** - Starting, waiting, stream handling

### Design Patterns
1. **Command Pattern** - Encapsulating command execution
2. **Factory Pattern** - Parser creates appropriate command type
3. **Template Method** - Base class with abstract methods
4. **Strategy Pattern** - Different execution strategies per type

### Best Practices
1. **Separation of Concerns** - Parser vs Executor
2. **Type Safety** - Use strong types, not strings
3. **Error Handling** - Validate at parse time
4. **Resource Management** - Close streams, cleanup files
5. **Testing** - Unit, integration, end-to-end

---

## 🎯 Key Takeaways

### Mental Models

**1. Parsing is a Pipeline**
```
Raw Input → Tokenization → Type Detection → Object Creation
```

**2. Execution is a Graph**
```
   Input Files
      ↓
   Command 1
      ↓
   Command 2
      ↓
   Output Files
```

**3. Redirection is Plumbing**
```
< : Connect input pipe
> : Connect output pipe
2>: Connect error pipe
| : Connect command pipes
```

---

## 📚 Additional Resources

### Further Reading
1. **Unix Philosophy** - "Do one thing well"
2. **POSIX Shell Specification** - Standard behavior
3. **Process Management** - fork, exec, wait
4. **File Descriptors** - Low-level I/O

### Projects to Try
1. Add tab completion
2. Implement command history
3. Add shell scripts support
4. Build-in commands (cd, export, alias)
5. Job control (fg, bg, jobs)

---

## 🎉 Congratulations!

You now understand how to build a functional shell from scratch!

**Skills Gained:**
- ✅ Parser implementation
- ✅ Process management
- ✅ Stream redirection
- ✅ Object-oriented design
- ✅ Testing strategies

**Next Steps:**
1. Review the 247 tests
2. Try adding a new feature
3. Read the codebase
4. Experiment with edge cases

---

## 📝 Quick Reference Card

```bash
# Redirection Operators
<     Input from file
>     Output to file (overwrite)
>>    Output to file (append)
2>    Errors to file
|     Pipe to next command

# Examples
cat < in.txt              # Read from file
echo hi > out.txt         # Write to file
echo hi >> log.txt        # Append to file
cmd 2> err.txt           # Errors to file
cat file | grep test      # Pipe output

# Combined
cat < in.txt | grep x > out.txt 2> err.txt
# Flow: in.txt → cat → grep → out.txt
#                         ↓
#                     err.txt
```

---

**Total Lines of Code:** ~500 (core implementation)  
**Total Tests:** 247 (100% passing)  
**Languages:** Java 11+  
**Build Tool:** Maven  

**Built with ❤️ using Java**

---

*End of Guide. Happy Shell Building!* 🚀

