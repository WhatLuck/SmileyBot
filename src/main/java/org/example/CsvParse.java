package org.example;

import com.opencsv.exceptions.CsvException;;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class CsvParse {
    public static String[] main(String[] args) throws IOException, CsvException {
        String filePath = "C:\\Users\\slend\\Documents\\GitHub\\mega-balls-29\\CSV\\Draft League  - Sheet1 (1).csv";
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = "";
        // Xerneas,Latios,Alazkazam,Amoongus,Bewear
        int commas = arrSize(filePath)[0];
        int lines = arrSize(filePath)[1];
        String[][] data = new String[lines][commas+1];
        for(int i = 0; i<lines; i++){
            line = br.readLine();
            String[] temp = line.split(",\\s*");
            for (int j = 0; j < temp.length; j++) {
                data[i][j] = temp[j];
                if(data[i][j] == "")
                    data[i][j] = null;
            }
        }
        return randomVar(data,5, filePath);
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

    public static String[] randomVar(String[][] arr, int n, String filePath) throws IOException, CsvException {
        int commas = arrSize(filePath)[0];
        int lines = arrSize(filePath)[1];
        Random random = new Random();
        String[] randoms = new String[n];
        for(int i = 0; i<n; i++){
            int[] rNumbs;
            do {
                rNumbs = new int[]{random.nextInt(commas - 1) + 1, random.nextInt(lines - 1) + 1};
            } while(arr[rNumbs[0]][rNumbs[1]] == null || Arrays.asList(randoms).contains(arr[rNumbs[0]][rNumbs[1]]));

            randoms[i] = arr[rNumbs[0]][rNumbs[1]];
            System.out.println(Arrays.toString(rNumbs));
        }

        return randoms;
    }
    //
}