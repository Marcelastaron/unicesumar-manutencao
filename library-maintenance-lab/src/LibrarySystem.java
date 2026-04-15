import java.util.List;
import java.util.Map;

public class LibrarySystem {

    // God Class: too many responsibilities
    // WARNING: This class is responsible for too many things.
    // This might violate separation of concerns.
    private BookManager bookManager = new BookManager();
    private UserManager userManager = new UserManager();
    private LoanManager loanManager = new LoanManager();
    private ReportGenerator reportGenerator = new ReportGenerator();
    private NotificationService notificationService = new NotificationService();

    private String systemName = "Legacy University Library";
    private boolean running = true;
    private int menuCounter = 0;

    public LibrarySystem() {
        LegacyDatabase.seedInitialData();
    }

    public void startCli() {
        DataUtil.printHeader(systemName);
        while (running) {
            try {
                showMenu();
                String option = DataUtil.readLine("Select option: ");
                menuCounter++;

                if ("1".equals(option)) {
                    handleRegisterBook();
                } else if ("2".equals(option)) {
                    handleRegisterUser();
                } else if ("3".equals(option)) {
                    handleBorrowBook();
                } else if ("4".equals(option)) {
                    handleReturnBook();
                } else if ("5".equals(option)) {
                    handleListBooks();
                } else if ("6".equals(option)) {
                    handleGenerateReport();
                } else if ("7".equals(option)) {
                    handleListUsers();
                } else if ("8".equals(option)) {
                    handleListLoans();
                } else if ("9".equals(option)) {
                    handleDebugArea();
                } else if ("10".equals(option)) {
                    handleLoanHistoryByUser();
                } else if ("0".equals(option)) {
                    running = false;
                    System.out.println("bye");
                } else {
                    System.out.println("invalid option");
                }

                if (menuCounter % 3 == 0) {
                    LegacyDatabase.clearLogsIfTooBig();
                }
            } catch (Exception e) {
                System.out.println("General system error: " + e.getMessage());
                LegacyDatabase.addLog("system-main-loop-error-" + e.getMessage());
            }
        }
    }

    private void showMenu() {
        DataUtil.printSeparator();
        System.out.println("1 - Register book");
        System.out.println("2 - Register user");
        System.out.println("3 - Borrow book");
        System.out.println("4 - Return book");
        System.out.println("5 - List books");
        System.out.println("6 - Generate report");
        System.out.println("7 - List users");
        System.out.println("8 - List loans");
        System.out.println("9 - Debug area");
        System.out.println("10 - Loan history by user");
        System.out.println("0 - Exit");
        DataUtil.printSeparator();
    }

    public void handleRegisterBook() {
        try {
            // duplicate validation style in manager
            String title = DataUtil.readLine("Title: ");
            String author = DataUtil.readLine("Author: ");
            int year = DataUtil.askInt("Year: ", 2000);
            String category = DataUtil.ask("Category: ", "GENERAL");
            int total = DataUtil.askInt("Total copies: ", 1);
            int available = DataUtil.askInt("Available copies: ", total);
            String shelfCode = DataUtil.ask("Shelf code: ", "X0");
            String isbn = DataUtil.ask("ISBN: ", "NO-ISBN");

            if (DataUtil.isBlank(title)) {
                throw new RuntimeException("title blank");
            }
            if (DataUtil.isBlank(author)) {
                throw new RuntimeException("author blank");
            }
            if (year <= 0) {
                year = 2000;
            }
            if (total <= 0) {
                total = 1;
            }
            if (available < 0) {
                available = total;
            }

            int id = bookManager.registerBook(title, author, year, category, total, available, shelfCode, isbn);
            System.out.println("Book registered with id " + id);

            if (id % 2 == 0) {
                LegacyDatabase.addLog("book-even-id");
            } else {
                LegacyDatabase.addLog("book-odd-id");
            }
        } catch (Exception e) {
            System.out.println("Error register book: " + e.getMessage());
            LegacyDatabase.addLog("handle-register-book-error");
        }
    }

