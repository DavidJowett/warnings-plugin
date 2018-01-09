package hudson.plugins.warnings.parser;

import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.Extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

@Extension
public class ExceptionParser extends AbstractWarningsParser {
    private static final long serialVersionUID = 324132154565435L;
    // Match the first line of a stack trace with an optional "[testng]" prefix
    private static final String stRegex =  ".*(\\[testng\\])?\\s*[\\w\\.]*Exception[\\w\\.]*:.*";
    // Match the following lines of a stack trace with an optional "[testng]" prefix
    private static final String stlRegex = "\\s*(\\[testng\\])?\\s*((at)|(Caused by:)|(\\.\\.\\.)) [\\w<>.]+.*";
    public ExceptionParser(){
        super("Java Exceptions","Exceptions","Exceptions");
    }
    public Collection<FileAnnotation> parse(final Reader reader) throws IOException {
        Collection<FileAnnotation> fas = new HashSet<>();
        BufferedReader br = new BufferedReader(reader);
        String line;
        int currentLine = 0;
        Warning cw = null;
        while((line = br.readLine()) != null) {
            if(Pattern.matches(stRegex, line)){
                if(cw != null)
                    fas.add(cw);
                cw = new Warning("", currentLine, "Exception","Exceptions",line);
            } else if(Pattern.matches(stlRegex, line) && cw != null) {
                cw = new Warning(cw, "<br>" + line, currentLine);
            } else if (cw != null){
                fas.add(cw);
                cw = null;
            }
            currentLine++;
        }
        if(cw != null)
            fas.add(cw);

        return fas;
    }
}
