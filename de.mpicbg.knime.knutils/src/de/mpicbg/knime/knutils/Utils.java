package de.mpicbg.knime.knutils;

import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Utils {

    /**
     * Try to determine whether this application is running under Windows or some other platform by examing the
     * "os.name" property.
     *
     * @return true if this application is running under a Windows OS
     */
    public static boolean isWindowsPlatform() {
        String os = System.getProperty("os.name");
        return os != null && os.startsWith("Windows");
    }


    public static boolean isMacOSPlatform() {
        String os = System.getProperty("os.name");
        return os != null && os.equals("Mac OS X");
    }
    
    /**
     * converts a float array into a double array
     * @param input
     * @return array with doubles
     */
    public static double[] convertFloatsToDoubles(float[] input)
    {
        if (input == null)
        {
            return null;
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }
    
    /**
     * converts a double array into a float array; USE WITH CAUTION as precision might be lost! 
     * @param input
     * @return array with doubles
     */
    public static float[] convertDoublesToFloats(double[] input)
    {
        if (input == null)
        {
            return null;
        }
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float) input[i];
        }
        return output;
    }

    /**
     * retrieves the KNIME DataType corresponding to a java class
     *
     * @param intStrOrDouble
     * @return
     */
    public static DataType mapType(Class intStrOrDouble) {
        if (intStrOrDouble.equals(Integer.class)) {
            return IntCell.TYPE;

        } else if (intStrOrDouble.equals(Double.class) || intStrOrDouble.equals(Float.class)) {
            return DoubleCell.TYPE;

        } else if (intStrOrDouble.equals(String.class)) {
            return StringCell.TYPE;

        } else {
            throw new RuntimeException("Could not map unknown type '" + intStrOrDouble + "'to knime-type");
        }
    }


    public static void main(String[] args) {
        isWindowsPlatform();

        isMacOSPlatform();
    }
}
