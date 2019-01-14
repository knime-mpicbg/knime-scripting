package de.mpicbg.knime.knutils;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * A warpper of NodeModel which implements a convenience API to manage node settings.
 *
 * @author Holger Brandl
 */
public abstract class AbstractNodeModel extends NodeModel {

    protected final NodeLogger logger = NodeLogger.getLogger(this.getClass());

    // flag is used to test a new implementation of the settings list (as hashmap, to retrieve back settings from the list)
    protected final boolean useNewSettingsHashmap;

    // old implementation to store setting
    private List<SettingsModel> allSettings = new ArrayList<SettingsModel>();

    // new implementation to store settings
    protected HashMap<String, SettingsModel> modelSettings = new HashMap<String, SettingsModel>();


    protected AbstractNodeModel() {
        super(1, 1);
        this.useNewSettingsHashmap = false;
    }


    public AbstractNodeModel(int numInputs, int numOutputs) {
        super(numInputs, numOutputs);
        this.useNewSettingsHashmap = false;
    }

    public AbstractNodeModel(int numInputs, int numOutputs, boolean useNewSettingsHashmap) {
        super(numInputs, numOutputs);
        this.useNewSettingsHashmap = useNewSettingsHashmap;
    }

    public AbstractNodeModel(PortType[] inPorts, PortType[] outPorts) {
        super(inPorts, outPorts);
        this.useNewSettingsHashmap = false;
    }

    public AbstractNodeModel(PortType[] inPorts, PortType[] outPorts, boolean useNewSettingsHashmap) {
        super(inPorts, outPorts);
        this.useNewSettingsHashmap = useNewSettingsHashmap;
    }


    public static PortType[] createPorts(final int numPorts, final int... optionalPortsIds) {
        return createPorts(numPorts, BufferedDataTable.TYPE, BufferedDataTable.class, optionalPortsIds);
    }


    public static PortType[] createPorts(final int numPorts, PortType portType, Class<? extends PortObject> portDataClass, final int... optionalPortsIds) {
        PortType[] portTypes = new PortType[numPorts];
        Arrays.fill(portTypes, portType);

        if (optionalPortsIds.length > 0) {
            for (int portId : optionalPortsIds) {
                if ((portId - 1) < numPorts) {
                    portTypes[portId - 1] = new PortType(portDataClass, true);
                }
            }
        }

        return portTypes;
    }

    /**
     * add settings to a list
     *
     * @param settingModel
     * @deprecated
     */
    protected void addSetting(SettingsModel settingModel) {
        allSettings.add(settingModel);
    }

    /**
     * ================================================================================
     * implements an alternative to the array list of model settings
     * advantage: settings can be retrieved by name and do not have to be stored again as member of the child class
     *
     * @param settingName
     * @param settingsModel
     */
    protected void addModelSetting(String settingName, SettingsModel settingsModel) {
        modelSettings.put(settingName, settingsModel);
    }

    protected SettingsModel getModelSetting(String settingName) {
        return modelSettings.get(settingName);
    }
    
    /**
     * if autoguessing is used in configure, setting model needs to be updates (=> replaced)
     * 
     * @param settingName		key of the setting to be replaced
     * @param settingsModel		new settings model
     * 
     * @throws InvalidSettingsException		thrown if key is not present or model class does not fit
     */
    protected void updateModelSetting(String settingName, SettingsModel settingsModel) 
    		throws InvalidSettingsException {
    	if(!modelSettings.containsKey(settingName))
    		throw new InvalidSettingsException("Setting \"" + settingName + "\" does not belong to the settings of this node. Implementation error.");
    	SettingsModel sm = modelSettings.get(settingName);
    	
    	String modelClass = sm.getClass().getSimpleName();
    	if(!sm.getClass().equals( settingsModel.getClass())) {
    		throw new InvalidSettingsException("Setting \"" + settingName + "\" is not of class \"" + modelClass + "\". Implementation error.");   		
    	}
    	
    	modelSettings.replace(settingName, settingsModel);
    }

