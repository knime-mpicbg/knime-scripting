/*
 * Copyright (c) 2011.
 * Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
 *
 * This module is distributed under the BSD-License. For details see the license.txt.
 *
 * It is the obligation of every user to abide terms and conditions of The MathWorks, Inc. Software License Agreement.
 * In particular Article 8 “Web Applications”: it is permissible for an Application created by a Licensee of the
 * NETWORK CONCURRENT USER ACTIVATION type to use MATLAB as a remote engine with static scripts.
 */

package de.mpicbg.knime.scripting.matlab.srv;

/**
 * Author: Felix Meyenhofer
 * Date: 2/23/11
 * Time: 6:51 PM
 */

public class TestMatlab {

    public static void main(String[] args) throws Exception {

        MatlabWebClient matlab = null;
        if ((matlab == null) || !matlab.isConnected()) {
            matlab = new MatlabWebClient("localhost", 1198);
        }

        matlab.eval("disp('ist jemand da?')");

        System.out.println("Create a temp file.");
        matlab.createTempFile("matlab_", ".tmp");

        System.out.println("This is the end.");
    }

}
