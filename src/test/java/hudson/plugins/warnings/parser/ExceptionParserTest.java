package hudson.plugins.warnings.parser;

import hudson.plugins.analysis.util.model.FileAnnotation;

import hudson.plugins.analysis.util.model.Priority;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * Test the class {@link ExceptionParser}
 */
public class ExceptionParserTest extends ParserTester {
    @Test
    public void test() throws IOException {
        Collection<FileAnnotation> warnings = new ExceptionParser().parse(openFile());
        assertEquals(WRONG_NUMBER_OF_WARNINGS_DETECTED, 1, warnings.size());
        Iterator<FileAnnotation> iterator = warnings.iterator();
        FileAnnotation annotation = iterator.next();
        checkWarning(annotation,
                0,
                "Exception in thread \"main\" java.lang.NullPointerException: Fictitious NullPointerException\n" +
                        "<br>at StackTraceExample.method111(StackTraceExample.java:15)\n" +
                        "<br>at StackTraceExample.method11(StackTraceExample.java:11)\n" +
                        "<br>at StackTraceExample.method1(StackTraceExample.java:7)\n" +
                        "<br>at StackTraceExample.main(StackTraceExample.java:3)",
                "",
                "Exception", "Exceptions", Priority.NORMAL);
    }

    @Override
    protected String getWarningsFile() {
        return "exceptions.txt";
    }
}

