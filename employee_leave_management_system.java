import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Simple Employee Leave Management System (single-file Java Swing app)
 * - File-based persistence using Java serialization (leave_data.ser)
 * - Two tabs: Employee (apply, view balance & history) and Admin (approve/reject)
 *
 * To compile: javac LeaveManagementApp.java
 * To run:     java LeaveManagementApp
 */
public class LeaveManagementApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LeaveManager manager = LeaveManager.loadFromFile();
            if (manager == null) manager = LeaveManager.createSampleData();
            new MainFrame(manager);
        });
    }

}

// ---------------------------- Data classes ----------------------------
class Employee implements Serializable {
    String id;
    String name;
    int leaveBalance; // days
    List<LeaveApplication> history = new ArrayList<>();

    Employee(String id, String name, int leaveBalance) {
        this.id = id;
        this.name = name;
        this.leaveBalance = leaveBalance;
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }
}

class LeaveApplication implements Serializable {
    static final long serialVersionUID = 1L;
    static int nextId = 1;

    int appId;
    String empId;
    String fromDate;
    String toDate;
    int days;
    String reason;
    String status; // PENDING / APPROVED / REJECTED

    LeaveApplication(String empId, String fromDate, String toDate, int days, String reason) {
        this.appId = nextId++;
        this.empId = empId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.days = days;
        this.reason = reason;
        this.status = "PENDING";
    }
}

class LeaveManager implements Serializable {
    static final long serialVersionUID = 1L;
    Map<String, Employee> employees = new LinkedHashMap<>();
    List<LeaveApplication> applications = new ArrayList<>();

    // persistence file
    static final String DATA_FILE = "leave_data.ser";

    void addEmployee(Employee e) { employees.put(e.id, e); }

    LeaveApplication applyLeave(String empId, String from, String to, int days, String reason) {
        Employee e = employees.get(empId);
        if (e == null) return null;
        LeaveApplication app = new LeaveApplication(empId, from, to, days, reason);
        applications.add(app);
        e.history.add(app);
        saveToFile();
        return app;
    }

    boolean approve(int appId) {
        LeaveApplication app = findApp(appId);
        if (app == null || !app.status.equals("PENDING")) return false;
        Employee e = employees.get(app.empId);
        if (e.leaveBalance >= app.days) {
            e.leaveBalance -= app.days;
            app.status = "APPROVED";
            saveToFile();
            return true;
        } else {
            return false;
        }
    }

    boolean reject(int appId) {
        LeaveApplication app = findApp(appId);
        if (app == null || !app.status.equals("PENDING")) return false;
        app.status = "REJECTED";
        saveToFile();
        return true;
    }

    LeaveApplication findApp(int id) {
        for (LeaveApplication a : applications) if (a.appId == id) return a;
        return null;
    }

    void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static LeaveManager loadFromFile() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            LeaveManager m = (LeaveManager) ois.readObject();
            // restore nextId for future applications
            int maxId = 0;
            for (LeaveApplication a : m.applications) if (a.appId > maxId) maxId = a.appId;
            LeaveApplication.nextId = maxId + 1;
            return m;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    static LeaveManager createSampleData() {
        LeaveManager m = new LeaveManager();
        m.addEmployee(new Employee("E001", "Alice", 18));
        m.addEmployee(new Employee("E002", "Bob", 12));
        m.addEmployee(new Employee("E003", "Charlie", 20));
        m.saveToFile();
        return m;
    }
}

// ---------------------------- GUI ----------------------------
class MainFrame extends JFrame {
    LeaveManager manager;

    JComboBox<Employee> empCombo;
    JLabel balanceLabel;
    DefaultTableModel historyModel;

    DefaultTableModel adminModel;

    MainFrame(LeaveManager manager) {
        super("Employee Leave Management System");
        this.manager = manager;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Employee", createEmployeePanel());
        tabs.add("Admin", createAdminPanel());

        add(tabs);
        setVisible(true);
    }

