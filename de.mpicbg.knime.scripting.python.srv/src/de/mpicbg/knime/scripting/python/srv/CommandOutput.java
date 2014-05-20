package de.mpicbg.knime.scripting.python.srv;

/**
 * Encapsulates output/error streams.
 * User: haux
 * Date: 1/31/11
 * Time: 3:11 PM
 * To change this template use File | Settings | File Templates.
 */

import java.util.Hashtable;
import java.util.List;

/**
 * Encapsulate the standard and error output streams
 */
public class CommandOutput extends Hashtable {
    private List<String> stdout;
    private List<String> stderr;

    public CommandOutput(StreamGobbler stdout, StreamGobbler stderr) {
        this.stdout = stdout.getOutput();
        this.stderr = stderr.getOutput();
    }

    public boolean hasStandardOutput() {
        return !stdout.isEmpty();
    }

    public boolean hasErrorOutput() {
        return
        !stderr.isEmpty();
    }

    public List<String> getStandardOutput() {
        return stdout;
    }

    public List<String> getErrorOutput() {
        return stderr;
    }
}
