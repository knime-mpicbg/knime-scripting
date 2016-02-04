package de.mpicbg.knime.scripting.r.misc;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RPersistenceTest {

    public static void main(String[] args) throws RserveException, REXPMismatchException {
//        StartRserve.checkLocalRserve();

        RConnection connection = new RConnection();

        REXP rexp = connection.eval("data(iris);iris");

        connection.voidEval("sink('summary.txt')");
        connection.voidEval("sumR<-summary(iris)");
        connection.voidEval("printsumR)");
        connection.voidEval("sink()");
        String summary = connection.eval("readChar(file('summary.txt', 'r'), 100000)").asString();

        System.err.println("the read summary is " + summary);
        System.err.println("the read summary is " + connection.getLastError());

        connection.close();
    }
}
