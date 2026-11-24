import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileListMaker {

    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<>();
        boolean needsToBeSaved = false;
        String currentFileName = null;
        boolean done = false;

        do {
            displayMenu(currentFileName, needsToBeSaved);
            String choice = getMenuChoice(in);

            try {
                switch (choice) {
                    case "A":
                        addItem(list);
                        needsToBeSaved = true;
                        break;

                    case "D":
                        if (list.isEmpty()) {
                            System.out.println("The list is empty. Nothing to delete.");
                        } else {
                            displayNumberedList(list);
                            int delIndex = SafeInput.getRangedInt(in, "Enter item number to delete", 1, list.size()) - 1;
                            String removed = list.remove(delIndex);
                            System.out.println("Removed: \"" + removed + "\"");
                            needsToBeSaved = true;
                        }
                        break;

                    case "I":
                        int insIndex = SafeInput.getRangedInt(
                                in,
                                "Enter position to insert at",
                                1,
                                list.size() + 1
                        ) - 1;
                        String newItem = SafeInput.getNonZeroLenString(in, "Enter item to insert");
                        list.add(insIndex, newItem);
                        System.out.println("Inserted \"" + newItem + "\" at position " + (insIndex + 1) + ".");
                        needsToBeSaved = true;
                        break;

                    case "M":
                        if (list.size() < 2) {
                            System.out.println("Need at least 2 items in the list to move an item.");
                        } else {
                            displayNumberedList(list);
                            int fromIndex = SafeInput.getRangedInt(
                                    in,
                                    "Enter item number to move",
                                    1,
                                    list.size()
                            ) - 1;
                            int toIndex = SafeInput.getRangedInt(
                                    in,
                                    "Enter new position",
                                    1,
                                    list.size()
                            ) - 1;

                            if (fromIndex != toIndex) {
                                String itemToMove = list.remove(fromIndex);
                                list.add(toIndex, itemToMove);
                                System.out.println("Moved \"" + itemToMove + "\" to position " + (toIndex + 1) + ".");
                                needsToBeSaved = true;
                            } else {
                                System.out.println("Item is already at that position.");
                            }
                        }
                        break;

                    case "C":
                        if (list.isEmpty()) {
                            System.out.println("List is already empty.");
                        } else {
                            boolean confirmClear = SafeInput.getYNConfirm(in, "Clear all items from the current list?");
                            if (confirmClear) {
                                list.clear();
                                System.out.println("List cleared.");
                                needsToBeSaved = true;
                            }
                        }
                        break;

                    case "V":
                        viewList(list, currentFileName, needsToBeSaved);
                        break;

                    case "O":
                        if (needsToBeSaved) {
                            boolean saveFirst = SafeInput.getYNConfirm(
                                    in,
                                    "You have unsaved changes. Save current list before opening another?"
                            );
                            if (saveFirst) {
                                currentFileName = saveCurrentList(list, currentFileName);
                                needsToBeSaved = false;
                            }
                        }

                        Path selectedPath = chooseListFile();
                        if (selectedPath != null) {
                            list = loadListFromFile(selectedPath);
                            currentFileName = selectedPath.getFileName().toString();
                            needsToBeSaved = false;
                            System.out.println("Loaded \"" + currentFileName + "\" (" + list.size() + " items).");
                        } else {
                            System.out.println("Open cancelled.");
                        }
                        break;

                    case "S":
                        currentFileName = saveCurrentList(list, currentFileName);
                        needsToBeSaved = false;
                        break;

                    case "Q":
                        if (needsToBeSaved) {
                            boolean saveBeforeQuit = SafeInput.getYNConfirm(
                                    in,
                                    "You have unsaved changes. Save before quitting?"
                            );
                            if (saveBeforeQuit) {
                                currentFileName = saveCurrentList(list, currentFileName);
                                needsToBeSaved = false;
                            }
                        }
                        done = SafeInput.getYNConfirm(in, "Are you sure you want to quit?");
                        break;
                }
            } catch (IOException ex) {
                System.out.println("File error: " + ex.getMessage());
            }

        } while (!done);

        System.out.println("Program exited. Goodbye!");
    }

    // --- Menu & Display Helpers ---

    private static String getMenuChoice(Scanner pipe) {
        while (true) {
            System.out.print("Enter choice [A, D, I, M, C, V, O, S, Q]: ");
            String input = pipe.nextLine().trim().toUpperCase();
            if (input.matches("[ADIMCVOSQ]")) {
                return input;
            }
            System.out.println("Invalid choice. Please enter one of: A, D, I, M, C, V, O, S, Q.");
        }
    }

    private static void displayMenu(String currentFileName, boolean needsToBeSaved) {
        String fileLabel = (currentFileName == null) ? "[New List]" : currentFileName;
        String dirtyMarker = needsToBeSaved ? " * (Unsaved)" : " (Saved)";
        System.out.println("\n==========================================");
        System.out.println("File: " + fileLabel + dirtyMarker);
        System.out.println("==========================================");
        System.out.println("A - Add item      | I - Insert item");
        System.out.println("D - Delete item     | M - Move item");
        System.out.println("C - Clear list      | V - View list");
        System.out.println("O - Open file       | S - Save file");
        System.out.println("Q - Quit");
        System.out.println("------------------------------------------");
    }

    private static void viewList(ArrayList<String> list, String currentFileName, boolean needsToBeSaved) {
        System.out.println("\n--- Current List (" + (currentFileName == null ? "New List" : currentFileName) + ") ---");
        if (list.isEmpty()) {
            System.out.println("[List is currently empty]");
        } else {
            displayNumberedList(list);
        }
        System.out.println("Needs to be saved: " + needsToBeSaved);
        System.out.println("------------------------------------------");
    }

    private static void displayNumberedList(ArrayList<String> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, list.get(i));
        }
    }

    private static void addItem(ArrayList<String> list) {
        String item = SafeInput.getNonZeroLenString(in, "Enter item to add");
        list.add(item);
        System.out.println("Added \"" + item + "\".");
    }

    // --- File Operation Orchestration & Static NIO Support Methods ---

    private static String saveCurrentList(ArrayList<String> list, String currentFileName) throws IOException {
        String targetFileName = currentFileName;
        if (targetFileName == null) {
            String baseName = SafeInput.getNonZeroLenString(in, "Enter base filename (without .txt)").trim();
            if (baseName.toLowerCase().endsWith(".txt")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            if (baseName.isEmpty()) {
                throw new IOException("Filename cannot be blank.");
            }
            targetFileName = baseName + ".txt";
        }

        Path targetPath = Paths.get(targetFileName);
        saveListToFile(targetPath, list);
        System.out.println("Saved list to \"" + targetPath.getFileName() + "\".");
        return targetPath.getFileName().toString();
    }

    private static Path chooseListFile() {
        JFrame owner = new JFrame();
        owner.setAlwaysOnTop(true);
        owner.setLocationRelativeTo(null);

        try {
            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setDialogTitle("Select a .txt List File");
            chooser.setFileFilter(new FileNameExtensionFilter("Text List Files (*.txt)", "txt"));

            int result = chooser.showOpenDialog(owner);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                return chooser.getSelectedFile().toPath();
            }
            return null;
        } finally {
            owner.dispose();
        }
    }

    /**
     * Loads lines from a disk file using Java NIO.
     * Throws IOException to be handled centrally in main.
     */
    public static ArrayList<String> loadListFromFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath.toAbsolutePath());
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return new ArrayList<>(lines);
    }

    /**
     * Writes list elements to disk using Java NIO, overwriting existing contents.
     * Throws IOException to be handled centrally in main.
     */
    public static void saveListToFile(Path filePath, List<String> items) throws IOException {
        Files.write(
                filePath,
                items,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }
}