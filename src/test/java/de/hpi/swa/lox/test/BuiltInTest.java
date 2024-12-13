package de.hpi.swa.lox.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

import de.hpi.swa.lox.cli.LoxMain;

public class BuiltInTest extends AbstractLoxTest {
    @Test
    public void testNumberInt() {
        runAndExpect("parse number",
                "print 3 + Number(\"4\");", "7\n");
    }

    @Test
    public void testNumberDouble() {
        runAndExpect("parse double",
                "print 1 + Number(\"0.5\");", "1.5\n");
    }

    @Test
    public void testStringConversion() {
        runAndExpect("convert number to string",
                "print \"Hello\" + String(3);", "Hello3\n");
    }

    @Test
    public void testFileWithArgs() throws IOException {
        // Create a temporary file
        File tempFile = File.createTempFile("testFile", ".lox");
        tempFile.deleteOnExit();
        // Write to the temporary file
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("print ARGV[0];");
        }
        // Execute the LoxMain with the temporary file
        LoxMain.main(new String[] { tempFile.getAbsolutePath(), "hello" });
        // Verify the output
        assertEquals("Should print hello", "hello\n", normalize(outContent.toString()));
    }
}
