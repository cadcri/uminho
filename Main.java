import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    // arg 1 = annotate file and arg 2 = binary file if C
    public static void main(String[] args){

        List<LineOfCode> linesFileOne = getLinesFromAnnotateFile(args[1]);
        List<LineOfCode> linesFileTwo = null;

        if (args.length > 2)
            linesFileTwo = getLinesFromAnnotateFile(args[2]);

        prettyPrint(linesFileOne, linesFileTwo);
    }

    static List<LineOfCode> getLinesFromAnnotateFile(String path){
        String filename = path.substring(path.lastIndexOf('/') + 1);

        // remove prefix (c_ or java_) and suffix (_annotate.txt)
        filename = filename.replaceFirst("^(?:c_|java_)", "");
        filename = filename.replaceFirst("_annotate\\.txt$", "");

        // read file
        String content ="";
        try {

            content =  new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }


        // clean file
        StringBuilder cleaned = new StringBuilder();
        for (String line : content.split("\\R")) { // \\R = any line break
            // trim leading/trailing spaces
            line = line.trim();
            // replace multiple spaces/tabs with a single space
            line = line.replaceAll("\\s+", " ");
            // remove empty lines and lines with just the : character
            if (line.isEmpty() || line.equals(":")) {
                continue;
            }
            // remove : at start of the line and trim it
            if (line.charAt(0)==':') {
                line = line.substring(1);
                line = line.trim();
            }
            // add the line
            cleaned.append(line).append("\n");
        }
        String normalizedContent = cleaned.toString();


        // normalize first word if its a number
        Pattern firstNumber = Pattern.compile("^(\\d+)\\b");
        StringBuilder out = new StringBuilder();
        for (String line : normalizedContent.split("\\R")) {
            Matcher m = firstNumber.matcher(line);
            if (m.find()) {
                String num = m.group(1);
                String padded = String.format("%05d", Integer.parseInt(num));
                line = padded + line.substring(m.end());
            }
            out.append(line).append("\n");
        }
        String result = out.toString();


        // read the different sections
        Map<String, String> sections = new HashMap<>();
        // pattern to match the header line
        Pattern headerPattern = Pattern.compile(
                "Samples \\| Source code & Disassembly of (.+?) for cycles.*?\\n-+\\n",
                Pattern.DOTALL
        );
        // pattern to match the content section (from address line onwards)
        Pattern contentPattern = Pattern.compile(
                "Disassembly of section \\.text:\\n([0-9a-f]+ <.+?>:.*?)(?=Samples \\||$)",
                Pattern.DOTALL
        );
        Matcher headerMatcher = headerPattern.matcher(result);
        Matcher contentMatcher = contentPattern.matcher(result);
        // find all headers and their corresponding content
        while (headerMatcher.find() && contentMatcher.find()) {
            String key = headerMatcher.group(1).trim();
            String value = contentMatcher.group(1).trim();
            if(!sections.containsKey(key))
                sections.put(key, value);
        }

        String text = sections.get(filename);

        // Remove the first lines of context
        String[] lines = text.split("\n");
        StringBuilder res = new StringBuilder();
        boolean foundFirstNumberLine = false;
        String previousLine = null;
        for (String line : lines) {
            // check if line starts with a number followed by colon ("00003 :")
            if (!foundFirstNumberLine && line.trim().matches("^\\d+\\s*:.*")) {
                foundFirstNumberLine = true;
                // add the previous line first (the context line before the assembly)
                if (previousLine != null) {
                    res.append(previousLine).append("\n");
                }
            }
            if (foundFirstNumberLine) {
                res.append(line).append("\n");
            }
            previousLine = line;
        }
        text = res.toString().trim();


        // put it in a LineOfCode array
        List<LineOfCode> linesOfCode = new ArrayList<>();
        Pattern assemblyPattern = Pattern.compile("^(\\d+)\\s*:\\s*([0-9a-f]+):\\s*(.+)$");
        LineOfCode currentLineOfCode = null;
        int indent = 0;
        for (String line : text.split("\n")) {
            Matcher matcher = assemblyPattern.matcher(line.trim());
            if (matcher.matches()) {
                LineOfAssembly assembly = new LineOfAssembly();
                assembly.samples = Integer.parseInt(matcher.group(1));
                assembly.address = matcher.group(2);
                assembly.assembly = matcher.group(3).trim();
                currentLineOfCode.assemblies.add(assembly);

                // if c find line of current line TODO remove if java
                currentLineOfCode.line = runAddr2Line(path, assembly.address);
            } else {
                // test if
                currentLineOfCode = new LineOfCode();
                currentLineOfCode.code = line.trim();
                if(currentLineOfCode.code.endsWith("}")) {
                    indent--;
                }
                currentLineOfCode.indent = indent;
                if (currentLineOfCode.code.endsWith("{")) {
                    indent++;
                }
                linesOfCode.add(currentLineOfCode);
            }
        }

        // merge lines if they have the same line
        Map<Integer, LineOfCode> lineMap = new HashMap<>();
        List<LineOfCode> lineOfCodeMerged = new ArrayList<>();
        for (LineOfCode loc : linesOfCode) {
            if (loc.line != -1 && lineMap.containsKey(loc.line)) {
                LineOfCode existing = lineMap.get(loc.line);
                existing.assemblies.addAll(loc.assemblies);
            } else {
                lineMap.put(loc.line, loc);
                lineOfCodeMerged.add(loc);
            }
        }

        return lineOfCodeMerged;
    }

    static int runAddr2Line(String binaryPath, String address) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "/usr/bin/addr2line", "-e", binaryPath, address
            );
            pb.redirectErrorStream(true); // merge stderr with stdout

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("addr2line exited with code " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    static void prettyPrint(List<LineOfCode> linesOfCode, List<LineOfCode> linesOfCodeTwo) {

        int line_char_size_max = 0;

        for (LineOfCode lineOfCode : linesOfCode) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%06d", lineOfCode.samples())).append("  ");
            for (int i = 0; i < lineOfCode.indent; i++) {
                sb.append("  ");
            }
            sb.append(lineOfCode.code);
            line_char_size_max = Math.max(line_char_size_max, sb.length());
        }

        int maxSize = linesOfCodeTwo != null ? Math.max(linesOfCode.size(), linesOfCodeTwo.size()) : linesOfCode.size();

        for (int i = 0; i < maxSize; i++) {
            StringBuilder sb = new StringBuilder();

            if (i < linesOfCode.size()) {
                LineOfCode lineOfCode = linesOfCode.get(i);
                sb.append(String.format("%06d", lineOfCode.samples())).append("  ");
                for (int j = 0; j < lineOfCode.indent; j++) {
                    sb.append("  ");
                }
                sb.append(lineOfCode.code);
            }

            if (linesOfCodeTwo != null) {
                while (sb.length() < line_char_size_max) {
                    sb.append(" ");
                }

                sb.append(" | ");

                if (i < linesOfCodeTwo.size()) {
                    LineOfCode lineOfCodeTwo = linesOfCodeTwo.get(i);
                    sb.append(String.format("%06d", lineOfCodeTwo.samples())).append("  ");
                    for (int j = 0; j < lineOfCodeTwo.indent; j++) {
                        sb.append("  ");
                    }
                    sb.append(lineOfCodeTwo.code);
                }
            }

            System.out.println(sb.toString());
        }
    }


    static class LineOfCode {
        String code;
        int line =-1;
        int indent = 0;
        List<LineOfAssembly> assemblies = new ArrayList<>();

        int samples(){
            int samples = 0;
            for (LineOfAssembly assembly : assemblies) {
                samples += assembly.samples;
            }
            return samples;
        }
    }

    static class LineOfAssembly {
        String assembly;
        int samples;
        String address;
    }
}
