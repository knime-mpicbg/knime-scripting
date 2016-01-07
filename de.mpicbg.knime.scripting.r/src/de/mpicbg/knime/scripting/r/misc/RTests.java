package de.mpicbg.knime.scripting.r.misc;

import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RTests {

    public static void main2(String[] args) throws RserveException {
        RConnection connection = new RConnection("localhost", 6311);


        File workSpaceFile = new File("RUtils-testrdata2.bin");
        String robjectName = "myR";

        connection.voidEval(robjectName + "<-iris");

        try {
            RUtils.saveToLocalFile(workSpaceFile, connection, "localhost", robjectName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (REXPMismatchException e) {
            e.printStackTrace();
        } catch (REngineException e) {
            e.printStackTrace();
        } catch (KnimeScriptingException e) {
			e.printStackTrace();
		}

        connection.close();
        System.err.println("done");
    }


    public static String transferCharset = "UTF-8";


    public static void main3(String[] args) throws RserveException, REXPMismatchException, REngineException, UnsupportedEncodingException {
//        RConnection connection = new RConnection("localhost", 6311);


//        File workSpaceFile = new File("RUtils-testrdata2.bin");
//        String robjectName = "myR";
//
////        connection.eval(robjectName + "<-iris");
//
//        try {
////            saveToLocalFile(workSpaceFile, connection, "localhost", robjectName);
//
//            RUtils.loadGenericInputs(Collections.singletonMap("newVarName", workSpaceFile), connection);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (REXPMismatchException e) {
//            e.printStackTrace();
//        }

//        byte b[] = "".getBytes(RConnection.transferCharset);
//
//        System.err.println("bytes " + b);
//        RList rList= new RList(1,true);
//        rList.put("concentration", new REXPString(new String[]{"", "250 nm"}));
//        rList.put("testdf", new REXPDouble(new double[]{1.,2.,5.}));
//        rList.put("testdfd", new REXPDouble(new double[]{1.,2.,5.}));
//
//        REXP rexp = REXP.createDataFrame(rList);
        RConnection connection = new RConnection("localhost", 6311);
        connection.assign("testVar", new String[]{"", "foobar"});

//        connection.assign("testVar", new String[]{"test", "blabla", ""});
//        connection.assign("test2", rexp);

//        connection.eval("plot(1:10)");

        connection.close();
//        REXPDouble rexp = (REXPDouble) connection.eval("as.double(c(2, rep(NA, 5)))");
//        System.err.println("done: " + Arrays.toString(rexp.isNA()));
    }

    public static void main(String[] args) throws RserveException, REXPMismatchException, REngineException, UnsupportedEncodingException {
        RConnection c = new RConnection("localhost", 6311);

        // test factors and levels
        c.assign("x", new int[]{1, 2, 3, 4});
        REXP result = c.eval("mean(x)");
        RList rList = new RList(2, true);
        rList.put("a", new REXPFactor(new int[]{1, 2, 3, 1, 2, 3}, new String[]{"blue", "red", "green"}));
        rList.put("x", new REXPDouble(new double[]{0.1, 0.1, 0.5, 0.7, 0.9, 0.6}));
        c.assign("mylist", REXP.createDataFrame(rList));
        REXP levels = c.eval("levels(mylist$a)");
        REXP tapply = c.eval("tapply(mylist$x, mylist$a, mean)");

        c.close();
        System.err.println("done: " + result.asString() + " +++ " + levels.asString());
    }

    public static Map<String, Map<String, Object>> convertToSerializable(Map<String, REXP> pushTable) {
        try {
            Map<String, Map<String, Object>> serialPushTable = new LinkedHashMap<String, Map<String, Object>>();

            for (String parName : pushTable.keySet()) {
                REXP dataFrame = pushTable.get(parName);

                Map<String, Object> dfTable = new LinkedHashMap<String, Object>();
                serialPushTable.put(parName, dfTable);

                // post-process nominal attributes to convert missing values to actual NA in R
                RList rList = dataFrame.asList();

                for (Object columnKey : rList.keys()) {
                    String colName = columnKey.toString();

                    REXPVector column = (REXPVector) rList.get(columnKey);

                    if (column instanceof REXPDouble) {
                        dfTable.put(colName, ((REXPDouble) column).asDoubles());
                    } else if (column instanceof REXPInteger) {
                        dfTable.put(colName, ((REXPInteger) column).asIntegers());
                    } else if (column instanceof REXPString) {
                        dfTable.put(colName, ((REXPString) column).asStrings());
                    }

                    System.err.println("");
                }
            }

            return serialPushTable;
        } catch (REXPMismatchException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static Map<String, REXP> loadFromSerializable(Map<String, Map<String, Object>> dfMap) throws REXPMismatchException {
        Map<String, REXP> pushTable = new LinkedHashMap<String, REXP>();

        for (String dfName : dfMap.keySet()) {
            Map<String, Object> columns = dfMap.get(dfName);
            RList rList = new RList(columns.size(), true);

            for (String colName : columns.keySet()) {
                Object colData = columns.get(colName);
                if (colData instanceof double[]) {
                    rList.put(colName, new REXPDouble((double[]) colData));
                } else if (colData instanceof String[]) {
                    rList.put(colName, new REXPString((String[]) colData));
                } else if (colData instanceof int[]) {
                    rList.put(colName, new REXPInteger((int[]) colData));
                }
            }


            REXP df = REXP.createDataFrame(rList);

            pushTable.put(dfName, df);
        }

        return pushTable;
    }
}
