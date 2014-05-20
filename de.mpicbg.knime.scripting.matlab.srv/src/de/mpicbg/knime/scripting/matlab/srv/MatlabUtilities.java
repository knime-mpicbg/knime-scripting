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
 * Created by IntelliJ IDEA.
 * User: Holger Brandl
 * Date: 1/27/11
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */

public class MatlabUtilities {

    public static StringBuffer convert2StringVector(double[] vector) {
        StringBuffer stringVec = new StringBuffer("[");
        for (double value : vector) {
            stringVec.append(value).append(", ");
        }
        stringVec.deleteCharAt(stringVec.length() - 1).append("]");
        return stringVec;
    }

}
