import java.io.*;
import java.util.*;
public class LibraryManagementSystem {

    // ----- Book class -----
    public static class Book implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        private String title;
        private String author;
        private boolean issued;
        private String issuedTo; // optional

        public Book(int id, String title, String author) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.issued = false;
            this.issuedTo = null;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public boolean isIssued() { return issued; }
        public String getIssuedTo() { return issuedTo; }

        public void issueTo(String person) {
            this.issued = true;
            this.issuedTo = person;
        }

        public void returned() {
            this.issued = false;
            this.issuedTo = null;
        }

        @Override
        public String toString() {
            return String.format("[%d] %s by %s %s", id, title, author,
                    (issued ? "(Issued to: " + issuedTo + ")" : "(Available)"));
        }
    }

    // ----- Library class -----
    public static class Library implements Serializable {
        private static final long serialVersionUID = 1L;
        private Map<Integer, Book> books = new LinkedHashMap<>();
        private int nextId = 1;

        public synchronized Book addBook(String title, String author) {
            Book b = new Book(nextId++, title.trim(), author.trim());
            books.put(b.getId(), b);
            return b;
        }

        public synchronized List<Book> listBooks() {
            return new ArrayList<>(books.values());
        }

        public synchronized Book findBook(int id) {
            return books.get(id);
        }

        public synchronized boolean issueBook(int id, String person) {
            Book b = books.get(id);
            if (b == null || b.isIssued()) return false;
            b.issueTo(person.trim());
            return true;
        }

        public synchronized boolean returnBook(int id) {
            Book b = books.get(id);
            if (b == null || !b.isIssued()) return false;
            b.returned();
            return true;
        }

        // persistence helpers
        public static void saveToFile(Library lib, String filename) throws IOException {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
                out.writeObject(lib);
            }
        }

        public static Library loadFromFile(String filename) throws IOException, ClassNotFoundException {
            File f = new File(filename);
            if (!f.exists()) return new Library();
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                Object o = in.readObject();
                if (o instanceof Library) return (Library) o;
                else return new Library();
            }
        }
    }

    // ----- Main app -----
    private static final String DATA_FILE = "library.dat";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Library lib;
        try {
            lib = Library.loadFromFile(DATA_FILE);
            System.out.println("Loaded library from " + DATA_FILE + ".");
        } catch (Exception e) {
            System.out.println("Could not load saved data. Starting with an empty library.");
            lib = new Library();
        }

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": // add
                    System.out.print("Title: ");
                    String title = sc.nextLine();
                    System.out.print("Author: ");
                    String author = sc.nextLine();
                    if (title.isBlank() || author.isBlank()) {
                        System.out.println("Title and author cannot be empty.");
                        break;
                    }
                    Book added = lib.addBook(title, author);
                    System.out.println("Added: " + added);
                    save(lib);
                    break;
                case "2": // list
                    List<Book> all = lib.listBooks();
                    if (all.isEmpty()) {
                        System.out.println("No books in library.");
                    } else {
                        System.out.println("Books in library:");
                        for (Book b : all) System.out.println("  " + b);
                    }
                    break;
                case "3": // issue
                    System.out.print("Enter book id to issue: ");
                    int idToIssue = readInt(sc);
                    if (idToIssue == -1) break;
                    Book bi = lib.findBook(idToIssue);
                    if (bi == null) {
                        System.out.println("No book with id " + idToIssue);
                        break;
                    }
                    if (bi.isIssued()) {
                        System.out.println("Book already issued to " + bi.getIssuedTo());
                        break;
                    }
                    System.out.print("Issue to (person name): ");
                    String name = sc.nextLine();
                    if (name.isBlank()) { System.out.println("Name cannot be empty."); break; }
                    boolean ok = lib.issueBook(idToIssue, name);
                    System.out.println(ok ? "Book issued." : "Could not issue book.");
                    save(lib);
                    break;
                case "4": // return
                    System.out.print("Enter book id to return: ");
                    int idToReturn = readInt(sc);
                    if (idToReturn == -1) break;
                    boolean ret = lib.returnBook(idToReturn);
                    System.out.println(ret ? "Book returned." : "Cannot return (invalid id or not issued).");
                    save(lib);
                    break;
                case "5": // search
                    System.out.print("Search by title or author (substring): ");
                    String q = sc.nextLine().toLowerCase().trim();
                    if (q.isBlank()) { System.out.println("Empty query."); break; }
                    List<Book> found = new ArrayList<>();
                    for (Book b : lib.listBooks()) {
                        if (b.getTitle().toLowerCase().contains(q) || b.getAuthor().toLowerCase().contains(q)) found.add(b);
                    }
                    if (found.isEmpty()) System.out.println("No matches.");
                    else for (Book b : found) System.out.println("  " + b);
                    break;
                case "0":
                    System.out.println("Exiting. Saving data...");
                    save(lib);
                    running = false;
                    break;
                default:
                    System.out.println("Unknown option.");
            }
            System.out.println();
        }

        sc.close();
    }

    private static void printMenu() {
        System.out.println("=== Library Management ===");
        System.out.println("1. Add book");
        System.out.println("2. List books");
        System.out.println("3. Issue book");
        System.out.println("4. Return book");
        System.out.println("5. Search books");
        System.out.println("0. Exit");
    }

    private static int readInt(Scanner sc) {
        String s = sc.nextLine().trim();
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { System.out.println("Invalid number."); return -1; }
    }

    private static void save(Library lib) {
        try {
            Library.saveToFile(lib, DATA_FILE);
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }
}
