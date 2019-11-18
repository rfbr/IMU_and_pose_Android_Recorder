package com.example.imurecorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//This function is used to write in csv format the following data:
// - The timestamp, 3D acceleration and angular velocity processed by the IMU
// - The timestamp, 3D translation and rotation as quaternions processed by ARCore

public class csvWriter implements Runnable {
    private String path;
    private String data;
    private int type;

    csvWriter(String path, String data, int type) {
        this.path = path;
        this.data = data;
        this.type = type;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        writeCSV();
    }

    private void writeCSV() {
        File file = new File(path);
        BufferedWriter writer;
        FileWriter fileWriter;
        //File already exists
        if (file.exists() && !file.isDirectory()) {
            try {
                fileWriter = new FileWriter(path, true);
                writer = new BufferedWriter(fileWriter);
                writer.write(data);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Create a new file
        else {
            try {
                writer = new BufferedWriter(new FileWriter(path));
                switch (type) {
                    case 0:
                        String IMU_header = "Timestamp,Ax,Ay,Az,Gx,Gy,Gz";
                        writer.write(IMU_header);
                        writer.newLine();
                        writer.write(data);
                        writer.newLine();
                        writer.flush();
                        writer.close();

                    case 1:
                        String ARCore_header = "Timestamp,Tx,Ty,Tz,Qx,Qy,Qz,Qw";
                        writer.write(ARCore_header);
                        writer.newLine();
                        writer.write(data);
                        writer.newLine();
                        writer.flush();
                        writer.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
