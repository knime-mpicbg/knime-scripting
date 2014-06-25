package de.mpicbg.knime.knutils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class FileUtils {

    /**
     * Recursively parses a directory and put all files in the return list which match ALL given filters.
     */
    public static List<File> findFiles(File directory) {
        return findFiles(directory, true);
    }


    /**
     * Collects all files list which match ALL given filters.
     *
     * @param directory   the base directory for the search
     * @param beRecursive
     * @return
     */
    public static List<File> findFiles(File directory, boolean beRecursive) {
        assert directory.isDirectory();

        List<File> allFiles = new ArrayList<File>();

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (beRecursive)
                    allFiles.addAll(findFiles(file));
            } else {
                if (!file.isHidden()) {
                    allFiles.add(file);
                }
            }
        }

        Collections.sort(allFiles);

        return allFiles;
    }
}
