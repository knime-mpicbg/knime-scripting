package de.mpicbg.knime.scripting.r;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.property.ShapeFactory;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.knutils.data.property.ColorModelUtils;
import de.mpicbg.knime.knutils.data.property.ShapeModelUtils;
import de.mpicbg.knime.knutils.data.property.SizeModel;
import de.mpicbg.knime.knutils.data.property.SizeModel.Mapping;
import de.mpicbg.knime.knutils.data.property.SizeModelUtils;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.data.RDataColumn;
import de.mpicbg.knime.scripting.r.data.RDataFrameContainer;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

public abstract class AbstractRScriptingNodeModel extends AbstractScriptingNodeModel {

	public static final String R_INVAR_BASE_NAME = "kIn";
	public static final String R_OUTVAR_BASE_NAME = "rOut";

	/** map with possible KNIME shapes */
	public static final Map<String, Integer> R_SHAPES;
	static {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(ShapeFactory.ASTERISK, 8);
		map.put(ShapeFactory.CIRCLE, 16);
		map.put(ShapeFactory.CROSS, 4);
		map.put(ShapeFactory.DIAMOND, 18);
		map.put(ShapeFactory.HORIZONTAL_LINE, 45);
		map.put(ShapeFactory.RECTANGLE, 15);
		map.put(ShapeFactory.REVERSE_TRIANGLE, 25);
		map.put(ShapeFactory.TRIANGLE, 17);
		map.put(ShapeFactory.VERTICAL_LINE, 124);
		map.put(ShapeFactory.X_SHAPE, 4);
		R_SHAPES = Collections.unmodifiableMap(map);
	}

	public static final String CFG_SCRIPT_DFT = "rOut <- kIn";
	public static final String CFG_SCRIPT2_DFT = "rOut <- kIn1";

	// constants defining the naming of variables used as KNIME input/output within R
	/** data-frame with input KNIME-table */
	public static final String VAR_RKNIME_IN = "knime.in";
	/** data-frame for output KNIME-table */
	public static final String VAR_RKNIME_OUT = "knime.out";
	/** list with input KNIME flow variables */
	public static final String VAR_RKNIME_FLOW_IN = "knime.flow.in";
	/** list for output KNIME flow variables */
	public static final String VAR_RKNIME_FLOW_OUT = "knime.flow.out";
	/** character vector for loading/saving used R packages */
	public static final String VAR_RKNIME_LIBS = "knime.loaded.libraries";
	/** KNIME color information TODO: how to represent in R? */
	public static final String VAR_RKNIME_COLOR = "knime.colors";
	/** KNIME input script */
	public static final String VAR_RKNIME_SCRIPT = "knime.script.in";
	/** KNIME workspace handle */
	public static final String VAR_RKNIME_WS_IN = "knime.ws.in";

	/** enum for datatypes which can be pushed to R via R-serve */
	public enum RType { R_DOUBLE, R_LOGICAL, R_INT, R_STRING, R_FACTOR };

	RConnection m_con = null;

	/**
	 * reset the connection member variable to null after closing
	 */
	private void closeRConnection() {
		if(m_con != null) {
			if(m_con.isConnected())
				m_con.close();
			m_con = null;
		}
	}

	public AbstractRScriptingNodeModel(PortType[] inPorts, PortType[] outPorts, RColumnSupport rColumnSupport) {
		super(inPorts, outPorts, rColumnSupport);
	}

	public AbstractRScriptingNodeModel(PortType[] inPorts, PortType[] outPorts) {
		super(inPorts, outPorts, new RColumnSupport());
	}

	public AbstractRScriptingNodeModel(ScriptingModelConfig nodeModelConfig) {
		super(nodeModelConfig);
	}

	protected void pushInputToR(PortObject[] inData, ExecutionContext exec) 
			throws KnimeScriptingException, CanceledExecutionException {

		ScriptingModelConfig cfg = getNodeCfg();
		int chunkInSize = -1;
		if(cfg.useChunkSettings())
			chunkInSize = getChunkIn(((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_IN)).getIntValue(), inData);

		exec.setMessage("Transfer to R");
		ExecutionMonitor transferToExec = exec.createSubProgress(1.0/2);

		assert m_con == null;

		m_con = RUtils.createConnection();

		// assign ports to R variable names
		Map<String, PortObject> inPorts = createPortMapping(inData);

		int nInData = getNumberOfUsedInputPorts(inData, false);
		int nInTables = getNumberOfUsedInputPorts(inData, true);
		int gIdx = getGenericIndex(inPorts);
		
		pushFlowVariablesToR(getAvailableFlowVariables(), exec);

		// capture all exception to close the R connection in that case
		try {
			// generic input to push first
			if(gIdx >= 0) {
				File gWorkspaceFile = ((RPortObject)inData[gIdx]).getFile();
				RUtils.loadWorkspace(gWorkspaceFile, m_con);
			}

			// push all KNIME data tables
			for(String in : inPorts.keySet()) {
				PortObject pObj = inPorts.get(in);
				if(pObj != null) {
					if(BufferedDataTable.TYPE.acceptsPortObject(pObj)) {
						pushTableToR((BufferedDataTable) pObj, in, transferToExec.createSubProgress(1/nInTables), chunkInSize);
					}
				}
			}
		} catch (KnimeScriptingException e) {
			closeRConnection();
			throw e;
		}
	}
	
