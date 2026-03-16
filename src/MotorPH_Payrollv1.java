import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
// utils na ginamit for this project
public class MotorPH_Payrollv1 {

    // local folder ko lang to. yung files kinuha natin sa github ni sir
    static String empFile = "/home/sirmarcdens/Documents/MAPUA/Practice Files/Java/MotorPH_Payroll/data/employees.csv"; 
    static String attFile = "/home/sirmarcdens/Documents/MAPUA/Practice Files/Java/MotorPH_Payroll/data/attendance.csv";
    static int year = 2024;
    static int startMonth = 6;
    static int endMonth = 12;
    static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) throws Exception {

        Scanner Login = new Scanner(System.in);

        System.out.println("===== MOTOR PH SYSTEM =====");
        System.out.print("Username: ");
        String username = Login.nextLine();

        System.out.print("Password: ");
        String password = Login.nextLine();

        if (!password.equals("12345")) {
            System.out.println("Incorrect password or username.");
            Login.close();
            return;
        }

        if (username.equals("employee")) {
            employeeMenu(Login);
        } else if (username.equals("payrollstaff")) {
            payrollMenu(Login);
        } else {
            System.out.println("Invalid username.");
        }

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
        br.readLine(); // header

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] emp = splitEmployeeCSV(line);
            processPayroll(emp);
        }

        br.close();
    }
// payroll staff na part
    // ================= CORE PAYROLL =================
    static void processPayroll(String[] emp) throws Exception {

        String bsText = emp[13].trim();
        String hrText = emp[18].trim();


        double basicSalary = Double.parseDouble(cleanNumber(bsText));
        double hourlyRate = Double.parseDouble(cleanNumber(hrText));

        for (int month = startMonth; month <= endMonth; month++) {
            printMonthlyPayroll(emp, basicSalary, hourlyRate, month);
        }
    }

    // ================= MONTHLY PAYROLL =================
    static void printMonthlyPayroll(String[] emp, double basicSalary, double hourlyRate, int month) throws Exception {

        double hours1 = 0;
        double hours2 = 0;

        BufferedReader br = new BufferedReader(new FileReader(attFile));
        br.readLine(); // header

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] a = line.split(",");

            if (!a[0].trim().equals(emp[0].trim())) continue;

            String[] dateParts = a[3].trim().split("/");
            int m = Integer.parseInt(dateParts[0]);
            int d = Integer.parseInt(dateParts[1]);
            int y = Integer.parseInt(dateParts[2]);

            if (m != month || y != year) continue;

            LocalTime login = LocalTime.parse(a[4].trim(), timeFormat);
            LocalTime logout = LocalTime.parse(a[5].trim(), timeFormat);

            double h = computeWorkHours(login, logout);

            if (d <= 15) hours1 += h;
            else hours2 += h;
        }

        br.close();

        double gross1 = hours1 * hourlyRate;
        double gross2 = hours2 * hourlyRate;

        double totalGross = gross1 + gross2;

        double[] deductions = computeDeductions(basicSalary, totalGross);

        double net1 = gross1;
        double net2 = gross2 - deductions[4];

        System.out.println("=================================");
        System.out.println("Month: " + month + "/" + year);
        System.out.println("Employee #: " + emp[0]);
        System.out.println("Name: " + emp[1] + ", " + emp[2]);

        System.out.println("Cutoff 1 (1-15) Hours: " + hours1);
        System.out.println("Cutoff 1 Gross: " + gross1);
        System.out.println("Cutoff 1 Net: " + net1);

        System.out.println("Cutoff 2 (16-end) Hours: " + hours2);
        System.out.println("Cutoff 2 Gross: " + gross2);
        
        System.out.println("---------------------------------");
        System.out.println("SSS Deduction: " + deductions[0]);
        System.out.println("PhilHealth Deduction: " + deductions[1]);
        System.out.println("Pag-IBIG Deduction: " + deductions[2]);
        System.out.println("Withholding Tax: " + deductions[3]);
        System.out.println("Total Deductions (after gross1 + gross2): " + deductions[4]);
        System.out.println("---------------------------------");
        System.out.println("Cutoff 2 Net: " + net2);
        System.out.println("=================================");
    }

    // ================= HOURS <ogic natin =================
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
    // yung logic dito base lang sa binigay na data ni motorph. parang typical pinoy deduction lang to. magdedepende nalang kung ilan sweldo
    static double[] computeDeductions(double basicSalary, double totalGross) {

        //sss
        double sss = 0;
        if (totalGross <= 3250) { sss = 135.0; } 
        else if (totalGross >= 24750) { sss = 1125.0; } 
        else {
            int multiplier = (int) ((totalGross - 3250) / 500) + 1;
            sss = 135.0 + (multiplier * 22.50);
        }

        // 2. PhilHealth 
        double philhealth = 0;
        if (totalGross <= 10000) { philhealth = 150.0; } 
        else if (totalGross >= 60000) { philhealth = 900.0; } 
        else { philhealth = (totalGross * 0.03) / 2; }

        // 3. Pag-IBIG 
        double pagibig = 0;
        if (totalGross > 1500) { pagibig = totalGross * 0.02; } 
        else { pagibig = totalGross * 0.01; }
        
        if (pagibig > 100) { pagibig = 100.0; } // Max cap

        // 4. Withholding Tax 
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

    // ito dito na yung comma separator. medjo madugo yung part na to hahaha
    // mas madaling basahin to compared dun sa regex na initial na ginawa ko. ty jared xD
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

    static String[] findEmployee(String empNo) throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(empFile));
        br.readLine(); // header

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
}