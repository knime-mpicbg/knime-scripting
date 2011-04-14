package de.mpicbg.tds.knime.scripting.r.genericr;

import de.mpicbg.tds.knime.scripting.r.OpenInRNodeModel;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

import java.io.File;


/**
 * This is the model implementation of generic version of OpenInR. It allows to spawn a new instance of R using the node
 * input as workspace initialization. It's main purpose is for prototyping.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GenericOpenInRNodeModel extends GenericRSnippet {


    protected GenericOpenInRNodeModel() {
        super(createPorts(1, RPortObject.TYPE, RPortObject.class), new PortType[0]);
    }


    @Override
    protected PortObject[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {
        PortObject[] portObjects = super.execute(inData, exec);

        File workSpaceFile = ((RPortObject) portObjects[0]).getFile();

        logger.info("Spawning R-instance ...");
        OpenInRNodeModel.openWSFileInR(workSpaceFile, prepareScript());

        return new PortObject[0];
    }
}