    /**
     * ================================================================================
     */

    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }


    @Override
    /** A fake implementation of configure which is supposed to be overriden by extending classes. */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        if (getNrOutPorts() == 0)
            return new DataTableSpec[0];
        else
            return inSpecs;
    }


    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (!useNewSettingsHashmap) {
            for (SettingsModel uiProperty : allSettings) {
                uiProperty.saveSettingsTo(settings);
            }
        } else {
            for (SettingsModel uiProperty : modelSettings.values()) {
                uiProperty.saveSettingsTo(settings);
            }
        }


    }


    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	
    	boolean debug = true;
    	if(debug) nodeSettingsToLog(this.getClass());

        if (!useNewSettingsHashmap) {
            for (SettingsModel uiProperty : allSettings) {
                try {
                    uiProperty.loadSettingsFrom(settings);
                } catch (InvalidSettingsException ise) {
                    String message = "Could not load property '" + uiProperty.toString() + "'of node " + this.getClass().getSimpleName();

                    setWarningMessage(message);
                    logger.warn(message + ". \nPlease re-execute the node to get rid of this problem!");
                }
            }
        } else {
            for (SettingsModel uiProperty : modelSettings.values()) {
                try {
                    uiProperty.loadSettingsFrom(settings);
                } catch (InvalidSettingsException ise) {
                    String message = "Could not load property '" + uiProperty.toString() + "'of node " + this.getClass().getSimpleName();

                    setWarningMessage(message);
                    logger.warn(message + ". \nPlease re-execute the node to get rid of this problem!");
                }
            }
        }

    }


    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        for (SettingsModel uiProperty : allSettings) {
            try {
                uiProperty.validateSettings(settings);
            } catch (InvalidSettingsException ise) {
//                logger.warn("Could not validate property '" + uiProperty.toString() + "' of node " + this.getClass().getSimpleName());
            }
        }
    }


    @Override
    protected void loadInternals(final File internDir,
                                 final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

//        System.err.println("t");
        // TODO load internal data.
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }


    @Override
    protected void saveInternals(final File internDir,
                                 final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
//        System.err.println("t");

        // TODO save internal models.
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }


    public String getFlowVariable(String flowVarName) {
        try {
            return peekFlowVariableString(flowVarName);
        } catch (Throwable t) {
            // don't do anything
        }

        try {
            return "" + peekFlowVariableInt(flowVarName);
        } catch (Throwable t) {
            // don't do anything
        }

        try {
            return "" + peekFlowVariableDouble(flowVarName);
        } catch (Throwable t) {
            // don't do anything
        }

        // we have not found a flow-variable for the given name
        return null;
    }
    
    /**
     * debug method to push all model settings (name+type) to the log
     * 
     * @param nodeClass
     */
    public void nodeSettingsToLog(Class<? extends AbstractNodeModel> nodeClass) {
    	StringBuilder logString = new StringBuilder();
    	if(this.useNewSettingsHashmap) {
    		logString.append(nodeClass.toString() + " SETTINGS:\n");
    		for(String setting : modelSettings.keySet()) {
    			SettingsModel model = modelSettings.get(setting);
    			logString.append("\t" + setting + " (" + model.getClass().getName() + ")\n");
    		}
    		
    	} else
    		logString.append(nodeClass.getName() + " does not yet use the hashmap for settings.");
    	
    	logger.info(logString);
    }
    
	/**
	 * checks if columns of given list are available; throws warning or error if columns are missing
	 * 
	 * @param inSpec		table specs
	 * @param onlyWarn		true = no error, just a warning
	 * @param columns		list of columns to test for
	 * 
	 * @throws InvalidSettingsException
	 */
	protected void checkColumnsForAvailability(DataTableSpec inSpec, String[] columns, final Class<? extends DataValue> valueClass, boolean onlyWarn, boolean atLeastOneRequired)
			throws InvalidSettingsException {
		// collect columns which are not available in input spec and need to be double-compatible
		List<String> missingColumns = new ArrayList<String>();
		for(String col : columns) {
			if(!inSpec.containsName(col))
				missingColumns.add(col);
			else 
				if(!inSpec.getColumnSpec(col).getType().isCompatible(valueClass))
					missingColumns.add(col);
		}	

		if(atLeastOneRequired) {
			if(missingColumns.size() == columns.length)
				if(columns.length == 1)
					throw new InvalidSettingsException("Input table does not contain the expected columns: " + String.join(",", missingColumns) + " or column is not of type: " + valueClass.getSimpleName());
				else
					throw new InvalidSettingsException("Input table does not contain any of the expected columns: " + String.join(",", missingColumns) + " or columns are not of type: " + valueClass.getSimpleName());
		}
		
		if(!missingColumns.isEmpty()) {
			if(onlyWarn)
				setWarningMessage("The following columns are either missing or not numeric (will be ignored for processing): " + String.join(",", missingColumns));
			else 
				throw new InvalidSettingsException("The following columns are either missing or not numeric: " + String.join(",", missingColumns));
		}
	}
}
