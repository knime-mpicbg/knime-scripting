package de.mpicbg.knime.scripting.core.prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: niederle
 * Date: 7/6/11
 * Time: 10:40 AM
 * Class handles the string format for template preferences (parse + create)
 */
public class TemplatePrefString {

    private static final String DEFAULT_SEPERATOR = ";";
    private String prefString;

    public TemplatePrefString(String prefString) {
        this.prefString = prefString;
    }

    public String getPrefString() {
        return prefString;
    }

    public TemplatePrefString(List<TemplatePref> prefList) {
        this.prefString = createPrefString(prefList);
    }

    public List<TemplatePref> parsePrefString() {
        List<String> entries = new ArrayList<String>();
        List<TemplatePref> tList = new ArrayList<TemplatePref>();
        StringTokenizer st =
                new StringTokenizer(prefString, DEFAULT_SEPERATOR);

        // get entries separated by delimiter
        while (st.hasMoreElements()) {
            entries.add(st.nextToken());
        }

        // Pattern example: ("http://www.test.de/test.txt",true)  - contains url and if active or not
        Pattern pattern = Pattern.compile("^\\(\"(.*)\",(.*)\\)$");

        for (String entry : entries) {
            TemplatePref templateEntry = new TemplatePref();
            Matcher pMatch = pattern.matcher(entry);
            if (pMatch.matches()) {
                templateEntry.setUri(pMatch.group(1));
                templateEntry.setActive(Boolean.parseBoolean(pMatch.group(2)));
            } else {
                templateEntry.setUri(entry);
                templateEntry.setActive(true);
            }
            tList.add(templateEntry);
        }
        return tList;
    }

    public String createPrefString(List<TemplatePref> prefList) {
        StringBuilder prefString = new StringBuilder();

        boolean firstEntry = true;
        for (TemplatePref pref : prefList) {
            if (!firstEntry) prefString.append(DEFAULT_SEPERATOR);
            prefString.append("(\"");
            prefString.append(pref.getUri());
            prefString.append("\",");
            prefString.append(((Boolean) pref.isActive()).toString());
            prefString.append(")");
            firstEntry = false;
        }

        return prefString.toString();
    }
}
