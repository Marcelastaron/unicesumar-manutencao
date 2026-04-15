import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportGenerator {

    // REFACTORING (R5):
    // Bug 1 - totalLoans had a hardcoded "+1" with no business justification. Removed.
    // Bug 2 - closedLoans was incrementing for every loan regardless of status. 
    public String generateSimpleReport(String reportName, int mode, String manager, String helper, int yearFilter,
            String categoryFilter) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORT: ").append(reportName).append(" ===\n");
        sb.append("mode=").append(mode).append(" manager=").append(manager).append(" helper=").append(helper).append("\n");

        Map<Integer, Map<String, Object>> books = LegacyDatabase.getBooks();
        Map<Integer, Map<String, Object>> users = LegacyDatabase.getUsers();
        List<Map<String, Object>> loans = LegacyDatabase.getLoans();

        int totalBooks = books.size();
        int totalUsers = users.size();
        // REFACTORING (R5 - Bug 1): removed the hardcoded "+1" adjustment.
        // There is no business rule that justifies inflating the loan count.
        int totalLoans = loans.size();
        int openLoans = 0;
        int closedLoans = 0;

        for (Map<String, Object> loan : loans) {
            if ("OPEN".equals(String.valueOf(loan.get("status")))) {
                openLoans++;
            }
            // REFACTORING (R5 - Bug 2): was incrementing closedLoans for every loan.
            // Now only increments when the loan status is actually "CLOSED".
            if ("CLOSED".equals(String.valueOf(loan.get("status")))) {
                closedLoans++;
            }
        }
        sb.append("Books: ").append(totalBooks).append("\n");
        sb.append("Users: ").append(totalUsers).append("\n");
        sb.append("Loans: ").append(totalLoans).append("\n");
        sb.append("Open loans: ").append(openLoans).append("\n");
        sb.append("Closed loans: ").append(closedLoans).append("\n");

        sb.append("\nBooks detail:\n");
        for (Map<String, Object> b : books.values()) {
            int y = ((Integer) b.get("year")).intValue();
            String c = String.valueOf(b.get("category"));
            if ((yearFilter <= 0 || y == yearFilter) && (DataUtil.isBlank(categoryFilter) || categoryFilter.equals(c))) {
                sb.append(" - ").append(b.get("id")).append(" | ").append(b.get("title")).append(" | ").append(b.get("author"))
                        .append(" | year=").append(y).append(" | cat=").append(c).append(" | av=")
                        .append(b.get("availableCopies")).append("\n");
            }
        }

        sb.append("\nUsers with debt:\n");
        for (Map<String, Object> u : users.values()) {
            double debt = ((Double) u.get("debt")).doubleValue();
            if (debt > 0) {
                sb.append(" - ").append(u.get("id")).append(" | ").append(u.get("name")).append(" | debt=").append(debt)
                        .append(" | status=").append(u.get("status")).append("\n");
            }
        }

        if (mode == 1) {
            sb.append("\nRecent logs:\n");
            List<String> logs = LegacyDatabase.getLogs();
            int start = logs.size() - 10;
            if (start < 0) {
                start = 0;
            }
            for (int i = start; i < logs.size(); i++) {
                sb.append(" * ").append(logs.get(i)).append("\n");
            }
        }

        LegacyDatabase.addLog("report-generated-" + reportName + "-" + manager + "-" + helper);
        return sb.toString();
    }

    public void printSimpleReport() {
        String r = generateSimpleReport("Legacy Library", 1, "manager", "helper", 0, "");
        System.out.println(r);
    }

    public Map<String, Integer> countLoansByUser() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (Map<String, Object> loan : LegacyDatabase.getLoans()) {
            String uid = String.valueOf(loan.get("userId"));
            Integer c = map.get(uid);
            if (c == null) {
                c = 0;
            }
            c = c + 1;
            map.put(uid, c);
        }
        return map;
    }

    public void printLoanHistogram() {
        Map<String, Integer> map = countLoansByUser();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            String bar = DataUtil.repeat("#", e.getValue());
            System.out.println("User " + e.getKey() + " -> " + bar);
        }
    }
}
