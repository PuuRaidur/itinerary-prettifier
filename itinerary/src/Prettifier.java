import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Prettifier {
    static String GREEN = "\033[0;32m";
    static String RED = "\033[0;31m";
    static String RESET = "\033[0m";
    static String inputFile;
    static String outputFile;
    static String airportLookupCSV;
    static String text;
    static ArrayList<String> IATACodes = new ArrayList<>();
    static ArrayList<String> ICAOCodes = new ArrayList<>();
    static String[] airportData = new String[6];

    static boolean getInputFileTextAndCheckIfInputFileMalformed() {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            StringBuilder sb = new StringBuilder();
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            text = sb.toString();
        } catch (IOException e) {
            System.out.println(RED + "Input not found" + RESET);
            return true;
        }
        if(text.isEmpty()) {
            System.out.println(RED + "Input is empty" + RESET);
            return true;
        }
        Pattern iataPattern = Pattern.compile("(?<!#)#(?!#)([A-Z]+)");
        Matcher iataMatcher = iataPattern.matcher(text);
        while(iataMatcher.find()) {
            String code = iataMatcher.group(1);
            if(code.length() != 3) {
                System.out.println(RED + "Input is malformed" + RESET);
                return true;
            }
        }
        Pattern icaoPattern = Pattern.compile("##([A-Z]+)");
        Matcher icaoMatcher = icaoPattern.matcher(text);
        while(icaoMatcher.find()) {
            String code = icaoMatcher.group(1);
            if(code.length() != 4) {
                System.out.println(RED + "Input is malformed" + RESET);
                return true;
            }
        }
        if (text.contains("D(")) {
            if (!text.matches("(?s).*D\\(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z\\).*") &&
                    !text.matches("(?s).*D\\(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}\\).*")) {
                System.out.println(RED + "Input is malformed" + RESET);
                return true;
            }
        }
        if (text.contains("T12(")) {
            if (!text.matches("(?s).*T12\\(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z\\).*") &&
                    !text.matches("(?s).*T12\\(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}\\).*")) {
                System.out.println(RED + "Input is malformed" + RESET);
                return true;
            }
        }
        if (text.contains("T24(")) {
            if (!text.matches("(?s).*T24\\(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z\\).*") &&
                    !text.matches("(?s).*T24\\(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}\\).*")) {
                System.out.println(RED + "Input is malformed" + RESET);
                return true;
            }
        }
        return false;
    }

    static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString().trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());

        return fields.toArray(new String[0]);
    }

    static boolean isLookupMalformed(String[] airportData) {
        if(airportData.length != 6) {
            return true;
        }
        for(int i = 0; i < airportData.length; i++) {
            if(airportData[i].isBlank()) {
                return true;
            }
        }
        return false;
    }

    static void replaceCodes() {
        try {
            Scanner CSVReader = new Scanner(new File(airportLookupCSV));
            while (CSVReader.hasNextLine()) {
                String line = CSVReader.nextLine();
                airportData = parseCSVLine(line);

                if(isLookupMalformed(airportData)){
                    System.out.println(RED + "Airport lookup malformed" + RESET);
                    return;
                }
                for(int i = 0; i < airportData.length; i++){
                    airportData[i] = airportData[i].trim();
                }
                for(int i = 0; i < IATACodes.size(); i++){
                    String IATACode = IATACodes.get(i);

                    if(IATACode.startsWith("*#")){
                        if(airportData[4].equals(IATACode.substring(2))){
                            text = text.replace(IATACode, airportData[2]);
                        }
                    }
                    if(IATACode.startsWith("#")) {
                        if(airportData[4].equals(IATACode.substring(1))){
                            text = text.replace(IATACode, airportData[0]);
                        }
                    }
                }
                for(int i = 0; i < ICAOCodes.size(); i++){
                    String ICAOCode = ICAOCodes.get(i);
                    if(ICAOCode.startsWith("*##")){
                        if(airportData[3].equals(ICAOCode.substring(3))){
                            text = text.replace(ICAOCode, airportData[2]);
                        }
                    }
                    if(ICAOCode.startsWith("##")){
                        if(airportData[3].equals(ICAOCode.substring(2))){
                            text = text.replace(ICAOCode, airportData[0]);
                        }
                    }
                }
            }
            CSVReader.close();
        } catch(IOException e) {
            System.out.println(RED + "Airport lookup not found" + RESET);
        }
    }

    static String getAddedTime(String timeAndZone){
        for(int i = 0; i < timeAndZone.length(); i++){
            char c = timeAndZone.charAt(i);
            if(c == 'Z'){
                return "(+00:00)";
            }
            if(c == '+' || c == '-'){
                return "(" + timeAndZone.substring(i) + ")";
            }
        }
        return "";
    }

    static String getRightOffset(String offset){
        int colonIndex = offset.indexOf(':');
        int hour = Integer.parseInt(offset.substring(0, colonIndex));
        String minutes = offset.substring(colonIndex + 1);
        String period;
        if (hour >= 12) {
            period = "PM";
        } else {
            period = "AM";
        }
        if (hour == 0) {
            hour = 12;
        } else if (hour > 12) {
            hour = hour - 12;
        }
        return hour + ":" + minutes + period;
    }

    static void prettifyDates() {
        for(int i = 0; i < text.length(); i++) {
            if (text.startsWith("D(", i)) {
                int closeParen = text.indexOf(')', i);
                String content = text.substring(i + 2, closeParen);
                int tIndex = content.indexOf('T');
                if (tIndex != -1) {
                    content = content.substring(0, tIndex);
                }

                String year = content.substring(0, 4);
                String month = content.substring(5, 7);
                String day = content.substring(8, 10);

                switch (month) {
                    case "01":
                        month = "January";
                        break;
                    case "02":
                        month = "February";
                        break;
                    case "03":
                        month = "March";
                        break;
                    case "04":
                        month = "April";
                        break;
                    case "05":
                        month = "May";
                        break;
                    case "06":
                        month = "June";
                        break;
                    case "07":
                        month = "July";
                        break;
                    case "08":
                        month = "August";
                        break;
                    case "09":
                        month = "September";
                        break;
                    case "10":
                        month = "October";
                        break;
                    case "11":
                        month = "November";
                        break;
                    case "12":
                        month = "December";
                        break;
                }
                String date = day + " " + month + " " + year;
                text = text.replace(text.substring(i, closeParen + 1), date);
            }

            if (text.startsWith("T12(", i)) {
                int closeParen = text.indexOf(')', i);
                String content = text.substring(i+4, closeParen);
                int tIndex = content.indexOf('T');
                String timeAndZone = content.substring(tIndex + 1);

                int zoneStart = -1;
                for(int j = 0; j < timeAndZone.length(); j++){
                    char c = timeAndZone.charAt(j);
                    if(c == 'Z' || c == '+' || c == '-'){
                        if(j > 2){
                            zoneStart = j;
                            break;
                        }
                    }
                }

                String timeStr = timeAndZone.substring(0, zoneStart);
                String offset = getRightOffset(timeStr);
                String addedTime = getAddedTime(timeAndZone.substring(zoneStart));
                String time = offset + " " + addedTime;
                text = text.replace(text.substring(i, closeParen + 1), time);
            }

            if (text.startsWith("T24(", i)) {
                int closeParen = text.indexOf(')', i);
                String content = text.substring(i+5, closeParen);
                int tIndex = content.indexOf('T');
                String timeAndZone = content.substring(tIndex + 1);

                int zoneStart = -1;
                for(int j = 0; j < timeAndZone.length(); j++){
                    char c = timeAndZone.charAt(j);
                    if(c == 'Z' || c == '+' || c == '-'){
                        if(j > 2){
                            zoneStart = j;
                            break;
                        }
                    }
                }

                String timeStr = timeAndZone.substring(0, zoneStart);
                String addedTime = getAddedTime(timeAndZone.substring(zoneStart));
                String time = timeStr + " " + addedTime;
                text = text.replace(text.substring(i, closeParen + 1), time);
            }
        }
    }

    static void writeToOutputFile() {
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile))){
            outputWriter.write(text);
        } catch(FileNotFoundException e) {
            System.out.println(RED + "File not found" + RESET);
        } catch(IOException e){
            System.out.println(RED + "Error writing to output file" + RESET);
        }
    }

    static void prettifyText() {
        String code;
        if(getInputFileTextAndCheckIfInputFileMalformed()) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            if (i + 4 < text.length() && text.startsWith("*#", i) && text.charAt(i+2) != '#') {
                code = text.substring(i, i + 5);
                IATACodes.add(code);
            }
            if (i + 6 < text.length() && text.charAt(i) == '*' && text.startsWith("##", i + 1)) {
                code = text.substring(i, i + 7);
                ICAOCodes.add(code);
            }
            if (i + 3 < text.length() && text.charAt(i) == '#' && text.charAt(i + 1) != '#' && text.charAt(i - 1) != '#' && text.charAt(i - 1) != '*') {
                code = text.substring(i, i + 4);
                IATACodes.add(code);
            }
            if (i + 5 < text.length() && text.startsWith("##", i) && text.charAt(i - 1) != '*') {
                code = text.substring(i, i + 6);
                ICAOCodes.add(code);
            }
        }
        replaceCodes();
        prettifyDates();
        text = text.trim();
        text = text.replaceAll(" +", " ");
        text = text.replaceAll("\n\n+", "\n\n");
        writeToOutputFile();
    }

    public static void main(String[] args) {
        if(args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(GREEN + "Itinerary usage:" + RESET);
            System.out.println(GREEN + "$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
        }
        else if(args.length == 3 &&
                args[0].equals("./input.txt") &&
                args[1].equals("./output.txt") &&
                args[2].equals("./airport-lookup.csv")) {

            inputFile = args[0];
            outputFile = args[1];
            airportLookupCSV = args[2];
            prettifyText();
        }
        else {
            System.out.println(RED + "False usage." + RESET);
            System.out.println(GREEN + "Itinerary usage:" + RESET);
            System.out.println(GREEN + "$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
        }
    }
}