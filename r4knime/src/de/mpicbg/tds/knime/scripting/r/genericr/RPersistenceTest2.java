package de.mpicbg.tds.knime.scripting.r.genericr;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RPersistenceTest2 {

    public static void main(String[] args) throws RserveException, REXPMismatchException, IOException, ClassNotFoundException {
//        StartRserve.checkLocalRserve();

//        RConnection connection = new RConnection();
//
//        REXP rexp = connection.eval("data(iris);iris");
//        connection.close();
//
//
//        System.err.println("rexp is " + rexp);
//
//
//        persistRexp(rexp, new File("/Users/brandl/testobject.bin"));


        REXP deserialRexp = unpersistRexp(new File("/Users/brandl/knime-rexp.bin"));

        RConnection connection2 = new RConnection();
        connection2.assign("blabla", deserialRexp);

        System.err.println("from xml is \n" + deserialRexp.asList());
    }


    private static REXP unpersistRexp(File inputFile) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile)));

            return (REXP) ois.readObject();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public static void persistRexp(REXP rexp, File outputFile) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

            oos.writeObject(rexp);
            oos.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