	protected PortObject[] pullOutputFromR(ExecutionContext exec) 
			throws KnimeScriptingException {
		ScriptingModelConfig cfg = getNodeCfg();
		int chunkOutSize = -1;
		if(cfg.useChunkSettings())
			chunkOutSize = ((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_IN)).getIntValue();
		
		exec.setMessage("Pull output data from R");
		ExecutionMonitor transferFromExec = exec.createSubProgress(1.0/2);
		//ExecutionContext transferFromExec = exec.createSubExecutionContext(0.9);

		assert m_con != null;
		
		int nGeneric = 0;
		int nTables = getNumberOfTableOututPorts();
		
		PortObject[] outData = new PortObject[nTables + nGeneric];
		
		for(int i = 0; i < getNrOutPorts(); i++) {
			PortType pType = this.getOutPortType(i);
			
			if(pType.equals(RPortObject.TYPE)) {
				nGeneric++;
				if(nGeneric > 1) 
					throw new KnimeScriptingException("Implementation error: Multiple RPorts are not allowed");
			}
			else if(pType.equals(BufferedDataTable.TYPE)) {
				String outVarName = R_OUTVAR_BASE_NAME + (nTables > 1 ? i : "");
				BufferedDataTable table = null;
				try {
					table = pullTableFromR(outVarName, 
							transferFromExec.createSubProgress(1.0/(nTables + nGeneric)), exec, chunkOutSize);
				} catch (RserveException | REXPMismatchException | CanceledExecutionException e) {
					closeRConnection();
					throw new KnimeScriptingException("Failed to retrieve " + outVarName + " from R:\n" + e.getMessage());
				}
				outData[i] = table;
			}
		}
		
		return outData;
	}

	/**
	 * push KNIME table and its properties to R
	 * @param inTable
	 * @param varName
	 * @param exec
	 * @param chunkInSize 
	 * @throws CanceledExecutionException 
	 * @throws REXPMismatchException 
	 * @throws RserveException 
	 * @throws KnimeScriptingException 
	 */
	private void pushTableToR(BufferedDataTable inTable, String varName, ExecutionMonitor exec, int chunkInSize) 
			throws CanceledExecutionException, KnimeScriptingException {

		assert m_con != null;

		DataTableSpec inSpec = inTable.getSpec();

		// push color/size/shape model to R
		pushColorModelToR(inSpec, m_con, exec, varName);
		pushShapeModelToR(inSpec, m_con, exec, varName);
		pushSizeModelToR(inSpec, m_con, exec, varName);

		try {
			transferRDataContainer(exec, inTable, chunkInSize, m_con, varName);
		} catch(REXPMismatchException | RserveException e) {
			throw new KnimeScriptingException("Failed to transfer data to R:\n" + e.getMessage());
		}
	}

	private int getNumberOfUsedInputPorts(PortObject[] inData, boolean tablesOnly) {
		int i = 0;
		for(PortObject obj : inData)
			if(obj != null)
				if(tablesOnly) {
					if(BufferedDataTable.TYPE.acceptsPortObject(obj)) i++;
				} else i++;
		return i;
	}
	
	private int getNumberOfTableOututPorts() {
		int count = 0;
		for(int i = 0; i < getNrOutPorts(); i++)
			if(getOutPortType(i).equals(BufferedDataTable.TYPE))
				count ++;
		return count;
	}

	/**
	 * @param inPorts
	 * @return the index for the generic input port, -1 if not available
	 */
	private int getGenericIndex(Map<String, PortObject> inPorts) {
		int i = 0;
		for(String s : inPorts.keySet()) {
			if( s.equals("generic"))
				return i;
			i++;
		}
		return -1;
	}

	/**
	 * execute method needs to be implemented in sub nodes to create appropriate output
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		pushInputToR(inData, exec);	
		return null;	
	}

	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException, CanceledExecutionException {
		pushInputToR(inData, exec);
		openInR(inData, exec);
	}

	/**
	 * if chunk size setting is -1 or 0, set chunk size to the number of columns available (= no data chunking)
	 * for multiple inputs use the maximum number of columns of all input tables
	 * @param cIn
	 * @param inSpec
	 * @return chunksize
	 */
	public static int getChunkIn(int cIn, PortObject[] inTables) {

		if(cIn > 0) return cIn;

		int nColumnMax = 0;
		for(int i = 0; i < inTables.length; i++) {
			if(inTables[i] != null && BufferedDataTable.TYPE.acceptsPortObject(inTables[i])) {
				int n = ((BufferedDataTable)inTables[i]).getSpec().getNumColumns();
				nColumnMax = n > nColumnMax ? n : nColumnMax;
			}
		}
		return nColumnMax;
	}

