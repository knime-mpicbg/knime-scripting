package de.mpicbg.knime.scripting.core;

import java.nio.file.Path;

public class ScriptingOpenInDialog extends ScriptingNodeDialog {

	@Override
	public String getTemplatesFromPreferences() {
		return null;
	}

	@Override
	protected Path getTemplateCachePath() {
		return null;
	}

}
