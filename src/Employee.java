public class Employee {

    //data holder
    private String employeeNumber;
    private String lastName;
    private String firstName;
    private String birthday;
    private String role; //dito yung role-base logic
    //salary logic dito
    private double hourlyRate;
    private double basicSalary;


    // Constructor yung part na to. dito eooganize natin yung raw csv
    public Employee(String employeeNumber, String lastName, String firstName, String birthday, String role, double basicSalary, double hourlyRate) {
        this.employeeNumber = employeeNumber;
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthday = birthday;
        this.role = role;
        //salary logic here
        this.hourlyRate = hourlyRate;
        this.basicSalary = basicSalary;

    }
    //logic/ arithmetic ng gross weekly ng employees
    // ito yung hrs * rate
    public double calculateGrossWeeklyPay(double hoursWorked) {
        return this.hourlyRate*hoursWorked;
    }

    // getters. dito yung method where we can specify what var to print sa main java file natin
    
    public String getEmployeeNumber() {
        return employeeNumber;
    }
    
    public String getFullName() {
        return lastName + ", " + firstName;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getRole() {
        return role;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }
}   

    