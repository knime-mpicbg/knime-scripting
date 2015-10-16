package de.mpicbg.knime.knutils.data.property;

/**
 * represents the KNIME {@link org.knime.core.data.property.SizeModelDouble}
 * necessary as the model cannot be accessed from outside
 * @author Antje Janosch
 *
 */
public class SizeModel {
	private double m_min;
	private double m_max;
	private Mapping m_method;
	private double m_factor;
	
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

	public Mapping getMethod() {
		return m_method;
	}

	public double getMin() {
		return m_min;
	}
	
	public double getMax() {
		return m_max;
	}
	
	public double getFactor() {
		return m_factor;
	}
}
