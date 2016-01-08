package de.mpicbg.knime.scripting.core.rgg;

import org.knime.core.data.*;
import org.knime.core.data.StringValue;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class TemplateUtils {

    public static final String RGG_NUM_PARNAMES = "$$$NUM_ATTRIBUTES$$$";
    public static final String RGG_STR_PARNAMES = "$$$STR_ATTRIBUTES$$$";
    public static final String RGG_ALL_PARNAMES = "$$$ALL_ATTRIBUTES$$$";
    public static final String RGG_FLOWVARS = "$$$FLOWVARS$$$";

    // Outdated definitions, kept only for backward compatibility
    public static final String RGG_CAT_PARNAMES = "$$$FACTORS$$$";
    public static final String RGG_PARNAMES = "$$$PARNAMES$$$";

    // example: $$$DOMAIN('treatment')$$$
    public static final String RGG_DOMAIN = "[$]{3}DOMAIN[(]'([\\w\\d_ ]*)'[)][$]{3}";


    public static String concatParNames(List<DataColumnSpec> columnSpecs) {
        StringBuffer sb = new StringBuffer();

        for (DataColumnSpec cspec : columnSpecs) {

            sb.append(cspec.getName());
            sb.append(",");
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }


    private static String concatNumParNames(List<DataColumnSpec> columnSpecs) {
        StringBuffer sb = new StringBuffer();

        for (DataColumnSpec cspec : columnSpecs) {

            DataType type = cspec.getType();

            if (type.isCompatible(IntValue.class)) {
                sb.append(cspec.getName());
                sb.append(",");
            } else if (type.isCompatible(DoubleValue.class)) {
                sb.append(cspec.getName());
                sb.append(",");
            }
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }


    public static String concatCatParNames(List<DataColumnSpec> columnSpecs) {
        StringBuffer sb = new StringBuffer();

        for (DataColumnSpec cspec : columnSpecs) {

            DataType type = cspec.getType();

            if (type.isCompatible(IntValue.class)) {
                sb.append(cspec.getName());
                sb.append(",");
            } else if (type.isCompatible(StringValue.class)) {
                sb.append(cspec.getName());
                sb.append(",");
            }
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * replace RGG-placeholder with input specific data
     * @param templateText
     * @param inputAttributes
     * @return adapted template
     */
    public static String replaceRGGPlaceholders(String templateText, Map<Integer, List<DataColumnSpec>> inputAttributes) {
        if (templateText.contains(RGG_NUM_PARNAMES))
            templateText = templateText.replace(RGG_NUM_PARNAMES, concatNumParNames(inputAttributes.get(0)));
        if (templateText.contains(RGG_STR_PARNAMES))
            templateText = templateText.replace(RGG_STR_PARNAMES, concatCatParNames(inputAttributes.get(0)));
        if (templateText.contains(RGG_ALL_PARNAMES))
            templateText = templateText.replace(RGG_ALL_PARNAMES, concatParNames(inputAttributes.get(0)));

        // support outdated definitions
        if (templateText.contains(RGG_CAT_PARNAMES))
            templateText = templateText.replace(RGG_CAT_PARNAMES, concatCatParNames(inputAttributes.get(0)));
        if (templateText.contains(RGG_PARNAMES))
            templateText = templateText.replace(RGG_PARNAMES, concatParNames(inputAttributes.get(0)));
        
        // check if DOMAIN placeholder has to be replaced
        // TODO: enable for two or more inputs
        Pattern pattern = Pattern.compile(RGG_DOMAIN);
        Matcher matcher = pattern.matcher(templateText);
        if (matcher.find()) 
        	templateText = replaceDomains(templateText, inputAttributes.get(0));
        
        // done, if there is only a single input
        if(inputAttributes.size() == 1)
        	return templateText;

        // now replace the parameters for scripting nodes that have several inputs and need to distinguish between those
        for (int inputIndex = 0; inputIndex < 2; inputIndex++) {
        	
            List<DataColumnSpec> tableInputModel = inputAttributes.get(inputIndex);
            int enduserIndex = inputIndex + 1;

            if (templateText.contains(createInputSpecificVarPattern(enduserIndex, RGG_NUM_PARNAMES)))
                templateText = templateText.replace(createInputSpecificVarPattern(enduserIndex, RGG_NUM_PARNAMES), concatNumParNames(tableInputModel));
            if (templateText.contains(createInputSpecificVarPattern(enduserIndex, RGG_STR_PARNAMES)))
                templateText = templateText.replace(createInputSpecificVarPattern(enduserIndex, RGG_STR_PARNAMES), concatCatParNames(tableInputModel));
            if (templateText.contains(createInputSpecificVarPattern(enduserIndex, RGG_ALL_PARNAMES)))
                templateText = templateText.replace(createInputSpecificVarPattern(enduserIndex, RGG_ALL_PARNAMES), concatParNames(tableInputModel));

            if (templateText.contains(createInputSpecificVarPattern(enduserIndex, RGG_CAT_PARNAMES)))
                templateText = templateText.replace(createInputSpecificVarPattern(enduserIndex, RGG_CAT_PARNAMES), concatCatParNames(tableInputModel));
            if (templateText.contains(createInputSpecificVarPattern(enduserIndex, RGG_PARNAMES)))
                templateText = templateText.replace(createInputSpecificVarPattern(enduserIndex, RGG_PARNAMES), concatParNames(tableInputModel));
        }

        return templateText;
    }

    /**
     * returns rgg-placeholder for multiple inputs
     * like $$$NUM_ATTRIBUTES_1$$$
     * note: replacement is a bit fishy but works as long as placeholder defaults end with 'S$$$'
     * @param enduserIndex - input table index starting with 1
     * @param basePattern - default placeholder string
     * @return adapted placeholder string
     */
    private static String createInputSpecificVarPattern(int enduserIndex, String basePattern) {
        return basePattern.replace("S$", "S_" + enduserIndex + "$");
    }


    private static String replaceDomains(String templateText, List<DataColumnSpec> attributeListModel) {
        // create the matcher from the regex and the given templateText
        Matcher matcher = Pattern.compile(RGG_DOMAIN).matcher(templateText);
        
        StringBuffer stringBuffer = new StringBuffer();
        
        while(matcher.find()){
        	String attributeName = matcher.group(1);
            matcher.appendReplacement(stringBuffer, concatDomain(attributeListModel, attributeName).toString());
        }
        matcher.appendTail(stringBuffer);

        return stringBuffer.toString();
    }


    private static CharSequence concatDomain(List<DataColumnSpec> attributeListModel, String attributeName) {
        // first try to find the parameter

        for (DataColumnSpec cspec : attributeListModel) {
            if (!cspec.getName().equals(attributeName)) {
                continue;
            }

            // we found it, so let's check its domain

            Set<DataCell> domain = cspec.getDomain().getValues();
            if (domain == null) {
                return "Error: There's no domain-information for attribute: " + attributeName + ",Use the domain-calculator to recalculate it!";
            }

            StringBuffer sb = new StringBuffer();

            // convert to string
            for (DataCell facLevel : domain) {
                sb.append(facLevel);
                sb.append(",");
            }

            // remove the trailing comma
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }

        return "Error: Could not find attribute: " + attributeName + ",You need to fix your template!!";
    }


    public static void main(String[] args) {
        String testTemplate = "$$$DOMAIN('treatment')$$$  $$$DOMAIN('baum')$$$";

        Matcher matcher = Pattern.compile(RGG_DOMAIN).matcher(testTemplate);
        while (matcher.find()) {
            String matchResult = testTemplate.substring(matcher.start(), matcher.end());
            System.err.println("match=" + matchResult);
            System.err.println("matchgroup=" + matcher.group(1));

        }

        boolean b = testTemplate.matches(RGG_DOMAIN);

        System.err.println("" + b);
    }


    public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        try {
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                String line;

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                } finally {
                    is.close();
                }
                return sb.toString();
            } else {
                return "";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Reads a resource file into a string object.
     *
     * @param packageRefObject An arbitrary object which is used as package reference for the file to be read
     * @param resourceFileName The name of the file without any package prefix (Example: 'drcutils.R')
     */
    public static String readResourceAsString(Object packageRefObject, String resourceFileName) {
        InputStream is = packageRefObject.getClass().getResourceAsStream(resourceFileName);

        return convertStreamToString(is);
    }


    public static void writeStreamToFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16384];
        while (true) {
            int count = in.read(buffer);
            if (count < 0)
                break;
            out.write(buffer, 0, count);
        }
        in.close();
        out.close();
    }


//    public static String fileNameTrunk(String str) {
//        File tmp = new File(str);
//        str = tmp.getName();
//        int index = str.indexOf(".");
//        if (index > 1) {
//            str = str.substring(0, index);
//        }
//        return str;
//    }


}
