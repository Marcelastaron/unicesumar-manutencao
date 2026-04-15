import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyDatabase {

    public static Map<Integer, Map<String, Object>> books = new HashMap<Integer, Map<String, Object>>();
    public static Map<Integer, Map<String, Object>> users = new HashMap<Integer, Map<String, Object>>();
    public static List<Map<String, Object>> loans = new ArrayList<Map<String, Object>>();
    public static List<String> logs = new ArrayList<String>();

    public static int BOOK_SEQ = 1;
    public static int USER_SEQ = 1;
    public static int LOAN_SEQ = 1;

    public static String SYSTEM_MODE = "LEGACY";
    public static int GLOBAL_FINE_PER_DAY = 2;
    public static int GLOBAL_MAX_LOAN_DAYS = 14;
    public static boolean WORKAROUND_FLAG = true;

    // REFACTORING (R4): extracted magic numbers into named constants.
    // Before: if (logs.size() > 500) and for (int i = 400; ...)
    // Now: named constants make the log rotation policy explicit and easy to change.
    private static final int LOG_MAX_SIZE = 500;
    private static final int LOG_TRIM_OFFSET = 400;

    public static int addBookData(String title, String author, int year, String category, int totalCopies, int availableCopies,
            String shelfCode, String isbn) {
        Map<String, Object> data = new HashMap<String, Object>();
        int id = BOOK_SEQ;
        BOOK_SEQ = BOOK_SEQ + 1;
        data.put("id", id);
        data.put("title", title);
        data.put("author", author);
        data.put("year", year);
        data.put("category", category);
        data.put("totalCopies", totalCopies);
        data.put("availableCopies", availableCopies);
        data.put("shelfCode", shelfCode);
        data.put("isbn", isbn);
        data.put("active", true);
        data.put("extra", "");
        books.put(id, data);
        logs.add("book-added-" + id);
        return id;
    }

    public static int addUserData(String name, String email, String phone, String userType, String city,
            String document, String status) {
        Map<String, Object> data = new HashMap<String, Object>();
        int id = USER_SEQ;
        USER_SEQ = USER_SEQ + 1;
        data.put("id", id);
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("userType", userType);
        data.put("city", city);
        data.put("document", document);
        data.put("status", status);
        data.put("debt", 0.0);
        users.put(id, data);
        logs.add("user-added-" + id);
        return id;
    }

    public static int addLoanData(int bookId, int userId, String borrowDate, String dueDate, String returnedDate,
            String status, double fine, String notes) {
        Map<String, Object> data = new HashMap<String, Object>();
        int id = LOAN_SEQ;
        LOAN_SEQ = LOAN_SEQ + 1;
        data.put("id", id);
        data.put("bookId", bookId);
        data.put("userId", userId);
        data.put("borrowDate", borrowDate);
        data.put("dueDate", dueDate);
        data.put("returnedDate", returnedDate);
        data.put("status", status);
        data.put("fine", fine);
        data.put("notes", notes);
        loans.add(data);
        logs.add("loan-added-" + id);
        return id;
    }

    public static Map<String, Object> getBookById(int id) {
        return books.get(id);
    }

    public static Map<String, Object> getUserById(int id) {
        return users.get(id);
    }

    public static Map<String, Object> getLoanById(int id) {
        for (Map<String, Object> item : loans) {
            if (((Integer) item.get("id")).intValue() == id) {
                return item;
            }
        }
        return null;
    }

    public static void addLog(String value) {
        logs.add(value);
    }

    public static void seedInitialData() {
        if (books.size() > 0 || users.size() > 0) {
            return;
        }
        addBookData("Clean Code", "Robert C. Martin", 2008, "Software", 3, 3, "A1", "ISBN-111");
        addBookData("Design Patterns", "GoF", 1994, "Software", 2, 2, "A2", "ISBN-222");
        addBookData("Refactoring", "Martin Fowler", 1999, "Software", 4, 4, "A3", "ISBN-333");

        addUserData("Ana", "ana@mail.com", "1111-1111", "student", "Maringa", "DOC-1", "ACTIVE");
        addUserData("Bruno", "bruno@mail.com", "2222-2222", "teacher", "Maringa", "DOC-2", "ACTIVE");

        addLog("seed-loaded");
    }

    public static void dumpState() {
        System.out.println("BOOKS=" + books.size() + "; USERS=" + users.size() + "; LOANS=" + loans.size());
    }

    public static Map<Integer, Map<String, Object>> getBooks() {
        return books;
    }

    public static Map<Integer, Map<String, Object>> getUsers() {
        return users;
    }

    public static List<Map<String, Object>> getLoans() {
        return loans;
    }

    public static List<String> getLogs() {
        return logs;
    }

    public static void unsafeUpdateBookField(int id, String field, Object value) {
        Map<String, Object> b = books.get(id);
        if (b != null) {
            b.put(field, value);
            logs.add("book-updated-" + id + "-" + field);
        }
    }

    public static void unsafeUpdateUserField(int id, String field, Object value) {
        Map<String, Object> u = users.get(id);
        if (u != null) {
            u.put(field, value);
            logs.add("user-updated-" + id + "-" + field);
        }
    }

    public static String getSystemMode() {
        return SYSTEM_MODE;
    }

    public static void setSystemMode(String mode) {
        SYSTEM_MODE = mode;
        logs.add("mode-" + mode);
    }

    public static int countOpenLoansByUser(int userId) {
        int c = 0;
        for (Map<String, Object> loan : loans) {
            if (((Integer) loan.get("userId")).intValue() == userId) {
                if ("OPEN".equals(String.valueOf(loan.get("status")))) {
                    c++;
                }
            }
        }
        return c;
    }

    // REFACTORING (R3): was filtering by loan.get("userId") instead of loan.get("bookId").
    // The method is named countOpenLoansByBook, so the filter must use bookId.
    // This bug caused the availability check in LoanManager to return wrong counts.
    public static int countOpenLoansByBook(int bookId) {
        int c = 0;
        for (Map<String, Object> loan : loans) {
            // FIXED: now correctly filters by bookId, not userId.
            if (((Integer) loan.get("bookId")).intValue() == bookId) {
                if ("OPEN".equals(String.valueOf(loan.get("status")))) {
                    c++;
                }
            }
        }
        return c;
    }

    public static void printLogs() {
        for (String s : logs) {
            System.out.println(s);
        }
    }

    // REFACTORING (R4): replaced magic numbers 500 and 400 with named constants.
    // LOG_MAX_SIZE = maximum log entries before trimming.
    // LOG_TRIM_OFFSET = index from which to keep entries after trimming.
    public static void clearLogsIfTooBig() {
        if (logs.size() > LOG_MAX_SIZE) {
            List<String> tmp = new ArrayList<String>();
            for (int i = LOG_TRIM_OFFSET; i < logs.size(); i++) {
                tmp.add(logs.get(i));
            }
            logs = tmp;
        }
    }
}