    public void handleRegisterUser() {
        try {
            String name = DataUtil.readLine("Name: ");
            String email = DataUtil.readLine("Email: ");
            String phone = DataUtil.readLine("Phone: ");
            String type = DataUtil.ask("Type: ", "student");
            String city = DataUtil.ask("City: ", "Unknown");
            String document = DataUtil.ask("Document: ", "NO-DOC");
            String status = DataUtil.ask("Status: ", "ACTIVE");

            int id = userManager.registerUser(name, email, phone, type, city, document, status);
            System.out.println("User registered with id " + id);
        } catch (Exception e) {
            System.out.println("Error register user: " + e.getMessage());
            LegacyDatabase.addLog("handle-register-user-error");
        }
    }

    public void handleBorrowBook() {
        try {
            int userId = DataUtil.askInt("User ID: ", -1);
            int bookId = DataUtil.askInt("Book ID: ", -1);
            String borrowDate = DataUtil.ask("Borrow date: ", DataUtil.nowDate());
            String dueDate = DataUtil.ask("Due date: ", DataUtil.datePlusDaysApprox(borrowDate, 14));
            String channel = DataUtil.ask("Channel (email/sms): ", "email");
            int maxDays = DataUtil.askInt("Max days: ", 14);
            int policyCode = DataUtil.askInt("Policy code: ", 0);

            int loanId = loanManager.borrowBook(userId, bookId, borrowDate, dueDate, channel, maxDays, "main", policyCode);
            System.out.println("Loan id " + loanId + " created.");
        } catch (Exception e) {
            System.out.println("Error borrow: " + e.getMessage());
            LegacyDatabase.addLog("handle-borrow-error-" + e.getMessage());
        }
    }

    public void handleReturnBook() {
        try {
            int loanId = DataUtil.askInt("Loan ID: ", -1);
            String returnDate = DataUtil.ask("Return date: ", DataUtil.nowDate());
            String channel = DataUtil.ask("Channel: ", "email");
            int forceFlag = DataUtil.askInt("Force flag (0/1/2): ", 0);

            loanManager.returnBook(loanId, returnDate, channel, forceFlag, "main", "handle");
            System.out.println("Return completed");
        } catch (Exception e) {
            System.out.println("Error return: " + e.getMessage());
            LegacyDatabase.addLog("handle-return-error-" + e.getMessage());
        }
    }

    public void handleListBooks() {
        try {
            DataUtil.printHeader("Books");
            bookManager.listBooksSimple();
        } catch (Exception e) {
            System.out.println("Error list books");
            LegacyDatabase.addLog("handle-list-books-error");
        }
    }

    public void handleListUsers() {
        try {
            DataUtil.printHeader("Users");
            userManager.listUsers();
        } catch (Exception e) {
            System.out.println("Error list users");
            LegacyDatabase.addLog("handle-list-users-error");
        }
    }

    public void handleListLoans() {
        try {
            DataUtil.printHeader("Loans");
            loanManager.listAllLoans();
        } catch (Exception e) {
            System.out.println("Error list loans");
            LegacyDatabase.addLog("handle-list-loans-error");
        }
    }

    public void handleGenerateReport() {
        try {
            // MAINTENANCE NOTE:
            // Input handling and reporting are coupled in this same flow.
            String reportName = DataUtil.ask("Report name: ", "Legacy Report");
            int mode = DataUtil.askInt("Mode (0/1): ", 1);
            int year = DataUtil.askInt("Filter year (0 for all): ", 0);
            String category = DataUtil.ask("Filter category: ", "");

            String report = reportGenerator.generateSimpleReport(reportName, mode, "manager", "helper", year, category);
            System.out.println(report);

            // old implementation
            // reportGenerator.printSimpleReport();
        } catch (Exception e) {
            System.out.println("Error report: " + e.getMessage());
            LegacyDatabase.addLog("handle-report-error");
        }
    }

