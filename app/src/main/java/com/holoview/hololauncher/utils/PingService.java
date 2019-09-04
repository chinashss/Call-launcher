package com.holoview.hololauncher.utils;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingService {

    public static int ping(String host, int pingCount) {
        String line = null;
        int delay = -1;
        Process process = null;
        BufferedReader successReader = null;
        String command = "ping -c " + pingCount + " " + host;
        try {
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                return -1;
            }
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while ((line = successReader.readLine()) != null) {
                if (line.contains("avg")) {
                    int i = line.indexOf("/", 20);
                    int j = line.indexOf(".", i);
                    delay =Integer.parseInt(line.substring(i + 1, j)) ;
                }
            }
            int status = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (successReader != null) {
                try {
                    successReader.close();
                } catch (IOException e) {
                  e.printStackTrace();
                }
            }
        }
        return delay;
    }

    private static void append(StringBuffer stringBuffer, String text) {
        if (stringBuffer != null) {
            stringBuffer.append(text + "\n");
        }
    }

}
