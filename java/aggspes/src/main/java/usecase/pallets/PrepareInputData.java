package usecase.pallets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class PrepareInputData {

    public static void main(String[] args) {
        String baseFolder = args[0];
        System.out.println("Looking for files in base folder: "+baseFolder);
        File dir = new File(baseFolder);
        String extension = ".txt";

        ArrayList<ArrayList<Double>> rows = new ArrayList<>();

        findFiles(dir, extension, rows);

        System.out.println("total number of rows read: " + rows.size());
    }

    public static void findFiles(File dir, final String extension, ArrayList<ArrayList<Double>> rows) {
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(extension) || pathname.isDirectory();
            }
        });

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findFiles(file, extension, rows);
                } else {
                    readFile(file, rows);
                }
            }
        }
    }

    public static void readFile(File file, ArrayList<ArrayList<Double>> rows) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int oldIndex = -1;
            int newIndex = -1;
            double value = -1;
            ArrayList<Double> rowValues = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) {
                    newIndex = Integer.valueOf(line.split(" ")[1]);
                    value = Double.valueOf(line.split(" ")[2]);
                    if (newIndex < oldIndex) {
                        rows.add(rowValues);
                        rowValues = new ArrayList<>();
                    }
                    rowValues.add(value);
                    oldIndex = newIndex;
                }
            }
            if (rowValues.size() > 0) {
                rows.add(rowValues);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
