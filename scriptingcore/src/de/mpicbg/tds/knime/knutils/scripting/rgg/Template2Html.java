package de.mpicbg.tds.knime.knutils.scripting.rgg;

import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplateWizard;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Template2Html {

    public static void main(String[] args) throws IOException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new URL("http://idisk.mpi-cbg.de/~brandl/scripttemplates/screenmining/R/figure-templates.txt"));
        urls.add(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Matlab/figure-templates.txt"));
        urls.add(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Python/figure-templates.txt"));
//


        List<ScriptTemplate> scriptTemplates = new ArrayList<ScriptTemplate>();
        for (URL url : urls) {
            scriptTemplates.addAll(ScriptTemplateWizard.parseTemplateFile(url));
        }

        System.err.println(scriptTemplates.toString());

        File galleryFile = File.createTempFile("templateGallery", ".html");

        FileWriter outFile = new FileWriter(galleryFile);
        PrintWriter out = new PrintWriter(outFile);


        out.print("<html>\n" +
                "<body>\n" +
                "\n" +
                "<h1>My First Heading</h1>\n" +
                "\n" +
                "<p>Hallo Antje.</p>\n");

        out.print("\n" +
                "</body>\n" +
                "</html>");

        out.flush();
        out.close();

        Desktop.getDesktop().edit(galleryFile);

    }


}
