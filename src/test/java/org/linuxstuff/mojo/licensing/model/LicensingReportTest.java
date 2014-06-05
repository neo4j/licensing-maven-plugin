package org.linuxstuff.mojo.licensing.model;

import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class LicensingReportTest {


    @Parameterized.Parameters
    public static Iterable<Object[]> data() {

        final List<Object[]> result = new ArrayList<Object[]>();

        List<String> nls = Arrays.asList("\r", "\n", "\r\n");

        for (String aNL : nls) {
            result.add(new Object[]{"aaa" + aNL + "bbb", aNL});

        }

        return result;
    }

    private final String input;
    private final String nl;
    private String originalNL;

    public LicensingReportTest(String input, String nl) {
        this.input = input;
        this.nl = nl;
    }

    @Before
    public void setUp() throws Exception {
        originalNL = System.getProperty("line.separator");
    }

    @Test
    public void testWriteToFile() throws Exception {

        File file = File.createTempFile("test", ".txt");
        file.deleteOnExit();

        FileWriter fileWriter = new FileWriter(file);
        IOUtil.copy(input, fileWriter);
        IOUtil.close(fileWriter);

        StringWriter stringWriter = new StringWriter();

        System.setProperty("line.separator", nl);
        LicensingReport.writeToFile(file, new PrintWriter(stringWriter));
        String output = stringWriter.toString();

        String expected = input + nl + nl;
        assertEquals(expected, output);
    }

    @After
    public void tearDown() throws Exception {
        System.setProperty("line.separator", originalNL);
    }
}