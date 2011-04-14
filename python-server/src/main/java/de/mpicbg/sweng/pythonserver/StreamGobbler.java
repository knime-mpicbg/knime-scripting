package de.mpicbg.sweng.pythonserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: haux
 * Date: 2/3/11
 * Time: 1:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class StreamGobbler extends Thread {
    InputStream is;
    List<String> output = new ArrayList<String>();

    public StreamGobbler(InputStream is) {
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                output.add(line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public List<String> getOutput() {
        return output;
    }
}
