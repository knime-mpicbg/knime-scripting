package de.mpicbg.tds.knime.knutils;

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
