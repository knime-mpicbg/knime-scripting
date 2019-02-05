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

	private String uri;
    private boolean active;

    public TemplatePref() {
        this.uri = null;
        this.active = true;
    }

    public TemplatePref(String uri, boolean active) {
        this.uri = uri;
        this.active = active;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
	public int hashCode() {
    	int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + Boolean.hashCode(active);
        return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TemplatePref){
	        TemplatePref toCompare = (TemplatePref) obj;
	        return uri.equals(toCompare.getUri()) && active == toCompare.isActive();
	    }
	    return false;
	}

	@Override
	public String toString() {
		return uri;
	}
}