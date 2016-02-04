package de.mpicbg.knime.knutils.data.property;

/**
 * represents the KNIME {@link org.knime.core.data.property.SizeModelDouble}
 * necessary as the model cannot be accessed from outside
 * @author Antje Janosch
 *
 */
public class SizeModel {
	/** minimum value set to size = 1 */
	private double m_min;
	/** maximum value set to size = m_factor */
	private double m_max;
	/** method to calculate the size between minimum and maximum value */
	private Mapping m_method;
	/** size factor */
	private double m_factor;
	
	/** Methods available for size calculation */
	public enum Mapping {
		/** for math see {@link org.knime.core.data.property.SizeModelDouble.Mapping.LINEAR} */
		LINEAR,
		/** for math see {@link org.knime.core.data.property.SizeModelDouble.Mapping.SQUARE_ROOT} */
		SQUARE_ROOT,
		/** for math see {@link org.knime.core.data.property.SizeModelDouble.Mapping.LOGARITHMIC} */
		LOGARITHMIC,
		/** for math see {@link org.knime.core.data.property.SizeModelDouble.Mapping.EXPONENTIAL} */
		EXPONENTIAL
	}
	
	/**
	 * constructor
	 * @param min - {@link SizeModel#m_min}
	 * @param max - {@link SizeModel#m_max}
	 * @param factor - {@link SizeModel#m_factor}
	 * @param method - as String: {@link SizeModel#m_method}
	 */
	public SizeModel(double min, double max, double factor, String method) {
		m_min = min;
		m_max = max;
		m_factor = factor;
		m_method = null;
		
		if(method.equals(Mapping.LINEAR.name())) m_method = Mapping.LINEAR;
		if(method.equals(Mapping.LOGARITHMIC.name())) m_method = Mapping.LOGARITHMIC;
		if(method.equals(Mapping.EXPONENTIAL.name())) m_method = Mapping.EXPONENTIAL;
		if(method.equals(Mapping.SQUARE_ROOT.name())) m_method = Mapping.SQUARE_ROOT;
	}

	/** return {@link SizeModel#m_method} */
	public Mapping getMethod() {
		return m_method;
	}

	/** return {@link SizeModel#m_min} */
	public double getMin() {
		return m_min;
	}
	
	/** return {@link SizeModel#m_max} */
	public double getMax() {
		return m_max;
	}
	
	/** return {@link SizeModel#m_factor} */
	public double getFactor() {
		return m_factor;
	}
}
