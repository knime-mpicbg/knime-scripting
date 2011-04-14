package de.mpicbg.tds.knime.knutils.scripting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class FlowVarUtils {

    public static final String FLOW_VAR_PATTERN = "FLOWVAR[(]([\\w\\d_ ]*)[)]";


    public static String replaceFlowVars(String text, AbstractScriptingNodeModel nodeModel) {

//        FlowObjectStack flowObjectStack = org.knime.core.node.FlowVarUtils.getStack(nodeModel);
//        Map<String, FlowVariable> stack = flowObjectStack.getAvailableFlowVariables();


        // create the matcher from the regex and the given templateText

        while (text.contains("FLOWVAR")) {
            Matcher matcher = Pattern.compile(FLOW_VAR_PATTERN).matcher(text);
            if (!matcher.find()) {
                // this may happen if the FLOWVAR char is there but does not comply to the expected pattern
                throw new RuntimeException("Incorrect use flow variables. Correct example: FLOWVAR(treatment)");
            }

            String matchResult = text.substring(matcher.start(), matcher.end());
//            System.err.println("match=" + matchResult);
            String flowVarName = matcher.group(1);
//            System.err.println("matchgroup=" + flowVarName);

            String flowVarValue = nodeModel.getFlowVariable(flowVarName);
            if (flowVarValue == null) {
                throw new RuntimeException("Could not replace (missing?) flow-variable : " + flowVarName);
            }

            text = text.replace(matchResult, flowVarValue);
        }

        return text;
    }
}
