import java.io.*;
import java.util.*;
import java.util.logging.*;

public class MarksImporter {

    private static final Logger LOGGER = Logger.getLogger(MarksImporter.class.getName());

    // Technical details go to import.log (not the console), so users never see raw stack traces.
    static {
        try {
            LOGGER.setUseParentHandlers(false);   // turn off default console output
            FileHandler fh = new FileHandler("import.log", true);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
        } catch (IOException e) {
            System.out.println("Warning: log file could not be created.");
        }
    }

    // DECISION: a bad record is SKIPPED (reported and logged), and the import continues.
    // One faulty line should not stop the other valid records from being imported.
    // A missing/unreadable file is different: there is nothing to import, so we stop.

    // 'throws' declares that this method may pass a CHECKED IOException up to its caller.
    static void importMarks(String filename) throws IOException {
        Set<String> seen = new HashSet<>();
        int accepted = 0, skipped = 0, lineNo = 0;

        // try-with-resources: the BufferedReader implements AutoCloseable, so Java calls
        // br.close() automatically when the block ends - whether it finishes normally
        // or an exception is thrown. No finally/close() needed.
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;   // ignore blank lines
                try {
                    String[] parts = line.split(",");

                    // TODO 1: exactly 2 fields
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Expected 2 fields but found " + parts.length);
                    }
                    String id = parts[0].trim();

                    // TODO 2: parse mark and validate range 0..100
                    int mark;
                    try {
                        mark = Integer.parseInt(parts[1].trim());   // may throw NumberFormatException
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Mark '" + parts[1].trim() + "' is not a number", e);
                    }
                    if (mark < 0 || mark > 100) {
                        // 'throw' actually raises an exception object here
                        throw new IllegalArgumentException("Mark " + mark + " is outside 0..100");
                    }

                    // TODO 3: duplicate detection (add() returns false if already present)
                    if (!seen.add(id)) {
                        throw new DuplicateStudentException(id);
                    }

                    // TODO 4: display accepted record
                    System.out.println("Accepted: " + id + " -> " + mark);
                    accepted++;

                } catch (DuplicateStudentException e) {
                    System.out.println("Line " + lineNo + " skipped: " + e.getMessage());
                    LOGGER.log(Level.WARNING, "Line " + lineNo + ": " + e.getClass().getName() + " - " + e.getMessage());
                    skipped++;
                } catch (IllegalArgumentException e) {
                    System.out.println("Line " + lineNo + " skipped: invalid data (" + e.getMessage() + ")");
                    LOGGER.log(Level.WARNING, "Line " + lineNo + ": " + e.getClass().getName()
                            + " - " + e.getMessage()
                            + (e.getCause() != null ? " | cause: " + e.getCause() : ""));
                    skipped++;
                }
            }
        }
        System.out.println("Summary: " + accepted + " accepted, " + skipped + " skipped.");
    }

    public static void main(String[] args) {
        String file = args.length > 0 ? args[0] : "marks.txt";
        try {
            importMarks(file);
        } catch (FileNotFoundException e) {            // more specific: must come BEFORE IOException
            System.out.println("Marks file not found; check the file name.");
            LOGGER.log(Level.SEVERE, "FileNotFoundException: " + e.getMessage(), e);
        } catch (IOException e) {                       // other I/O problems (e.g. read error)
            System.out.println("Could not read the marks file. Please try again later.");
            LOGGER.log(Level.SEVERE, "IOException: " + e.getMessage(), e);
        } catch (RuntimeException e) {                  // safety net for anything unexpected
            System.out.println("Unexpected problem during import. Contact support.");
            LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
        } finally {
            System.out.println("Import attempt finished");
        }
    }
}

// Custom UNCHECKED exception: extends RuntimeException, so callers are not forced to catch it.
class DuplicateStudentException extends RuntimeException {
    public DuplicateStudentException(String studentId) {
        super("Duplicate student number: " + studentId);
    }
}