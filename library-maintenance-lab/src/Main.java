 public class Main {

    public static void main(String[] args) {

        LibrarySystem system = new LibrarySystem();

        System.out.println("Starting legacy library system...");
        System.out.println("Mode: " + LegacyDatabase.getSystemMode());

        system.runDemoScenario();

        if (args != null && args.length > 0) {
            if ("--report".equals(args[0])) {
                String report = system.getReportGenerator().generateSimpleReport(
                        "Startup Report", 1, "main", "helper", 0, "");
                System.out.println(report);
                return;
            }
            if ("--list".equals(args[0])) {
                system.handleListBooks();
                system.handleListUsers();
                system.handleListLoans();
                return;
            }
        }

        system.startCli();
    }

}
  