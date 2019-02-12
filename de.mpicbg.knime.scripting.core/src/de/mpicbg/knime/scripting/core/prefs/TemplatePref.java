package de.mpicbg.knime.scripting.core.prefs;

/**
 * Created by IntelliJ IDEA.
 * User: niederle
 * Date: 6/28/11
 * Time: 1:32 PM
 * <p/>
 * Class stores a template URL or file locator and whether it is active or not
 */
public class TemplatePref extends Object {

	/* template file location */
	private String uri;
	/* true, if template file should be used */
    private boolean active;

    /**
     * constructor
     */
    public TemplatePref() {
        this.uri = null;
        this.active = true;
    }

    /**
     * constructor
     * 
     * @param uri		template file location
     * @param active	is template active?
     */
    public TemplatePref(String uri, boolean active) {
        this.uri = uri;
        this.active = active;
    }

    /**
     * @return template file location
     */
    public String getUri() {
        return uri;
    }

    /**
     * set template file location
     * @param uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * 
     * @return	true, if template is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * changes active flag
     * 
     * @param active
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public int hashCode() {
    	int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + Boolean.hashCode(active);
        return result;
	}

    /**
     * template prefs will be equal if URI and active status are comparable
     * 
     * {@inheritDoc}
     */
	@Override
	public boolean equals(Object obj) {
		
		if (this == obj) return true;
	    if (!(obj instanceof TemplatePref)) return false;
		
	    TemplatePref toCompare = (TemplatePref) obj;
	    return uri.equals(toCompare.getUri()) && active == toCompare.isActive();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return uri;
	}
}