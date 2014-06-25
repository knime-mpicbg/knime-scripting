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
}