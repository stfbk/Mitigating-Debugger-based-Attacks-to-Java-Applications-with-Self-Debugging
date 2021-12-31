package org.company.xml;

import java.io.*;

/**
 * util methods to parse XML files
 */
public class XMLUtil {

    /**
     * Remove the given unicode character from the file and replace that char with empty string ""
     * @param xmlFileToParse the XML file to parse from which the character will be removed
     * @param unicodeCharacterToRemove the character to remove
     * @throws IOException if the file was not found or there was an exception while opening the file
     */
    public static void removeUnicodeCharacter(File xmlFileToParse, String unicodeCharacterToRemove) throws IOException {

        BufferedReader xmlFileToModify = new BufferedReader(new FileReader(xmlFileToParse));
        StringBuilder inputBuffer = new StringBuilder();
        String currentLine;

        while ((currentLine = xmlFileToModify.readLine()) != null) {
            if (currentLine.contains(unicodeCharacterToRemove)) {
                currentLine = currentLine.replace(unicodeCharacterToRemove, "");
            }
            inputBuffer.append(currentLine).append('\n');
        }
        xmlFileToModify.close();

        // write the new string with the replaced line OVER the same file
        FileOutputStream newXMLFile = new FileOutputStream(xmlFileToParse);
        newXMLFile.write(inputBuffer.toString().getBytes());
        newXMLFile.close();
    }
}
