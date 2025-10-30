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


        // read file
        String content ="";
        try {
            String path = args[1];
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

        String text = sections.get("QuickSort");

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
        for (String line : text.split("\n")) {
            Matcher matcher = assemblyPattern.matcher(line.trim());
            if (matcher.matches()) {
                LineOfAssembly assembly = new LineOfAssembly();
                assembly.samples = Integer.parseInt(matcher.group(1));
                assembly.address = matcher.group(2);
                assembly.assembly = matcher.group(3).trim();
                currentLineOfCode.assemblies.add(assembly);

                // if c find line of current line TODO remove if java
                currentLineOfCode.line = runAddr2Line(args[1], assembly.address);
            } else {
                // test if
                currentLineOfCode = new LineOfCode();
                currentLineOfCode.code = line.trim();
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

        // pretty print
        prettyPrint(lineOfCodeMerged);
    }

    static int runAddr2Line(String binaryPath, String address) {
        try {
            // Build the command
            ProcessBuilder pb = new ProcessBuilder(
                    "/usr/bin/addr2line", "-e", binaryPath, address
            );
            pb.redirectErrorStream(true); // merge stderr with stdout

            // Start the process
            Process process = pb.start();

            // Read its output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Wait for the process to exit
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("addr2line exited with code " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    static void prettyPrint(List<LineOfCode> linesOfCode) {
        int indent = 0;
        for (LineOfCode lineOfCode : linesOfCode) {
            // print samples
            String samples = String.format("%05d", lineOfCode.samples());
            System.out.print(samples+"|  ");
            if(lineOfCode.code.endsWith("}")) {
                indent--;
            }
            for (int i = 0; i < indent; i++) {
                System.out.print("  ");
            }
            System.out.println(lineOfCode.code);
            if (lineOfCode.code.endsWith("{")) {
                indent++;
            }
        }
    }



    static class LineOfCode {
        String code;
        int line =-1;
        List<LineOfAssembly> assemblies = new ArrayList<>();

        int samples(){
            int samples = 0;
            for (LineOfAssembly assembly : assemblies) {
                samples += assembly.samples;
            }
            return samples;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LineOfCode{");
            sb.append("code='").append(code).append('\'');
            sb.append(", line=").append(line);
            sb.append(", assemblies=").append(assemblies);
            sb.append('}');
            return sb.toString();
        }
    }

    static class LineOfAssembly {
        String assembly;
        int samples;
        String address;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LineOfAssembly{");
            sb.append("assembly='").append(assembly).append('\'');
            sb.append(", samples=").append(samples);
            sb.append(", address='").append(address).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