	/**
	 * The values of the map are either files or (buffered)data tables.
	 * @param inObjects
	 * @return
	 * @throws KnimeScriptingException thrown if more than one generic input ports or unsupported port type
	 */
	public Map<String, PortObject> createPortMapping(PortObject[] inObjects) throws KnimeScriptingException {

		Map<String, PortObject> portVarMapping = new TreeMap<String, PortObject>();

		// number of non-null input port objects; (optional) input ports might be null if not connected
		int nTableInputs = 0;
		int nRPortInputs = 0;
		for (PortObject inport : inObjects) {
			if (inport != null) {
				if(inport instanceof BufferedDataTable) nTableInputs++;
				else if(inport instanceof RPortObject) nRPortInputs++;
				else
					throw new KnimeScriptingException("Implementation error: PortType " 
							+ inport.getClass().getSimpleName() + "is not yet supported");
			}
		}

		if(nRPortInputs > 1) throw new KnimeScriptingException("Implementation error: "
				+ "R Scripting nodes do not allow more than one generic input port");

		// create naming of input variables for R; e.g. kIn, or kIn1
		// map port objects to variable name
		int i = 0;
		for (PortObject inport : inObjects) {

			PortType pType = getInPortType(i);

			if(pType.equals(BufferedDataTable.TYPE)) {
				String RVariableName = R_INVAR_BASE_NAME + (nTableInputs > 1 ? (i + 1) : "");
				portVarMapping.put(RVariableName, inport);
				i++;
			}
			if(pType.equals(RPortObject.TYPE) && inport != null) {
				portVarMapping.put("generic", inport);
			}
		}

		return portVarMapping;
	}

	/**
	 * push all incoming KNIME-objects to R
	 * @param inObjects
	 * @param connection
	 * @param exec 
	 * @param chunkInSize 
	 * @return
	 * @throws KnimeScriptingException
	 */
	/*    public Map<String, Object> pushToR(PortObject[] inObjects, RConnection connection, ExecutionMonitor exec, int chunkInSize) throws KnimeScriptingException {
        Map<String, Object> inputMapping = createPortMapping(inObjects);

        double nInput = inputMapping.size();
        exec.setMessage("Transfer to R");
        ExecutionMonitor transferToExec = exec.createSubProgress(1.0/2);

        // first push the generic input (limited to one workspace input)
        File rWorkspaceFile = getWorkspaceFile(inputMapping);
        try {
           // if(genPortMapping.size() > 0) RUtils.loadGenericInputs(genPortMapping, connection);
        	if(rWorkspaceFile != null) RUtils.loadWorkspace(rWorkspaceFile, connection);
        } catch (Throwable e) {
            throw new KnimeScriptingException("Failed to convert generic node inputs into r workspace variables: " + e);
        }

        // second, push the table inputs
        Map<String, BufferedDataTable> tablePortMapping = getDataTablePorts(inputMapping);
        double nTables = tablePortMapping.size();
        try {
            if(tablePortMapping.size() > 0) loadTableInputs(connection, tablePortMapping, transferToExec.createSubProgress(nTables/nInput), chunkInSize);
        } catch (Throwable e) {
            throw new KnimeScriptingException("Failed to convert table node inputs into r workspace variables: " + e);
        }
        return inputMapping;
    }*/

	/**
	 * delivers a workspace file. As the nodes do only support one workspace input port, return the first workspace file
	 * @param inputMapping
	 * @return map with generic input ports only
	 */
	private File getWorkspaceFile(Map<String, Object> inputMapping) {

		for (String varName : inputMapping.keySet()) {
			Object curValue = inputMapping.get(varName);
			if (curValue instanceof File) {
				return (File) curValue;
			}

		}
		return null;
	}

	/**
	 * TODO: comment and check code
	 * @param inputMapping
	 * @return
	 */
	private Map<String, BufferedDataTable> getDataTablePorts(Map<String, Object> inputMapping) {
		TreeMap<String, BufferedDataTable> tablePortMapping = new TreeMap<String, BufferedDataTable>();

		for (String varName : inputMapping.keySet()) {
			Object curValue = inputMapping.get(varName);
			if (curValue instanceof BufferedDataTable) {

				tablePortMapping.put(varName, (BufferedDataTable) curValue);
			}

		}

		return tablePortMapping;
	}

	/**
	 * transfers KNIME tables to R
	 * @param connection
	 * @param tableMapping
	 * @param exec
	 * @param chunkInSize 
	 * @throws REXPMismatchException
	 * @throws RserveException
	 * @throws CanceledExecutionException
	 */
	private void loadTableInputs(RConnection connection, Map<String, BufferedDataTable> tableMapping, ExecutionMonitor exec, int chunkInSize) throws REXPMismatchException, RserveException, CanceledExecutionException {
		// transfer KNIME-tables to R
		for (String varName : tableMapping.keySet()) {
			BufferedDataTable input = tableMapping.get(varName);

			if (input == null) {
				throw new RuntimeException("null tables are not allowed in the table input mapping");
			}

			transferRDataContainer(exec.createSubProgress(1.0/tableMapping.size()), input, chunkInSize, connection, varName);           
		}
	}

