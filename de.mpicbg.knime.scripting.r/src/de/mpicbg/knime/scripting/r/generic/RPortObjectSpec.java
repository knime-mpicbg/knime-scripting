/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.09.2008 (gabriel): created
 */
package de.mpicbg.knime.scripting.r.generic;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;

import javax.swing.*;
import java.io.IOException;


/**
 * A port object spec for R model port.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RPortObjectSpec implements PortObjectSpec {

    /**
     * The port object spec instance.
     */
    public static final RPortObjectSpec INSTANCE = new RPortObjectSpec();


    /**
     * Creating a new instance of <code>RPortObjectSpec</code>.
     */
    private RPortObjectSpec() {
    }


    /**
     * Serializer used to save this port object spec.
     *
     * @return a {@link RPortObjectSpec}
     */
    public static PortObjectSpecSerializer<RPortObjectSpec>
    getPortObjectSpecSerializer() {
        return new PortObjectSpecSerializer<RPortObjectSpec>() {
            /** {@inheritDoc} */
            @Override
            public RPortObjectSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in)
                    throws IOException {
                return INSTANCE;
            }


            /** {@inheritDoc} */
            @Override
            public void savePortObjectSpec(final RPortObjectSpec portObjectSpec,
                                           final PortObjectSpecZipOutputStream out)
                    throws IOException {

            }
        };
    }


    /**
     * {@inheritDoc}
     */
    public JComponent[] getViews() {
        return new JComponent[]{};
    }


    // just used to debug the port visualization
//    public static void main(String[] args) throws REXPMismatchException, RserveException, IOException {
//        RConnection connection = new RConnection("localhost", 6311);
//
//        RUtils.restoreObjects(Collections.singletonMap("R", new File("/var/folders/fA/fAVb4LGvGBe3JwuJpbcU4k+++TY/-Tmp-/genericR2798260749948420474R")), connection);
//
//        // note: maybe we should more specific summarization methods here depending on the class of the R-variable
//
//        connection.eval("sink('summary.txt')");
//         connection.eval("sumR<-summary(R)");
//        connection.eval("print(sumR)");
//        connection.eval("sink()");
//
//        REXP rexp = connection.eval("readChar(file('summary.txt', 'r'), 100000)");
//
//        System.out.println("Result is:\n " + rexp.asString());
//
//    }

}
