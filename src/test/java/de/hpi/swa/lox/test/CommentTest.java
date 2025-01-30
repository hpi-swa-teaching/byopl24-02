package de.hpi.swa.lox.test;

import org.junit.Test;

public class CommentTest extends AbstractLoxTest {

    @Test
    public void testComment() {
        runAndExpect("comment", "// this is a comment\nprint 1;", "1\n");
    }

    @Test
    public void testCommentAtEnd() {
        runAndExpect("comment at end", "print 1; // this is a comment\n", "1\n");
    }

    @Test
    public void testMultiLineComment() {
        runAndExpect("multi line comment", "/* this is a\nmulti line comment */\nprint 1;", "1\n");
    }

}