	/**
	 * pushes one KNIME table to R in chunks
	 * @param exec				execution context
	 * @param bufTable			KNIME table
	 * @param colLimit			number of columns per chunk
	 * @param connection		R-connection
	 * @param parName			variable name in R
	 * @throws RserveException
	 * @throws REXPMismatchException
	 * @throws CanceledExecutionException
	 */
	public void transferRDataContainer(ExecutionMonitor exec, BufferedDataTable bufTable, int colLimit,
			RConnection connection, String parName) throws RserveException, REXPMismatchException, CanceledExecutionException {

		NodeLogger logger = NodeLogger.getLogger(RDataFrameContainer.class);

		DataTableSpec tSpec = bufTable.getDataTableSpec();
		int numRows = bufTable.getRowCount();
		int numCols = tSpec.getNumColumns();

		RDataFrameContainer rDFC = new RDataFrameContainer(numRows, numCols);

		// iterate over table columns; find the columns which can be pushed
		int chunkIdx = 0;
		int chunkCounter = 0;
		for(int colIdx = 0; colIdx < numCols; colIdx++) {
			DataColumnSpec cSpec = tSpec.getColumnSpec(colIdx);

			String cName = cSpec.getName();

			//check if column type is supported, then add to columns to pass
			RType type = getRType(cSpec.getType(), cSpec.getDomain().hasValues());
			if(type != null) {
				RDataColumn rCol = new RDataColumn(cName, type, colIdx);

				if(type.equals(RType.R_FACTOR)) {
					Set<DataCell> levels = new LinkedHashSet<DataCell>();
					levels = cSpec.getDomain().getValues();
					rCol.setLevels(levels);
				}   			
				rDFC.addColumnSpec(rCol, chunkIdx);

				chunkCounter ++;
				if(chunkCounter == colLimit || colIdx == (numCols - 1)) {
					chunkIdx ++;
					chunkCounter = 0;
				}
			} else {
				logger.info("Ommit column " + cName + "; data type not supported");
			}
		}

		// iterate over the chunks
		int nChunks = rDFC.getColumnChunks().size();
		for(int chunk : rDFC.getColumnChunks()) {

			// set sub execution context for this chunk
			ExecutionMonitor subExec = exec.createSubProgress(1.0/nChunks);
			subExec.setMessage("Chunk" + (chunk+1));  	

			// initialize data vectors
			rDFC.initDataVectors(chunk);    	

			// fill arrays with data
			int rowIdx = 0;
			for(DataRow row : bufTable) {
				if(chunk == 0) rDFC.addRowKey(rowIdx, row.getKey().getString());

				subExec.checkCanceled();
				subExec.setProgress(((double)rowIdx+1)/(double)numRows);
				subExec.setMessage("Row " + rowIdx + "(chunk " + (chunk+1) + "/ " + nChunks + ")");

				rDFC.addRowData(row, rowIdx, chunk);

				rowIdx ++;
			}
			if(!rDFC.hasRows()) subExec.setProgress(1);

			rDFC.pushChunk(chunk, connection, parName, subExec);
			rDFC.clearChunk(chunk);
		}

		// if table has no columns, store row-keys only
		if(nChunks == 0) {
			int rowIdx = 0;
			for(DataRow row : bufTable) {

				exec.checkCanceled();
				exec.setProgress(((double)rowIdx+1)/(double)numRows);
				exec.setMessage("Row " + rowIdx);

				rDFC.addRowKey(rowIdx, row.getKey().getString());
				rowIdx ++;
			}
			if(!rDFC.hasRows()) exec.setProgress(1);
		}

		exec.setMessage("Create R data frame (cannot be cancelled)");

		rDFC.createDataFrame(parName, connection);

		exec.setMessage("Successful transfer to R");
	}

	/**
	 * maps KNIME data type to RType
	 * @param dataType
	 * @param hasDomainValues
	 * @return
	 */
	public RType getRType(DataType dataType, boolean hasDomainValues) {
		// numeric values (order is important)
		if(dataType.isCompatible(BooleanValue.class)) return RType.R_LOGICAL;
		if(dataType.isCompatible(IntValue.class)) return RType.R_INT;
		if(dataType.isCompatible(DoubleValue.class)) return RType.R_DOUBLE;

		// string
		if(dataType.isCompatible(StringValue.class)) {
			if(hasDomainValues) return RType.R_FACTOR;
			else return RType.R_STRING;
		}

		return null;
	}

	/**
	 * maps R typeOf to RType
	 * @param typeOf
	 * @param isFactor
	 * @return
	 */
	public RType getRType(String typeOf, boolean isFactor) {
		if(typeOf.equals("double")) return RType.R_DOUBLE;
		if(typeOf.equals("character")) return RType.R_STRING;
		if(typeOf.equals("logical")) return RType.R_LOGICAL;
		if(typeOf.equals("integer")) {
			return isFactor ? RType.R_FACTOR : RType.R_INT;
		}

		return null;
	}

