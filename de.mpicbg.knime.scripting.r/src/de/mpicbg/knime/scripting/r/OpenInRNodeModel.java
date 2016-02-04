package de.mpicbg.knime.scripting.r;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.r.node.openinr.OpenInRNodeModel2;


/**
 * This is the model implementation of RSnippet. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 * @deprecated use {@link OpenInRNodeModel2} instead.
 */
public class OpenInRNodeModel extends AbstractRScriptingNodeModel {


    /**
     * Constructor for the node model.
     * @deprecated
     */
    protected OpenInRNodeModel() {
        super(createPorts(3, 2, 3), createPorts(0), new RColumnSupport());
    }


    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
        this.openIn(inData, exec);
        return new BufferedDataTable[0];
	}
}