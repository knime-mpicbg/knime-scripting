package de.mpicbg.knime.scripting.matlab.srv;

import de.mpicbg.knime.scripting.matlab.srv.MatlabWebClient;


/**
 * Created by IntelliJ IDEA. User: haux Date: Jan 12, 2011 Time: 12:57:18 PM To change this template use File | Settings
 * | File Templates.
 */
public class FetchTester {

    public static void main(String[] args) {
        MatlabWebClient matlab = new MatlabWebClient("quantpro-mac-3", 1198);
        matlab.eval("a= 1+1");
        System.out.println(matlab.getScalar("a"));

//        new Xfile(4096);
//        new Xfile(4096);
//        matlab.uploadFile(new File("/Users/haux/test.py"), new File("/tmp/test.py"));
//        ma/tlab.(new File("/private/tmp/tpb4568b4a_bafb_4d15_b591_7a6b9d3300cd.png"), new File("/Users/brandl/testfile.png"));

        //todo reenable me
        MatlabWebClient matlab2 = new MatlabWebClient("quantpro-mac-3", 1198);

    }

}