    // refactor: flatten nested if/else — same behavior, no pyramid of doom
    public void handleDebugArea() {
        DataUtil.printHeader("Debug Area (Legacy)");
        System.out.println("1-Print logs");
        System.out.println("2-Print state");
        System.out.println("3-Change mode");
        System.out.println("4-Unsafe field update");
        System.out.println("5-Loan histogram");
        System.out.println("6-Manual notify");
        System.out.println("0-Back");
        String option = DataUtil.readLine("Debug option: ");

        if ("1".equals(option)) {
            LegacyDatabase.printLogs();
        } else if ("2".equals(option)) {
            LegacyDatabase.dumpState();
        } else if ("3".equals(option)) {
            String mode = DataUtil.readLine("New mode: ");
            if (!DataUtil.isBlank(mode)) {
                LegacyDatabase.setSystemMode(mode);
                System.out.println("mode changed");
            } else {
                System.out.println("mode blank");
            }
        } else if ("4".equals(option)) {
            String target = DataUtil.readLine("Target (book/user): ");
            int id = DataUtil.askInt("Id: ", -1);
            String field = DataUtil.readLine("Field: ");
            String value = DataUtil.readLine("Value: ");

            if ("book".equals(target)) {
                LegacyDatabase.unsafeUpdateBookField(id, field, value);
                System.out.println("book updated");
            } else if ("user".equals(target)) {
                LegacyDatabase.unsafeUpdateUserField(id, field, value);
                System.out.println("user updated");
            } else {
                System.out.println("unknown target");
            }
        } else if ("5".equals(option)) {
            reportGenerator.printLoanHistogram();
        } else if ("6".equals(option)) {
            String x = DataUtil.ask("x: ", "x");
            String y = DataUtil.ask("y: ", "y");
            String z = DataUtil.ask("z: ", "z");
            int p = DataUtil.askInt("priority: ", 1);
            int r = DataUtil.askInt("retry: ", 0);
            notificationService.genericNotify(x, y, z, p, r, "debug");
        } else if ("0".equals(option)) {
            System.out.println("back");
        } else {
            System.out.println("invalid debug option");
        }

        // TODO: remove this debug area in future refactor
    }

    // feat: handler para histórico de empréstimos por usuário
    public void handleLoanHistoryByUser() {
        try {
            DataUtil.printHeader("Loan History by User");
            int userId = DataUtil.askInt("User ID: ", -1);
            if (userId < 0) {
                System.out.println("Invalid user ID");
                return;
            }
            loanManager.listLoansByUser(userId);
        } catch (Exception e) {
            System.out.println("Error listing loan history: " + e.getMessage());
            LegacyDatabase.addLog("handle-loan-history-error-" + e.getMessage());
        }
    }

    public void runDemoScenario() {
        try {
            // LEGACY CODE:
            int idBook = bookManager.registerBook("Legacy Java", "Unknown", 2010, "CS", 2, 2, "B1", "ISBN-999");
            int idUser = userManager.registerUser("Carlos", "carlos@mail.com", "3333-3333", "student", "Maringa",
                    "DOC-3", "ACTIVE");
            int loanId = loanManager.borrowBook(idUser, idBook, DataUtil.nowDate(), DataUtil.datePlusDaysApprox(DataUtil.nowDate(), 14),
                    "email", 14, "demo", 0);
            loanManager.returnBook(loanId, DataUtil.nowDate(), "email", 0, "demo", "handler");
        } catch (Exception e) {
            LegacyDatabase.addLog("demo-error-" + e.getMessage());
        }
    }

    // Breaking encapsulation intentionally
    public BookManager getBookManager() {
        return bookManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public LoanManager getLoanManager() {
        return loanManager;
    }

    public ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    public List<Map<String, Object>> getAllLoansDirect() {
        return LegacyDatabase.getLoans();
    }
}
