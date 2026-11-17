import java.util.*;

interface Dao<T, K> {
    void insert(T value);
    T findById(K id);
    List<T> findAll();
    void update(T value);
    void delete(K id);
}
class Student {
    int registerNo; String name; String email;
    Student(int id, String n, String e) { registerNo=id; name=n; email=e; }
    public String toString() { return registerNo + " " + name + " " + email; }
}
class StudentDao implements Dao<Student, Integer> {
    private final Map<Integer, Student> data = new LinkedHashMap<>();
    public void insert(Student s) { data.put(s.registerNo, s); }
    public Student findById(Integer id) { return data.get(id); }
    public List<Student> findAll() { return new ArrayList<>(data.values()); }
    public void update(Student s) { if (data.containsKey(s.registerNo)) data.put(s.registerNo, s); }
    public void delete(Integer id) { data.remove(id); }
}
public class DaoDemo {
    public static void main(String[] args) {
        StudentDao dao = new StudentDao();
        dao.insert(new Student(192311001, "Arun", "arun@example.com"));
        dao.insert(new Student(192311002, "Divya", "divya@example.com"));
        System.out.println("Find: " + dao.findById(192311001));
        System.out.println("All: " + dao.findAll());
    }
}
