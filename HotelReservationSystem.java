import java.util.*;

class Room {
    int roomNumber;
    String type; // e.g., Single, Double, Suite
    boolean isAvailable;

    Room(int roomNumber, String type) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.isAvailable = true;
    }
}

class Reservation {
    String customerName;
    Room room;
    Date checkIn;
    Date checkOut;

    Reservation(String customerName, Room room, Date checkIn, Date checkOut) {
        this.customerName = customerName;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }
}

class Hotel {
    List<Room> rooms = new ArrayList<>();
    List<Reservation> reservations = new ArrayList<>();

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public Room findAvailableRoom(String type) {
        for (Room room : rooms) {
            if (room.type.equalsIgnoreCase(type) && room.isAvailable) {
                return room;
            }
        }
        return null;
    }

    public boolean reserveRoom(String customerName, String type, Date checkIn, Date checkOut) {
        Room room = findAvailableRoom(type);
        if (room != null) {
            room.isAvailable = false;
            reservations.add(new Reservation(customerName, room, checkIn, checkOut));
            System.out.println("Room " + room.roomNumber + " reserved for " + customerName);
            return true;
        } else {
            System.out.println("No available room of type " + type);
            return false;
        }
    }

    public void showReservations() {
        for (Reservation r : reservations) {
            System.out.println("Customer: " + r.customerName + ", Room: " + r.room.roomNumber + ", Type: " + r.room.type +
                    ", Check-in: " + r.checkIn + ", Check-out: " + r.checkOut);
        }
    }
}

public class HotelReservationSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Hotel hotel = new Hotel();
        // Add sample rooms
        hotel.addRoom(new Room(101, "Single"));
        hotel.addRoom(new Room(102, "Double"));
        hotel.addRoom(new Room(103, "Suite"));

        while (true) {
            System.out.println("\nHotel Reservation System");
            System.out.println("1. Reserve Room");
            System.out.println("2. Show Reservations");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            if (choice == 1) {
                System.out.print("Enter customer name: ");
                String name = scanner.nextLine();
                System.out.print("Enter room type (Single/Double/Suite): ");
                String type = scanner.nextLine();
                System.out.print("Enter check-in date (yyyy-mm-dd): ");
                String checkInStr = scanner.nextLine();
                System.out.print("Enter check-out date (yyyy-mm-dd): ");
                String checkOutStr = scanner.nextLine();
                try {
                    Date checkIn = java.sql.Date.valueOf(checkInStr);
                    Date checkOut = java.sql.Date.valueOf(checkOutStr);
                    hotel.reserveRoom(name, type, checkIn, checkOut);
                } catch (Exception e) {
                    System.out.println("Invalid date format.");
                }
            } else if (choice == 2) {
                hotel.showReservations();
            } else if (choice == 3) {
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }
}