	/**
	 * if the input table contains a color model, it is pushed to R as a data frame 'knime.color.model'
	 * columns: 'value' and 'color'
	 * @param tSpec	input TableSpec
	 * @param con	R-serve connection
	 * @param exec	Execution context
	 * @param  varName 
	 * @throws KnimeScriptingException		if something went wrong pushing the data
	 */
	public void pushColorModelToR(DataTableSpec tSpec, RConnection con, ExecutionMonitor exec, String  varName) throws KnimeScriptingException {

		String nameInR = varName + ".color.model";

		int colorIdx = ColorModelUtils.getColorColumn(tSpec);

		// no color model column has been found
		if(colorIdx == -1) return;

		// data type of color model is not supported
		RType t = getRType(tSpec.getColumnSpec(colorIdx).getType(), false);
		if(t == null) return;

		exec.setMessage("Push color model to R (cannot be cancelled)");

		RDataColumn rC = new RDataColumn(tSpec.getColumnSpec(colorIdx).getName(), t, 0);
		HashMap<DataCell, Color> colorModel = null;

		// parse color model
		if(ColorModelUtils.isNumeric(tSpec, colorIdx)) {
			colorModel = ColorModelUtils.parseNumericColorModel(tSpec.getColumnSpec(colorIdx));
		} else {
			if(ColorModelUtils.isNominal(tSpec, colorIdx)) {
				colorModel = ColorModelUtils.parseNominalColorModel(tSpec.getColumnSpec(colorIdx));
			}
		}

		if(colorModel == null) return;

		rC.initDataVector(colorModel.size());

		// convert color model (color as hex-String and R-data-column with the associated values
		String[] colValues = new String[colorModel.size()];
		int i = 0;
		for(DataCell domVal : colorModel.keySet()) {
			rC.addData(domVal,i);
			Color col = colorModel.get(domVal);
			colValues[i] = String.format("#%02x%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
			i++;
		}

		// create color and value vectors for Rserve transfer
		RList l = new RList();
		l.put("value", rC.getREXPData());
		l.put("color", new REXPString(colValues));

		// push color model to R
		try {
			con.assign(nameInR, new REXPGenericVector(l));
			con.voidEval(nameInR + " <- as.data.frame(" + nameInR + ")");
		} catch (RserveException e) {
			throw new KnimeScriptingException("Failed to push nominal color model to R: " + e);
		}
		return;
	}

	/**
	 * if the input table contains a shape model, it is pushed to R as a data frame 'knime.shape.model'
	 * columns: 'value' and 'shape' and 'pch'
	 * @param tSpec
	 * @param con
	 * @param exec
	 * @param varName 
	 * @throws KnimeScriptingException
	 */
	public void pushShapeModelToR(DataTableSpec tSpec,
			RConnection con, ExecutionMonitor exec, String varName) throws KnimeScriptingException {

		String nameInR = varName + ".shape.model";

		int shapeIdx = ShapeModelUtils.getShapeColumn(tSpec);

		// no shape model column has been found
		if(shapeIdx == -1) return;

		// data type of color model is not supported
		RType t = getRType(tSpec.getColumnSpec(shapeIdx).getType(), false);
		if(t == null) return;

		exec.setMessage("Push shape model to R (cannot be cancelled)");

		// retrieve shape model
		HashMap<DataCell, Shape> shapeModel = null;		
		shapeModel = ShapeModelUtils.parseNominalShapeModel(tSpec.getColumnSpec(shapeIdx));

		assert shapeModel != null;

		// create R data vector
		RDataColumn rC = new RDataColumn(tSpec.getColumnSpec(shapeIdx).getName(), t, 0);
		rC.initDataVector(shapeModel.size());

		// convert shape model (shape as String and R-data-column with the associated values)
		String[] shapeValues = new String[shapeModel.size()];
		Integer[] shapePch = new Integer[shapeModel.size()];
		int i = 0;
		for(DataCell domVal : shapeModel.keySet()) {
			rC.addData(domVal,i);
			shapeValues[i] = shapeModel.get(domVal).toString();
			shapePch[i] = R_SHAPES.get(shapeValues[i]);
			i++;
		}

		// create shape and value vectors for Rserve transfer
		RList l = new RList();
		l.put("value", rC.getREXPData());
		l.put("shape", new REXPString(shapeValues));
		l.put("pch", new REXPInteger(ArrayUtils.toPrimitive(shapePch)));

		// push shape model to R
		try {
			con.assign(nameInR, new REXPGenericVector(l));
			con.voidEval(nameInR + " <- as.data.frame(" + nameInR + ")");
		} catch (RserveException e) {
			throw new KnimeScriptingException("Failed to push nominal shape model to R: " + e);
		}
		return;
	}

	/**
	 * if the input table contains a size model, it is pushed to R as a function 'knime.size.model.fun'
	 * @param tSpec
	 * @param con
	 * @param exec
	 * @param varName 
	 * @throws KnimeScriptingException
	 */
	public void pushSizeModelToR(DataTableSpec tSpec,
			RConnection con, ExecutionMonitor exec, String varName) throws KnimeScriptingException {
		String nameInR = varName + ".size.model.fun";

		int sizeIdx = SizeModelUtils.getSizeColumn(tSpec);

		// no shape model column has been found
		if(sizeIdx == -1) return;

		// data type of color model is not supported
		RType t = getRType(tSpec.getColumnSpec(sizeIdx).getType(), false);
		if(t == null) return;

		exec.setMessage("Push size model to R (cannot be cancelled)");

		// get KNIME size model
		ModelContent model = new ModelContent("Size"); 
		tSpec.getColumnSpec(sizeIdx).getSizeHandler().save(model);

		String sizeModelFunction = null;
		try {
			Config cfg = model.getConfig("size_model");
			double minv = cfg.getDouble("min");
			double maxv = cfg.getDouble("max");
			double fac = cfg.getDouble("factor");
			String method = cfg.getString("mapping");

			SizeModel sModel = new SizeModel(minv, maxv, fac, method);
			sizeModelFunction = getSizeModelFunction(sModel, nameInR);

		} catch (InvalidSettingsException e) {
			throw new KnimeScriptingException("KNIME size model does not contain expected keys. This is most likely due to implementation changes");
		}

		assert sizeModelFunction != null;

		// push size model function to R
		try {
			con.voidEval(sizeModelFunction);
		} catch (RserveException e) {
			throw new KnimeScriptingException("Failed to push numeric size model to R: " + e);
		}
		return;     	
	}

	/**
	 * creates the R function definition based on the given size model
	 * @param sModel sizeModel
	 * @return R function definition as String
	 */
	private String getSizeModelFunction(SizeModel sModel, String nameInR) {
		double min = sModel.getMin();
		double max = sModel.getMax();
		double factor = sModel.getFactor();
		if(sModel.getMethod().equals(Mapping.LINEAR))
			return nameInR + " <- function(v) {	(((v - " + min + ") / (" + max + " - " + min + ")) * (" + factor + " - 1)) + 1 }";
		if(sModel.getMethod().equals(Mapping.EXPONENTIAL))
			return nameInR + " <- function(v) {	(((v^2 - " + min + "^2) / (" + max + "^2 - " + min + "^2)) * (" + factor + " - 1)) + 1}";
		if(sModel.getMethod().equals(Mapping.LOGARITHMIC))
			return nameInR + " <- function(v) {	(((log(v) - log(" + min + ")) / (log(" + max + ") - log(" + min + "))) * (" + factor + " - 1)) + 1}";
		if(sModel.getMethod().equals(Mapping.SQUARE_ROOT))
			return nameInR + " <- function(v) {	(((sqrt(v) - sqrt(" + min + ")) / (sqrt(" + max + ") - sqrt(" + min + "))) * (" + factor + " - 1)) + 1}";
		return null;
	}

	public static void evalScript(RConnection connection, String fixedScript)
			throws RserveException, KnimeScriptingException,
			REXPMismatchException {

		REXP out;
		// evaluate script
		out = connection.eval("try({\n" + fixedScript + "\n}, silent = TRUE)");
		if( out.inherits("try-error"))
			throw new KnimeScriptingException("Error : " + out.asString());
	}

	/**
	 * check for syntax errors
	 * @param connection
	 * @param fixedScript
	 * @throws RserveException
	 * @throws KnimeScriptingException
	 * @throws REXPMismatchException
	 */
	public static void parseScript(RConnection connection, String fixedScript)
			throws RserveException, KnimeScriptingException,
			REXPMismatchException {
		REXP out;
		String rScriptVar = VAR_RKNIME_SCRIPT;
		connection.assign(rScriptVar, fixedScript);
		// parse script
		out = connection.eval("try(parse(text=" + rScriptVar + "), silent = TRUE)");
		if( out.inherits("try-error"))
			throw new KnimeScriptingException("Syntax error: " + out.asString());
	}

	/**
	 * use 'evaluate' package to execute the script and keep input+output+errors+warnings
	 * 
	 * @param fixedScript 	the R script
	 * @param connection	the connection to the R server
	 * @param logger		the node-logger to push warnings
	 * 
	 * @return	R list containing input, output, plots, (errors - not returned; throws exception instead) and warnings
	 * 
	 * @throws RserveException
	 * @throws KnimeScriptingException
	 * @throws REXPMismatchException
	 */
	public static REXPGenericVector evaluateScript(String fixedScript, RConnection connection) 
			throws RserveException, KnimeScriptingException, REXPMismatchException {

		// use 'evaluate' package to capture input+output+warnings+error
		// syntax errors are captured with try
		REXP r;

		// try to load evaluate package
		r = connection.eval("try(library(\"evaluate\"))");
		if (r.inherits("try-error")) 
			throw new KnimeScriptingException("Package 'evaluate' could not be loaded. \nTo run the script without, please turn off 'Evaluate script' in the node configuration dialog / preference settings?.");

		// try to evaluate script (fails with syntax errors)
		connection.assign(VAR_RKNIME_SCRIPT, fixedScript);
		r = connection.eval("knime.eval.obj <- evaluate("+ VAR_RKNIME_SCRIPT + ", new_device = FALSE)");

		// evaluation succeeded
		// check for errors
		int[] errIdx = ((REXPInteger) connection.eval("which(sapply(knime.eval.obj, inherits, \"error\") == TRUE, arr.ind = TRUE)")).asIntegers();
		if(errIdx.length > 0) {

			String firstError = "Error " + "(1/" + errIdx.length + "): ";
			REXPString error = (REXPString) connection.eval("knime.eval.obj[[" + errIdx[0] + "]]$message");
			firstError = firstError + error.asString() + "\n\tSee R-console view for further details";

			throw new KnimeScriptingException(firstError);
		}

		return (REXPGenericVector) (connection.eval("knime.eval.obj"));
	}

	/**
	 * input flow variables are pushed to R as knime.flow.in
	 * @param flowVariables
	 * @param con
	 * @param exec
	 * @throws KnimeScriptingException 
	 */
	public void pushFlowVariablesToR(Map<String, FlowVariable> flowVariables, ExecutionContext exec) throws KnimeScriptingException {
		
		assert m_con != null;

		RList l = new RList();

		exec.setMessage("Push KNIME flow variables to R (cannot be cancelled)");

		// put flow variables into an RList
		for(String flowVarName : flowVariables.keySet()) {
			FlowVariable flowvar = flowVariables.get(flowVarName);
			String name = flowvar.getName();
			REXP value = null;

			switch(flowvar.getType()){
			case STRING: 
				value = new REXPString(flowvar.getStringValue());
				break;
			case DOUBLE:
				value = new REXPDouble(flowvar.getDoubleValue());
				break;
			case INTEGER:
				value = new REXPInteger(flowvar.getIntValue());
				break;
			default:
				throw new KnimeScriptingException("Flow variable type '" + flowvar.getType() + "' is not yet supported");
			}

			if(value != null) l.put(name, value);
		}

		// push flow variables to R
		try {
			m_con.assign("knime.flow.in", new REXPGenericVector(l));
		} catch (RserveException e) {
			throw new KnimeScriptingException("Failed to push KNIME flow variables to R: " + e);
		}
	}

	public void openInR(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException {

		assert m_con != null;

		String rawScript = prepareScript();

		// save the work-space to a temporary file and open R
		File workspaceFile;
		try {
			workspaceFile = File.createTempFile("openInR_", ".RData");			
			RUtils.saveWorkspaceToFile(workspaceFile, m_con, RUtils.getHost());
			logger.info("Spawning R-instance ...");
			RUtils.openWSFileInR(workspaceFile, rawScript); 
		} catch (IOException | KnimeScriptingException e) {
			closeRConnection();
			throw new KnimeScriptingException("Failed to open in R\n" + e);
		}    
		exec.setProgress(1.0);
		closeRConnection();
	}


	/*    public void openInR(String rawScript, NodeLogger logger, RConnection connection) 
    		throws IOException, KnimeScriptingException {

    	// save the work-space to a temporary file and open R
    	File workspaceFile = File.createTempFile("openInR_", ".RData");    	
    	RUtils.saveWorkspaceToFile(workspaceFile, connection, RUtils.getHost());

    	logger.info("Spawning R-instance ...");
    	RUtils.openWSFileInR(workspaceFile, rawScript);           
    }*/

	/**
	 * this methods pulls the content from an R data frame and puts it into a BKNIME table
	 * 
	 * @param rOutName			look for such variable in R
	 * @param exec				subprogress-monitor
	 * @param execM				necessary to create new KNIME table
	 * @param chunkOutSize		how many rows at once?
	 * @return					KNIME table with content from R data frame
	 * @throws RserveException
	 * @throws REXPMismatchException
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable pullTableFromR(String rOutName, ExecutionMonitor exec, ExecutionContext execM, int chunkOutSize) 
			throws RserveException, REXPMismatchException, CanceledExecutionException {
		
		assert m_con != null;

		exec.setMessage("R snippet finished - pull data from R");

		int numRows = ((REXPInteger)m_con.eval("nrow(" + rOutName + ")")).asInteger();
		int numCols = ((REXPInteger)m_con.eval("ncol(" + rOutName + ")")).asInteger();
		// correct chunksize if it is -1 or 0 (no chunking)
		chunkOutSize = chunkOutSize <= 0 ? numRows : chunkOutSize;

		RDataFrameContainer rDFC = new RDataFrameContainer(numRows, numCols);

		//get row names
		String[] rowNames = new String[]{};
		exec.setMessage("retrieve row names from R (cannot be cancelled)");
		if(numRows > 0) rowNames = m_con.eval("rownames(" + rOutName + ")").asStrings();
		rDFC.addRowNames(rowNames);

		exec.setMessage("retrieve column specs from R (cannot be cancelled)");
		exec.checkCanceled();

		// get column specs
		if(numCols > 0) {
			//names(rOut)	column names
			String[] cNames = getDataFrameColumnNames(numCols, m_con, rOutName);
			//sapply(rOut, typeof)		storage mode
			String[] typeOf = getDataFrameColumnTypes(numCols, m_con, rOutName);	
			// sapply(rOut, is.factor)		is factor?
			boolean[] isFactor = ((REXPLogical)m_con.eval("sapply(" + rOutName + ", is.factor)")).isTRUE();

			// iterate over columns to get their data types
			for(int i = 0; i < numCols; i++) {		
				RType t = getRType(typeOf[i], isFactor[i]);
				RDataColumn rCol = new RDataColumn(cNames[i], t, i);
				exec.checkCanceled();
				// add level information
				if(rDFC.hasRows()) {
					if(t.equals(RType.R_FACTOR)) {
						String[] levels = ((REXPString)m_con.eval("levels(" + rOutName + "[," + (i+1) + "])")).asStrings();
						rCol.setLevels(levels);
					}
					// add information of lower / upper bounds
					if(t.equals(RType.R_DOUBLE) || t.equals(RType.R_INT) || t.equals(RType.R_LOGICAL)) {
						// range(rOut[,1],na.rm = TRUE)*1.0		returns min/max as doubles
						double[] bounds = ((REXPDouble) m_con.eval("range(" + rOutName + "[," + (i+1) + "], na.rm = TRUE)*1.0")).asDoubles();
						rCol.setBounds(bounds);
					}
				}


				rDFC.addColumnSpec(rCol, 0);		
			}
		}

		// create DataTableSpec from rDFC
		BufferedDataContainer con = execM.createDataContainer(rDFC.createDataTableSpec());

		// fill table with data
		exec.setMessage("retrieve data from R (cannot be cancelled)");
		exec.checkCanceled();

		if(numRows > 0) {
			rDFC.readDataFromR(con, m_con, exec, rOutName, chunkOutSize);
		}

		con.close();
		closeRConnection();
		return con.getTable();
	}

	private String[] getDataFrameColumnTypes(int numCols, RConnection connection, String rOutName) throws RserveException, REXPMismatchException {
		// ((REXPString)connection.eval("sapply(" + rOutName + ", typeof)")).asStrings();
		if(numCols == 1) {
			return new String[]{((REXPString)connection.eval("sapply(" + rOutName + ", typeof)")).asString()};
		} else {
			return ((REXPString)connection.eval("sapply(" + rOutName + ", typeof)")).asStrings();
		}
	}

	private String[] getDataFrameColumnNames(int numCols, RConnection connection, String rOutName) throws RserveException, REXPMismatchException {
		if(numCols == 1) {
			return new String[]{((REXPString)connection.eval("names(" + rOutName + ")")).asString()};
		} else {
			return ((REXPString)connection.eval("names(" + rOutName + ")")).asStrings();
		}
	}

	public static Image createImage(RConnection connection, String script, int width, int height, String device) 
			throws REngineException, RserveException, REXPMismatchException, KnimeScriptingException {

		// check preferences
		boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);

		// LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
		//script = supportOldVarNames(script);

		String tempFileName = "rmPlotFile." + device;

		String deviceArgs = device.equals("jpeg") ? "quality=97," : "";
		REXP xp = connection.eval("try(" + device + "('" + tempFileName + "'," + deviceArgs + " width = " + width + ", height = " + height + "))");

		if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
			// this is analogous to 'warnings', but for us it's sufficient to get just the 1st warning
			REXP w = connection.eval("if (exists('last.warning') && length(last.warning)>0) names(last.warning)[1] else 0");
			if (w.isString()) System.err.println(w.asString());
			throw new KnimeScriptingException("Can't open " + device + " graphics device:\n" + xp.asString());
		}

		// ok, so the device should be fine - let's plot - replace this by any plotting code you desire ...
		String preparedScript = AbstractScriptingNodeModel.fixEncoding(script);
		parseScript(connection, preparedScript);

		if(useEvaluate) {
			// parse and run script
			// evaluation list, can be used to create a console view
			evaluateScript(preparedScript, connection);

		} else {
			// parse and run script
			evalScript(connection, preparedScript);     	
		}

		// close the image
		connection.eval("dev.off();");
		// check if the plot file has been written
		int xpInt = connection.eval("file.access('" + tempFileName + "',0)").asInteger();
		if(xpInt == -1) throw new KnimeScriptingException("Plot could not be created. Please check your script");

		// we limit the file size to 1MB which should be sufficient and we delete the file as well
		xp = connection.eval("try({ binImage <- readBin('" + tempFileName + "','raw',2024*2024); unlink('" + tempFileName + "'); binImage })");

		if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
			throw new KnimeScriptingException(xp.asString());
		}

		// now this is pretty boring AWT stuff - create an image from the data and display it ...
		return Toolkit.getDefaultToolkit().createImage(xp.asBytes());
	}

	protected void runScript(ExecutionMonitor exec) throws KnimeScriptingException {
		
		assert m_con != null;
		
		// check preferences
    	boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);
		
    	
		exec.setMessage("Evaluate R-script (cannot be cancelled)");

        // PREPARE and parse script
        String script = prepareScript();
        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
        // stop support !
        //rawScript = RUtils.supportOldVarNames(rawScript);   
        try {
			parseScript(m_con, script);
		} catch (RserveException | KnimeScriptingException | REXPMismatchException e) {
			closeRConnection();
			throw new KnimeScriptingException("Failed to parse the script:\n" + e.getMessage());
		}

        try {
        	// EVALUATE script
        	if(useEvaluate) {
        		// parse and run script
        		// evaluation list, can be used to create a console view, throws first R-error-message

        		REXPGenericVector knimeEvalObj = evaluateScript(script, m_con);

        		// check for warnings
        		ArrayList<String> warningMessages = RUtils.checkForWarnings(m_con);
        		if(warningMessages.size() > 0) setWarningMessage("R-script produced " + warningMessages.size() + " warnings. See R-console view for further details");


        	} else {
        		// parse and run script
        		evalScript(m_con, script);     	
        	}
        } catch (RserveException | REXPMismatchException e) {
        	closeRConnection();
        	throw new KnimeScriptingException("Failed to evaluate the script:\n" + e.getMessage());
        }
	}
}
