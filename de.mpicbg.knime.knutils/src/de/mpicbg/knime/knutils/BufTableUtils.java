package de.mpicbg.knime.knutils;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class BufTableUtils {


    public static void main(String[] args) {
    }


    public static void saveBinary(File dumpFile, Object object) {
        try {
            ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream(dumpFile));
            objStream.writeObject(object);
            objStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Object loadBinary(File dumpFile) {
        try {
            ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(dumpFile));
            return inStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    public static List<DataRow> toList(BufferedDataTable input) {
        List<DataRow> data = new ArrayList<DataRow>();

        for (DataRow dataRow : input) {
            data.add(dataRow);
        }

        return data;
    }


    /**
     * check if the execution monitor was canceled.
     */
    public static void updateProgress(ExecutionContext exec, long done, long all) throws CanceledExecutionException {
        exec.checkCanceled();
        exec.setProgress((double) done / (double) all);
    }
}
