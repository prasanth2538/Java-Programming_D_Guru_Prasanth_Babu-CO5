import java.io.*;
import java.util.*;

class Resident implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String registerNo;
    transient String password; // never serialize a plaintext password
    transient Object liveConnection; // stand-in for a live JDBC Connection
    String phone; // added later; old compatible streams give null
    Resident(String n, String r, String p, String ph) {
        name=n; registerNo=r; password=p; phone=ph;
    }
}
class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    int roomNo;
    List<Resident> residents = new ArrayList<>();
    Room(int no) { roomNo=no; }
}
class Hostel implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Room> rooms = new ArrayList<>();
}
public class SerializationDemo {
    public static void main(String[] args) throws Exception {
        Hostel h = new Hostel();
        Room room = new Room(101);
        room.residents.add(new Resident("Arun", "192311001", "secret", "9876543210"));
        h.rooms.add(room);
        File f = new File("hostel.dat");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(h);
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Hostel loaded = (Hostel) in.readObject();
            Resident r = loaded.rooms.get(0).residents.get(0);
            System.out.println("Resident: " + r.name + ", " + r.registerNo);
            System.out.println("Password after reading: " + r.password);
            System.out.println("Phone: " + r.phone);
        }
        f.delete();
    }
}
