package de.mpicbg.tds.knime.knutils.scripting.rgg;

import com.thoughtworks.xstream.XStream;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RggTester {

    public static void main(String[] args) {
        XStream xStream = new XStream();
        System.err.println(xStream.toXML(new JCheckBox()));
    }

}
