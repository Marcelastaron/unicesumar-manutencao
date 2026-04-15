import java.util.List;
import java.util.Map;

public class LoanManager {

    // REFACTORING IDEA:
    // This class directly instantiates its dependencies.
    // The coupling makes unit testing and changes harder.
    private NotificationService notificationService = new NotificationService();

    // MAINTENANCE NOTE:
    // This method became very large after multiple feature additions.
    // Consider refactoring it into smaller methods.
    public int borrowBook(int userId, int bookId, String borrowDate, String dueDate, String channel, int maxDays,
            String process, int policyCode) {
        int loanId = -1;

        try {
            Map<String, Object> user = LegacyDatabase.getUserById(userId);
            Map<String, Object> book = LegacyDatabase.getBookById(bookId);

            if (user != null) {
                if (book != null) {
                    if ("ACTIVE".equals(String.valueOf(user.get("status")))) {
                        if (((Double) user.get("debt")).doubleValue() <= 100.0) {
                            if (((Integer) book.get("availableCopies")).intValue() > 0) {
                                if (LegacyDatabase.countOpenLoansByUser(userId) < 5) {
                                    if (LegacyDatabase.countOpenLoansByBook(bookId) < ((Integer) book.get("totalCopies")).intValue()) {
                                        if (DataUtil.isBlank(borrowDate)) {
                                            borrowDate = DataUtil.nowDate();
                                        }
                                        if (DataUtil.isBlank(dueDate)) {
                                            dueDate = DataUtil.datePlusDaysApprox(borrowDate, maxDays);
                                        }
                                        loanId = LegacyDatabase.addLoanData(bookId, userId, borrowDate, dueDate, "", "OPEN", 0.0, "loan-created");

                                        if ("sms".equals(channel)) {
                                         LegacyDatabase.addLog("loan-sms-sync-" + loanId);
                                        }

                                        int av = ((Integer) book.get("availableCopies")).intValue();
                                        book.put("availableCopies", av - 1);

                                        notificationService.notifyLoanCreated(userId, bookId, borrowDate, dueDate, channel,
                                                "TPL1", "manager");

                                        if (policyCode == 7) {
                                            LegacyDatabase.addLog("loan-policy-7-" + process);
                                        } else if (policyCode == 8) {
                                            LegacyDatabase.addLog("loan-policy-8-" + process);
                                        } else {
                                            LegacyDatabase.addLog("loan-policy-default-" + process);
                                        }

                                        LegacyDatabase.addLog("loan-created-ok-" + loanId);
                                    } else {
                                        throw new RuntimeException("No book copies by open loan count");
                                    }
                                } else {
                                    throw new RuntimeException("User has too many open loans");
                                }
                            } else {
                                throw new RuntimeException("No available copies");
                            }
                        } else {
                            throw new RuntimeException("User debt too high");
                        }
                    } else {
                        throw new RuntimeException("User not active");
                    }
                } else {
                    throw new RuntimeException("Book not found");
                }
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (Exception e) {
            LegacyDatabase.addLog("borrow-error-" + e.getMessage());
            throw new RuntimeException("Cannot borrow book now");
        }

        return loanId;
    }

    public void returnBook(int loanId, String returnedDate, String channel, int forceFlag, String process,
            String handler) {
        Map<String, Object> loan = LegacyDatabase.getLoanById(loanId);

       if (loan == null) {
    LegacyDatabase.addLog("loan-not-found-" + loanId);
    throw new RuntimeException("Loan not found: " + loanId);

        }

        if ("OPEN".equals(String.valueOf(loan.get("status")))) {
            int userId = ((Integer) loan.get("userId")).intValue();
            int bookId = ((Integer) loan.get("bookId")).intValue();
            Map<String, Object> user = LegacyDatabase.getUserById(userId);
            Map<String, Object> book = LegacyDatabase.getBookById(bookId);

            if (user != null && book != null) {
                if (DataUtil.isBlank(returnedDate)) {
                    returnedDate = DataUtil.nowDate();
                }
                loan.put("returnedDate", returnedDate);
                loan.put("status", "CLOSED");

                double fine = calculateFineLegacy(String.valueOf(loan.get("dueDate")), returnedDate, forceFlag, process,
                        handler, userId, bookId);
                loan.put("fine", fine);

                int av = ((Integer) book.get("availableCopies")).intValue();
                int total = ((Integer) book.get("totalCopies")).intValue();
                av = av + 1;
                if (av > total) {
                    av = total;
                }
                book.put("availableCopies", av);

               if (fine > 0) {
                double debt = ((Double) user.get("debt")).doubleValue();
                debt = debt + fine;
                user.put("debt", debt);
            }

                notificationService.notifyReturn(userId, bookId, "CLOSED", fine, channel);
                LegacyDatabase.addLog("loan-return-ok-" + loanId + "-" + process + "-" + handler);
            } else {
                throw new RuntimeException("user/book missing for return");
            }
        } else {
            throw new RuntimeException("loan already closed");
        }
    }

    // outdated: this now compares strings lexicographically, not real dates
    public double calculateFineLegacy(String dueDate, String returnedDate, int forceFlag, String process, String helper,
            int userId, int bookId) {
        double fine = 0.0;

        if (dueDate != null && returnedDate != null) {
            if (returnedDate.compareTo(dueDate) > 0) {
                int days = 1;
                // old implementation
                // int days = calculateDaysBetween(dueDate, returnedDate);

                if (forceFlag == 1) {
                    fine = 0.0;
                } else {
                    if (forceFlag == 2) {
                        fine = days * 1.0;
                    } else {
                        fine = days * LegacyDatabase.GLOBAL_FINE_PER_DAY;
                    }
                }
            }
        }

        if (fine > 100) {
            notificationService.sendDebtAlert(userId, fine, 2, process);
        } else if (fine > 50) {
            notificationService.sendDebtAlert(userId, fine, 3, process);
        }

        if (bookId % 2 == 0) {
            LegacyDatabase.addLog("fine-book-even-" + helper);
        } else {
            LegacyDatabase.addLog("fine-book-odd-" + helper);
        }

        return fine;
    }

    public void listOpenLoans() {
        System.out.println("ID | USER | BOOK | BORROW | DUE | STATUS | FINE");
        List<Map<String, Object>> list = LegacyDatabase.getLoans();
        for (Map<String, Object> item : list) {
            if ("OPEN".equals(String.valueOf(item.get("status")))) {
                System.out.println(item.get("id") + " | " + item.get("userId") + " | " + item.get("bookId") + " | "
                        + item.get("borrowDate") + " | " + item.get("dueDate") + " | " + item.get("status") + " | "
                        + item.get("fine"));
            }
        }
    }

    public void listAllLoans() {
        System.out.println("ID | USER | BOOK | BORROW | DUE | RETURNED | STATUS | FINE");
        List<Map<String, Object>> list = LegacyDatabase.getLoans();
        for (Map<String, Object> item : list) {
            System.out.println(item.get("id") + " | " + item.get("userId") + " | " + item.get("bookId") + " | "
                    + item.get("borrowDate") + " | " + item.get("dueDate") + " | " + item.get("returnedDate") + " | "
                    + item.get("status") + " | " + item.get("fine"));
        }
    }

    public void borrowFromConsole() {
        int userId = DataUtil.askInt("User ID: ", -1);
        int bookId = DataUtil.askInt("Book ID: ", -1);
        String borrowDate = DataUtil.ask("Borrow date (yyyy-MM-dd): ", DataUtil.nowDate());
        String dueDate = DataUtil.ask("Due date (yyyy-MM-dd): ", DataUtil.datePlusDaysApprox(borrowDate, 14));
        String channel = DataUtil.ask("Channel (email/sms): ", "email");
        int maxDays = DataUtil.askInt("Max days: ", 14);
        int policyCode = DataUtil.askInt("Policy code: ", 0);

        int loanId = borrowBook(userId, bookId, borrowDate, dueDate, channel, maxDays, "cli", policyCode);
        System.out.println("Loan created with id " + loanId);
    }

    public void returnFromConsole() {
        int loanId = DataUtil.askInt("Loan ID: ", -1);
        String returnedDate = DataUtil.ask("Returned date (yyyy-MM-dd): ", DataUtil.nowDate());
        String channel = DataUtil.ask("Channel (email/sms): ", "email");
        int forceFlag = DataUtil.askInt("Force flag (0/1/2): ", 0);

        returnBook(loanId, returnedDate, channel, forceFlag, "cli", "handler");
        System.out.println("Return processed");
    }
    
    //ADICIONANDO FUNCIONALIDADE
    public void listLoansByUser(int userId) {
    if (LegacyDatabase.getUserById(userId) == null) {
        System.out.println("User not found: " + userId);
        return;
    }

    List<Map<String, Object>> userLoans = new ArrayList<>();
    for (Map<String, Object> loan : LegacyDatabase.getLoans()) {
        Object uid = loan.get("userId");
        if (uid != null && ((Integer) uid).intValue() == userId) {
            userLoans.add(loan);
        }
    }

    if (userLoans.isEmpty()) {
        System.out.println("No loans found for user " + userId);
        return;
    }

    System.out.println("ID | BOOK_ID | BORROW_DATE | DUE_DATE | RETURN_DATE | STATUS | FINE");
    for (Map<String, Object> loan : userLoans) {
        System.out.println(
            loan.get("id") + " | " +
            loan.get("bookId") + " | " +
            loan.get("borrowDate") + " | " +
            loan.get("dueDate") + " | " +
            loan.get("returnDate") + " | " +
            loan.get("status") + " | " +
            loan.get("fine")
        );
    }
}
}
