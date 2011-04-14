package de.mpicbg.tds.knime.scripting.r;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RLauncherTest {

    public static void main(String[] args) throws RserveException {
//        StartRserve.checkLocalRserve();

        new RConnection().close();


    }

}
