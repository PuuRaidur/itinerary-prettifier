import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Prettifier {
    static String GREEN = "\033[0;32m";
    static String RED = "\033[0;31m";
    static String YELLOW = "\033[0;33m";
    static String UNDERLINE = "\u001B[4m";
    static String BOLD = "\u001B[1m";
    static String CLOCK_EMOJI = "\uD83D\uDD50";
    static String RED_CIRCLE = "\uD83D\uDD34";
    static String CHECK_MARK = "✅";
    static String INFO_MARK = "ℹ\uFE0F";
    static String RESET = "\033[0m";
    static String inputFile;
    static String outputFile;
    static String airportLookupCSV;
    static String text;
    static String[] CSVFirstColumn = {"name","iso_country","municipality","icao_code","iata_code","coordinates"};
    static ArrayList<String> IATACodes = new ArrayList<>();
    static ArrayList<String> ICAOCodes = new ArrayList<>();
    static String[] airportData = new String[6];

    static void getInputFileTextAndCheckIfInputFileMalformed() {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            StringBuilder sb = new StringBuilder();
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            text = sb.toString();
        } catch (IOException e) {
            System.out.println(RED_CIRCLE + RED + BOLD + " Input not found" + RESET);
            System.exit(0);
        }
        if(text.isEmpty()) {
            System.out.println(RED_CIRCLE + RED + BOLD + " Input is empty" + RESET);
            System.exit(0);
        }
        Pattern iataPattern = Pattern.compile("(?<!#)#(?!#)([A-Z]+)");
        Matcher iataMatcher = iataPattern.matcher(text);
        while(iataMatcher.find()) {
            String code = iataMatcher.group(1);
            if(code.length() != 3) {
                System.out.println(RED_CIRCLE + RED + BOLD + " Input is malformed" + RESET);
                System.exit(0);
            }
        }
        Pattern icaoPattern = Pattern.compile("##([A-Z]+)");
        Matcher icaoMatcher = icaoPattern.matcher(text);
        while(icaoMatcher.find()) {
            String code = icaoMatcher.group(1);
            if(code.length() != 4) {
                System.out.println(RED_CIRCLE + RED + BOLD + " Input is malformed" + RESET);
                System.exit(0);
            }
        }
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

    static void isFirstColumnMissing(String line) {
        String[] firstColumn = line.split(",");
        for(int i = 0; i < firstColumn.length; i++) {
            firstColumn[i] = firstColumn[i].trim();
        }
        if(!Arrays.equals(firstColumn, CSVFirstColumn)) {
            System.out.println(RED_CIRCLE + RED + BOLD + " Airport lookup is malformed" + RESET);
            System.exit(0);
        }
    }

    static void isLookupMalformed(String[] airportData) {
        if(airportData.length != 6) {
            System.out.println(RED_CIRCLE + RED + BOLD + " Airport lookup is malformed" + RESET);
            System.exit(0);
        }
        for(int i = 0; i < airportData.length; i++) {
            if(airportData[i].isBlank()) {
                System.out.println(RED_CIRCLE + RED + BOLD + " Airport lookup is malformed" + RESET);
                System.exit(0);
            }
        }
    }

    static void replaceCodes() {
        try {
            Scanner CSVReader = new Scanner(new File(airportLookupCSV));
            String firstColumn = CSVReader.nextLine();
            isFirstColumnMissing(firstColumn);
            while (CSVReader.hasNextLine()) {
                String line = CSVReader.nextLine();
                airportData = parseCSVLine(line);
                isLookupMalformed(airportData);
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
            System.out.println(RED_CIRCLE + RED + BOLD + " Airport lookup not found" + RESET);
        }
    }

    // parse an ISO offset datetime string, return null if parse fails
    static OffsetDateTime tryParseOffsetDateTime(String s) {
        if (s == null) return null;
        // normalize unicode minus (U+2212) to ASCII hyphen-minus
        String normalized = s.replace('\u2212', '-');
        try {
            return OffsetDateTime.parse(normalized); // accepts Z or ±hh:mm offsets
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String formatDate(OffsetDateTime odt) {
        // Example: "05 Apr 2007"
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        return odt.format(fmt);
    }

    static String formatT12(OffsetDateTime odt) {
        // Example: "12:30PM (-02:00)"  — note: DateTimeFormatter produces AM/PM without space
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);
        String timePart = odt.format(timeFmt);
        String offset = odt.getOffset().getId(); // "+02:00" or "Z"
        if ("Z".equals(offset)) offset = "+00:00";
        return timePart + " (" + offset + ")";
    }

    static String formatT24(OffsetDateTime odt) {
        // Example: "12:30 (-02:00)"
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
        String timePart = odt.format(timeFmt);
        String offset = odt.getOffset().getId();
        if ("Z".equals(offset)) offset = "+00:00";
        return timePart + " (" + offset + ")";
    }

    static void prettifyDates() {
        if (text == null || text.isEmpty()) return;

        StringBuilder out = new StringBuilder();
        int pos = 0;
        final int len = text.length();

        while (pos < len) {
            // Handle D(
            if (text.startsWith("D(", pos)) {
                int close = text.indexOf(')', pos + 2);
                if (close == -1) { // no closing parenthesis -> leave rest unchanged
                    out.append(text.substring(pos));
                    break;
                }
                String content = text.substring(pos + 2, close); // inside D(...)
                OffsetDateTime odt = tryParseOffsetDateTime(content);
                if (odt != null) {
                    out.append(formatDate(odt));
                } else {
                    // leave token unchanged
                    out.append(text.substring(pos, close + 1));
                }
                pos = close + 1;
                continue;
            }

            // Handle T12(
            if (text.startsWith("T12(", pos)) {
                int close = text.indexOf(')', pos + 4);
                if (close == -1) {
                    out.append(text.substring(pos));
                    break;
                }
                String content = text.substring(pos + 4, close); // inside T12(...)
                OffsetDateTime odt = tryParseOffsetDateTime(content);
                if (odt != null) {
                    out.append(formatT12(odt));
                } else {
                    out.append(text.substring(pos, close + 1));
                }
                pos = close + 1;
                continue;
            }

            // Handle T24(
            if (text.startsWith("T24(", pos)) {
                int close = text.indexOf(')', pos + 4);
                if (close == -1) {
                    out.append(text.substring(pos));
                    break;
                }
                String content = text.substring(pos + 4, close); // inside T24(...)
                OffsetDateTime odt = tryParseOffsetDateTime(content);
                if (odt != null) {
                    out.append(formatT24(odt));
                } else {
                    out.append(text.substring(pos, close + 1));
                }
                pos = close + 1;
                continue;
            }

            // default: copy one char
            out.append(text.charAt(pos));
            pos++;
        }

        text = out.toString();
    }

    static void writeToOutputFile() {
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile))) {
            outputWriter.write(text);
            System.out.println(GREEN + BOLD + "Prettifying and writing text to output file successful" + CHECK_MARK + RESET);
        } catch (FileNotFoundException e) {
            System.out.println(RED_CIRCLE + RED + BOLD + " File not found" + RESET);
        } catch (IOException e) {
            System.out.println(RED_CIRCLE + RED + BOLD + " Error writing to output file" + RESET);
        }
    }

    static void prettifyText() {
        String code;
        getInputFileTextAndCheckIfInputFileMalformed();
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
        text = text.replace("\\r", "\n");
        text = text.replace("\\f", "\n");
        text = text.replace("\\v", "\n");
        text = text.trim();
        text = text.replaceAll("(?m)^[ \\t]+", "");
        text = text.replaceAll("(?m)[ \\t]+$", "");
        text = text.replaceAll(" +", " ");
        text = text.replaceAll("\n{3,}", "\n\n");
        System.out.println(INFO_MARK + YELLOW + UNDERLINE + " Prettified text:" + RESET);
        System.out.println(text);
        writeToOutputFile();
    }

    public static void main(String[] args) {
        if(args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(YELLOW + UNDERLINE + "Itinerary usage:" + RESET);
            System.out.println(GREEN + "$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
        }
        else if(args.length == 3 &&
                args[0].endsWith(".txt") &&
                args[1].endsWith(".txt") &&
                args[2].endsWith(".csv")) {

            inputFile = args[0];
            outputFile = args[1];
            airportLookupCSV = args[2];
            System.out.println(BOLD + "Process will be done shortly..." + CLOCK_EMOJI + RESET);
            prettifyText();
        }
        else {
            System.out.println(RED_CIRCLE + RED + BOLD + " False usage!" + RESET);
            System.out.println(YELLOW + UNDERLINE + "Itinerary usage:" + RESET);
            System.out.println(GREEN + "$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
        }
    }
}