    private JPanel createEmployeePanel() {
        JPanel p = new JPanel(new BorderLayout(10,10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        empCombo = new JComboBox<>(manager.employees.values().toArray(new Employee[0]));
        empCombo.addActionListener(e -> refreshEmployeeView());
        top.add(new JLabel("Select Employee:"));
        top.add(empCombo);

        balanceLabel = new JLabel("Leave balance: -");
        top.add(balanceLabel);

        p.add(top, BorderLayout.NORTH);

        // Apply form
        JPanel form = new JPanel(new GridLayout(5,2,8,8));
        form.setBorder(BorderFactory.createTitledBorder("Apply for Leave"));
        JTextField fromField = new JTextField();
        JTextField toField = new JTextField();
        JTextField daysField = new JTextField();
        JTextField reasonField = new JTextField();
        JButton applyBtn = new JButton("Apply");

        form.add(new JLabel("From (YYYY-MM-DD):")); form.add(fromField);
        form.add(new JLabel("To   (YYYY-MM-DD):")); form.add(toField);
        form.add(new JLabel("Days:")); form.add(daysField);
        form.add(new JLabel("Reason:")); form.add(reasonField);
        form.add(new JLabel()); form.add(applyBtn);

        p.add(form, BorderLayout.WEST);

        // History table
        historyModel = new DefaultTableModel(new String[]{"AppID","From","To","Days","Reason","Status"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        JTable historyTable = new JTable(historyModel);
        JScrollPane sp = new JScrollPane(historyTable);
        sp.setBorder(BorderFactory.createTitledBorder("Leave History"));
        p.add(sp, BorderLayout.CENTER);

        applyBtn.addActionListener(ev -> {
            Employee emp = (Employee) empCombo.getSelectedItem();
            if (emp == null) return;
            String from = fromField.getText().trim();
            String to = toField.getText().trim();
            String daysS = daysField.getText().trim();
            String reason = reasonField.getText().trim();
            if (from.isEmpty() || to.isEmpty() || daysS.isEmpty() || reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int days;
            try { days = Integer.parseInt(daysS); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid days."); return; }

            LeaveApplication app = manager.applyLeave(emp.id, from, to, days, reason);
            if (app != null) {
                JOptionPane.showMessageDialog(this, "Application submitted (PENDING). AppID: " + app.appId);
                fromField.setText(""); toField.setText(""); daysField.setText(""); reasonField.setText("");
                refreshEmployeeView();
                refreshAdminTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to submit application.");
            }
        });

        refreshEmployeeView();
        return p;
    }

    private JPanel createAdminPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        adminModel = new DefaultTableModel(new String[]{"AppID","EmpID","EmpName","From","To","Days","Reason","Status"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        JTable adminTable = new JTable(adminModel);
        JScrollPane sp = new JScrollPane(adminTable);
        p.add(sp, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField appIdField = new JTextField(6);
        JButton approveBtn = new JButton("Approve");
        JButton rejectBtn = new JButton("Reject");
        JButton refreshBtn = new JButton("Refresh");

        controls.add(new JLabel("AppID:")); controls.add(appIdField);
        controls.add(approveBtn); controls.add(rejectBtn); controls.add(refreshBtn);
        p.add(controls, BorderLayout.SOUTH);

        approveBtn.addActionListener(e -> {
            String s = appIdField.getText().trim();
            if (s.isEmpty()) return;
            try {
                int id = Integer.parseInt(s);
                boolean ok = manager.approve(id);
                if (ok) JOptionPane.showMessageDialog(this, "Approved.");
                else JOptionPane.showMessageDialog(this, "Cannot approve (maybe insufficient balance or already processed).", "Info", JOptionPane.INFORMATION_MESSAGE);
                refreshEmployeeView(); refreshAdminTable();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid AppID."); }
        });

        rejectBtn.addActionListener(e -> {
            String s = appIdField.getText().trim();
            if (s.isEmpty()) return;
            try {
                int id = Integer.parseInt(s);
                boolean ok = manager.reject(id);
                if (ok) JOptionPane.showMessageDialog(this, "Rejected.");
                else JOptionPane.showMessageDialog(this, "Cannot reject (maybe already processed).", "Info", JOptionPane.INFORMATION_MESSAGE);
                refreshEmployeeView(); refreshAdminTable();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid AppID."); }
        });

        refreshAdminTable();
        return p;
    }

    private void refreshEmployeeView() {
        Employee emp = (Employee) empCombo.getSelectedItem();
        if (emp == null) return;
        balanceLabel.setText("Leave balance: " + emp.leaveBalance + " days");
        // refill history
        historyModel.setRowCount(0);
        for (LeaveApplication a : emp.history) {
            historyModel.addRow(new Object[]{a.appId, a.fromDate, a.toDate, a.days, a.reason, a.status});
        }
    }

    private void refreshAdminTable() {
        adminModel.setRowCount(0);
        for (LeaveApplication a : manager.applications) {
            Employee e = manager.employees.get(a.empId);
            String en = (e==null)? a.empId : e.name;
            adminModel.addRow(new Object[]{a.appId, a.empId, en, a.fromDate, a.toDate, a.days, a.reason, a.status});
        }
    }
}
