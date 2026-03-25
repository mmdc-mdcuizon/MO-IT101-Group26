import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MotorPH_Payrollv1 {

    // fixed: based dun sa sabi ni mentor.
    static String empFile = "data/employees.csv"; 
    static String attFile = "data/attendance.csv";
    
    static int year = 2024;
    static int startMonth = 6;
    static int endMonth = 12;
    static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("H:mm");
    
    // Hashmap suggestion ni mentor din. actually di ko pa masyado maintindihan to so research nalang din about hashmaps and hashbrowns x.x
    static Map<String, Map<Integer, double[]>> attendanceData = new HashMap<>();

    // ================= MAIN METHOD =================
    public static void main(String[] args) throws Exception {
        
        // Loads attendance data ONCE at the start of the program
        loadAttendanceData();

        Scanner Login = new Scanner(System.in);
        boolean keepRunning = true; // nagloop ako dito para di mag exit yung after ng task ah. see below

        // lahat ng nandito magloloop lang hanggang mag "false" o then opt to exit
        // as the peer evaluators suggested
        while (keepRunning) {
            System.out.println("\n===== MOTOR PH SYSTEM =====");
            System.out.print("Username: ");
            String username = Login.nextLine();

            System.out.print("Password: ");
            String password = Login.nextLine();

            if (!password.equals("12345")) {
                System.out.println("Incorrect password or username.");
            } else {
                // Routes the user to different menus based on who logged in
                if (username.equals("employee")) {
                    employeeMenu(Login);
                } else if (username.equals("payrollstaff")) {
                    payrollMenu(Login);
                } else {
                    System.out.println("Invalid username.");
                }
            }

        
            System.out.print("\nDo you want to log in again or perform another transaction? (Y/N): ");
            String choice = Login.nextLine().trim().toUpperCase();

           // press N exit na to sya 
            if (choice.equals("N")) {
                keepRunning = false;
                System.out.println("Exiting MotorPH Payroll System. Have a great day!");
            }
        }

        //baka di nyo makita, dito magcclose yung scanner(keyboard input) pagka N
        Login.close();
    }

    // ================= EMPLOYEE MENU =================
    static void employeeMenu(Scanner sc) throws Exception {

        System.out.print("Enter Employee #: ");
        String empNo = sc.nextLine().trim();

        String[] emp = findEmployee(empNo);

        if (emp == null) {
            System.out.println("Employee not found.");
            return;
        }

        System.out.println("Employee #: " + emp[0]);
        System.out.println("Name: " + emp[1] + ", " + emp[2]);
        System.out.println("Birthday: " + emp[3]);
    }

    // ================= PAYROLL MENU =================
    static void payrollMenu(Scanner sc) throws Exception {

        System.out.println("1. One employee");
        System.out.println("2. All employees");
        System.out.print("Choose: ");

        int choice = sc.nextInt();
        sc.nextLine();

        if (choice == 1) {
            System.out.print("Enter Employee #: ");
            String empNo = sc.nextLine().trim();
            processOneEmployee(empNo);
        } else if (choice == 2) {
            processAllEmployees();
        } else {
            System.out.println("Invalid choice.");
        }
    }

    // ================= PROCESS ONE =================
    static void processOneEmployee(String empNo) throws Exception {

        String[] emp = findEmployee(empNo);

        if (emp == null) {
            System.out.println("Employee not found.");
            return;
        }

        processPayroll(emp);
    }

    // ================= PROCESS ALL =================
    static void processAllEmployees() throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(empFile));
        br.readLine(); 

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] emp = splitEmployeeCSV(line);
            processPayroll(emp);
        }

        br.close();
    }

    // ================= CORE PAYROLL =================
    static void processPayroll(String[] emp) throws Exception {

        String bsText = emp[13].trim();
        String hrText = emp[18].trim();

        double basicSalary = Double.parseDouble(cleanNumber(bsText));
        double hourlyRate = Double.parseDouble(cleanNumber(hrText));

       
        // as per mentor's advice, nilagay ko na dito yung emp name and ID para once nalang sya mag print'
        System.out.println("\n=================================");
        System.out.println("Employee #: " + emp[0]);
        System.out.println("Name: " + emp[1] + ", " + emp[2]);
        System.out.println("=================================");

        for (int month = startMonth; month <= endMonth; month++) {
            printMonthlyPayroll(emp, basicSalary, hourlyRate, month);
        }
    }

    // ================= MONTHLY PAYROLL =================
    static void printMonthlyPayroll(String[] emp, double basicSalary, double hourlyRate, int month) throws Exception {

        double hours1 = 0;
        double hours2 = 0;

       
        // dito na mag grab sa hours from "hashmap" para di nag magpabalik-balik mag read ng file yung program as per mentor's advice'
        if (attendanceData.containsKey(emp[0].trim())) {
            Map<Integer, double[]> monthlyData = attendanceData.get(emp[0].trim());

            if (monthlyData.containsKey(month)) {
                double[] cutoffHours = monthlyData.get(month);
                hours1 = cutoffHours[0];
                hours2 = cutoffHours[1];
            }
        }

        // If no hours were worked this month, skip printing the payslip
        if (hours1 == 0 && hours2 == 0) {
            return;
        }

        double gross1 = hours1 * hourlyRate;
        double gross2 = hours2 * hourlyRate;
        double totalGross = gross1 + gross2;

        double[] deductions = computeDeductions(basicSalary, totalGross);

        double net1 = gross1;
        double net2 = gross2 - deductions[4];

        // --- plaintext formatting na pina generate ko lang kay Gemini para mas maayos tignan.


        System.out.println("\n=========================================================");
        System.out.printf("                  PAYSLIP: %d / %d\n", month, year);
        System.out.println("=========================================================");
        
        System.out.println("[ EARNINGS ]");
        System.out.println("                       Hours              Gross              Net");
        
       
        System.out.printf("  1st Half (Day 1-15): %-18s %-18s %s\n", hours1, gross1, net1);
        System.out.printf("  2nd Half (Day 16+) : %-18s %-18s (See Below)\n", hours2, gross2);
        
        System.out.println("\n[ GOVERNMENT DEDUCTIONS ]");
        System.out.printf("  SSS Contribution   : %s\n", deductions[0]);
        System.out.printf("  PhilHealth         : %s\n", deductions[1]);
        System.out.printf("  Pag-IBIG           : %s\n", deductions[2]);
        System.out.printf("  Withholding Tax    : %s\n", deductions[3]);
        System.out.println("  -------------------------------------------------------");
        System.out.printf("  Total Deductions   : %s\n", deductions[4]);
        
        System.out.println("=========================================================");
        System.out.printf("  FINAL 2ND CUTOFF NET PAY: %s\n", net2);
        System.out.println("=========================================================\n");
    }

    // ================= HOURS LOGIC =================
    static double computeWorkHours(LocalTime login, LocalTime logout) {

        LocalTime start = LocalTime.of(8, 0);
        LocalTime grace = LocalTime.of(8, 10);
        LocalTime end = LocalTime.of(17, 0);

        if (logout.isAfter(end)) logout = end;
        if (!login.isAfter(grace)) login = start;
        if (login.isBefore(start)) login = start;
        if (login.isAfter(logout)) return 0;

        long minutes = Duration.between(login, logout).toMinutes();

        if (minutes >= 60) minutes -= 60; // lunch break
        else minutes = 0;

        if (minutes > 480) minutes = 480; // max 8 hours

        return minutes / 60.0;
    }

    // ================= DEDUCTIONS =================
    // Calculates SSS, PhilHealth, Pag-IBIG, and Tax based on MotorPH 2024 contribution tables.
    static double[] computeDeductions(double basicSalary, double totalGross) {

        double sss = 0;
        if (totalGross <= 3250) { sss = 135.0; } 
        else if (totalGross >= 24750) { sss = 1125.0; } 
        else {
            int multiplier = (int) ((totalGross - 3250) / 500) + 1;
            sss = 135.0 + (multiplier * 22.50);
        }

        double philhealth = 0;
        if (totalGross <= 10000) { philhealth = 150.0; } 
        else if (totalGross >= 60000) { philhealth = 900.0; } 
        else { philhealth = (totalGross * 0.03) / 2; }

        double pagibig = 0;
        if (totalGross > 1500) { pagibig = totalGross * 0.02; } 
        else { pagibig = totalGross * 0.01; }
        
        if (pagibig > 100) { pagibig = 100.0; } 

        double totalContributions = sss + philhealth + pagibig;
        double taxableIncome = totalGross - totalContributions;
        double tax = 0;

        if (taxableIncome > 666667) { tax = 200833.33 + ((taxableIncome - 666667) * 0.35); } 
        else if (taxableIncome > 166667) { tax = 40833.33 + ((taxableIncome - 166667) * 0.32); } 
        else if (taxableIncome > 66667) { tax = 10833.0 + ((taxableIncome - 66667) * 0.30); } 
        else if (taxableIncome > 33333) { tax = 2500.0 + ((taxableIncome - 33333) * 0.25); } 
        else if (taxableIncome > 20833) { tax = (taxableIncome - 20833) * 0.20; } 

        double total = sss + philhealth + pagibig + tax;

        return new double[]{sss, philhealth, pagibig, tax, total};
    }

    static String cleanNumber(String s) {
        return s.replace("\"", "").replace(",", "").trim();
    }

    // ================= HELPER: SPLIT CSV =================
    static String[] splitEmployeeCSV(String line) {

        String[] fields = new String[19];
        boolean inQuotes = false;
        String current = "";
        int index = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                if (index < fields.length) fields[index] = current.trim();
                current = "";
                index++;
            } else {
                current = current + c;
            }
        }

        if (index < fields.length) fields[index] = current.trim();

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) fields[i] = "";
        }

        return fields;
    }

    // ================= HELPER: FIND EMPLOYEE =================
    static String[] findEmployee(String empNo) throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(empFile));
        br.readLine(); 

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] emp = splitEmployeeCSV(line);

            if (emp[0].trim().equals(empNo)) {
                br.close();
                return emp;
            }
        }

        br.close();
        return null;
    }
    
    // ================= LOAD ATTENDANCE DATA (mentor's fix suggestion') =================
    static void loadAttendanceData() throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(attFile));
        br.readLine();

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            // @jared: inedit ko lang vars mo, nag single letter var ka na naman
            String[] attendanceRow = line.split(",");

            String empNo = attendanceRow[0].trim();

            String[] dateParts = attendanceRow[3].trim().split("/");
            int monthValue = Integer.parseInt(dateParts[0]);
            int dayValue = Integer.parseInt(dateParts[1]);
            int yearValue = Integer.parseInt(dateParts[2]);

            if (yearValue != year) continue;
            if (monthValue < startMonth || monthValue > endMonth) continue;

            LocalTime login = LocalTime.parse(attendanceRow[4].trim(), timeFormat);
            LocalTime logout = LocalTime.parse(attendanceRow[5].trim(), timeFormat);

            double hoursWorked = computeWorkHours(login, logout);

            if (!attendanceData.containsKey(empNo)) {
                attendanceData.put(empNo, new HashMap<Integer, double[]>());
            }

            Map<Integer, double[]> monthlyData = attendanceData.get(empNo);

            if (!monthlyData.containsKey(monthValue)) {
                monthlyData.put(monthValue, new double[]{0, 0});
            }

            double[] cutoffHours = monthlyData.get(monthValue);

            if (dayValue <= 15) {
                cutoffHours[0] += hoursWorked;
            } else {
                cutoffHours[1] += hoursWorked;
            }
        }

        br.close();
    }
}

