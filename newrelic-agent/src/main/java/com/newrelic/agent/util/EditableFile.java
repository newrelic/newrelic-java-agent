/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a convenient wrapper API for editing files.
 */
public class EditableFile {

    String filePath;
    String fileAsString;
    public final String comment;
    static final String lineSep = System.getProperty("line.separator");

    // static final String fileSep = System.getProperty("file.separator");

    public EditableFile(String filestr) throws NullPointerException, FileNotFoundException, IOException {

        // Validate input arg
        if (filestr == null || filestr.equals("")) {
            throw new NullPointerException("A null or empty string can't become an EditableFile.");
        }

        filePath = filestr;
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File " + filePath + " does not exist, so it can't become an EditableFile.");
        }

        // Read file into string
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str + lineSep);
            }
            in.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.getMessage();
            fnfe.printStackTrace();
            throw new FileNotFoundException("Couldn't create EditableFile due to FileNotFoundException");
        } catch (IOException e) {
            e.getMessage();
            e.printStackTrace();
            throw new IOException("Couldn't create EditableFile due to IOException");
        }

        fileAsString = sb.toString();

        // Choose comment char(s)
        if (filePath.endsWith(".bat")) {
            comment = "::";
        } else if (filePath.endsWith(".java")) {
            comment = "//";
        } else {
            comment = "#";
        }
    }

    /**
     * Returns the file contents in string format.
     */
    public String getContents() {

        return fileAsString;

    }

    /**
     * Returns the path to the file we are editing.
     */
    public String getLocation() {

        return filePath;

    }

    /**
     * Returns true if the file contains the specified regex, false otherwise.
     * 
     * Regex metacharacters (dot, backslash, etc) will be interpreted as such unless they are escaped.
     * 
     * For example, the regex "file.txt" will be found in "C:\\file.txt", but also "C:\\file-txt", "C:\\fileNtxt", etc.
     * 
     * And this regex: "C:\\\\Some.*" will be found in "C:\\Some\\Arbitrary\\Windows\\Path\\File.txt", while the regex
     * "C:\\Some.*" will not be found.
     */
    public boolean contains(String regex) {
        if (fileAsString != null) {
            Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = p.matcher(fileAsString);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Modifies the file to replace the first occurrence of the specified regex with the specified replacement string.
     * 
     * Replacement is multiline, so ^ and $ can match the start and end of individual lines, or the start/end of the
     * entire expression.
     * 
     * The regex string must escape regex special characters for them to be interpreted as literals.
     * 
     * The replacement string must escape dollar signs for them to be interpreted as literals. Otherwise, dollar signs
     * will be interpreted as backreferences.
     */
    public String replaceFirst(String regex, String replacement) {
        return replaceFirst(regex, replacement, true);
    }

    /**
     * Modifies the file to replace the first occurrence of the specified regex with the specified replacement string.
     * 
     * Set multiline to true to have the ^ and $ metachars match the start and end of individual lines. Otherwise, they
     * will only match the start/end of the entire expression.
     * 
     * The regex string must escape regex special characters for them to be interpreted as literals.
     * 
     * The replacement string must escape dollar signs for them to be interpreted as literals. Otherwise, dollar signs
     * will be interpreted as backreferences.
     */
    public String replaceFirst(String regex, String replacement, boolean isMultiLine) {
        Pattern p;
        if (isMultiLine) {
            p = Pattern.compile(regex, Pattern.MULTILINE);
        } else {
            p = Pattern.compile(regex);
        }
        Matcher m = p.matcher(fileAsString);
        fileAsString = m.replaceFirst(replacement);
        write();
        return fileAsString;
    }

    /**
     * Modifies the file to replace all occurrences of the specified regex with the specified replacement string.
     * 
     * Same regex special character handling as {@link #replaceFirst(String, String)}.
     */
    public String replaceAll(String regex, String replacement) {
        return replaceAll(regex, replacement, true);
    }

    /**
     * Modifies the file to replace all occurrences of the specified regex with the specified replacement string.
     * 
     * Same regex special character handling as {@link #replaceFirst(String, String, boolean)}.
     */
    public String replaceAll(String regex, String replacement, boolean isMultiLine) {
        Pattern p;
        if (isMultiLine) {
            p = Pattern.compile(regex, Pattern.MULTILINE);
        } else {
            p = Pattern.compile(regex);
        }
        Matcher m = p.matcher(fileAsString);
        fileAsString = m.replaceAll(replacement);
        write();
        return fileAsString;
    }

    /**
     * Modifies the file to insert the specified text on the line before the *first* instance of the locator statement.
     * 
     * Set multiline to true to have the ^ and $ metachars match the start and end of individual lines. Otherwise, they
     * will only match the start/end of the entire expression.
     */
    public String insertBeforeLocator(String regex, String textToInsert, boolean isMultiLine) {
        fileAsString = this.replaceFirst("(" + regex + ")", textToInsert + lineSep + "$1", isMultiLine);
        write();
        return fileAsString;

    }

    /**
     * Modifies the file to insert the specified text on the line after the *first* instance of the locator statement.
     * 
     * Set multiline to true to have the ^ and $ metachars match the start and end of individual lines. Otherwise, they
     * will only match the start/end of the entire expression.
     */
    public String insertAfterLocator(String regex, String textToInsert, boolean isMultiLine) {
        fileAsString = this.replaceFirst("(" + regex + ")", "$1" + lineSep + textToInsert, isMultiLine);
        write();
        return fileAsString;

    }

    /**
     * Comments out first line matching the specified regex.
     */
    public void commentOutFirstLineMatching(String regex) {
        this.replaceFirst("(" + regex + ")", comment + "$1");

    }

    /**
     * Comments out all lines matching the specified regex.
     */
    public void commentOutAllLinesMatching(String regex) {
        this.replaceAll("(" + regex + ")", comment + "$1");

    }

    /**
     * Appends the specified text to the file.
     */
    public void append(String text) {
        fileAsString += lineSep + text;
        write();
    }

    /**
     * Write pending changes to a backup file (the original file name with a timestamp appended).
     * 
     * @return The name of the backup file, or an empty string if the file could not be written successfully.
     */
    public String backup() {
        DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String filename = filePath + "." + df.format(new Date());
        if (write(filename)) {
            return filename;
        } else {
            return "";
        }

    }

    /**
     * Write pending changes to the original file.
     */
    private boolean write() {
        return write(filePath);
    }

    /**
     * Write pending changes to an arbitrary location.
     * 
     * @return true if the write succeeded, false otherwise.
     */
    private boolean write(String pathToFile) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(pathToFile));
            out.write(fileAsString);
            out.close();
            return true;
        } catch (IOException e) {
            System.out.println("Problem writing file to disk");
            e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

}
