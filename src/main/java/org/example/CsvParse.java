package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class CsvParse {
    public static void main(String[] args) throws IOException, CsvException {
        String filePath = "C:\\Users\\slend\\IdeaProjects\\mega-balls-29\\CSV\\Untitled spreadsheet - Sheet1.csv";
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = "";
        // Xerneas,Latios,Alazkazam,Amoongus,Bewear
        int commas = arrSize(filePath)[0];
        int lines = arrSize(filePath)[1];
        String[][] data = new String[lines][commas+1];
        System.out.println(data.length);
        System.out.println(data[0].length);

        for(int i = 0; i<lines; i++){
            line = br.readLine();

            String[] temp = line.split(",\\s*");

            System.out.println(Arrays.toString(temp));
            for (int j = 0; j < temp.length; j++) {
                data[i][j] = temp[j];
            }
        }
        System.out.println(Arrays.deepToString(data));
    }
    public static int[] arrSize(String filePath) throws IOException, CsvException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        int cc = 0;
        int lines = 0;
        int charAt = 0;
        while(charAt < line.length()){
            if(line.charAt(charAt)==',')
                cc++;
            charAt++;
        }
        while((line = br.readLine()) != null){
            lines++;
        }
        return new int[]{cc, lines};
    }
}