package com.precisionguessworks.frc;

import com.sun.squawk.microedition.io.FileConnection;
import com.sun.squawk.util.Arrays;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import javax.microedition.io.Connector;

public class DataFile {
    private final PrintStream file;

    public DataFile(String suffix) throws IOException {
        Calendar cal = Calendar.getInstance();

        String year = new Integer(cal.get(Calendar.YEAR)).toString();
        String month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
        String date = new Integer(cal.get(Calendar.DATE)).toString();
        String hour = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
        String minute = new Integer(cal.get(Calendar.MINUTE)).toString();
        String second = new Integer(cal.get(Calendar.SECOND)).toString();
        String name =
                year +
                DataFile.paddingString(month, 2, '0', true) +
                DataFile.paddingString(date, 2, '0', true) + "-" +
                DataFile.paddingString(hour, 2, '0', true) +
                DataFile.paddingString(minute, 2, '0', true) +
                DataFile.paddingString(second, 2, '0', true) + "_" +
                suffix +
                ".csv";

        FileConnection connection = (FileConnection) Connector.open("file:///" + name, Connector.WRITE);
        connection.create();

        this.file = new PrintStream(connection.openDataOutputStream());

        System.out.println("Writing sensor data to: " + name);
    }

    public void writeln(String line) {
        this.file.println(line);
    }

    private static String paddingString(String s, int n, char c, boolean paddingLeft) {
        if (s == null) {
            return s;
        }

        int add = n - s.length(); // may overflow int size... should not be a problem in real life
        if(add <= 0) {
            return s;
        }

        StringBuffer str = new StringBuffer(s);
        char[] ch = new char[add];
        Arrays.fill(ch, c);

        if(paddingLeft){
            str.insert(0, ch);
        } else {
            str.append(ch);
        }

        return str.toString();
    }
}