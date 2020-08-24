/* Gaurav Agrawal (ga1380) Final Project */

package finalproject.entities;

public class Person implements java.io.Serializable {

    private static final long serialVersionUID = 4190276780070819093L;

    private String first;
    private String last;
    private int age;
    private String city;
    private Integer id;
    // this is a person object that you will construct with data from the DB
    // table. The "sent" column is unnecessary. It's just a person with
    // a first name, last name, age, city, and ID.

    public Person(String first, String last, int age, String city, Integer id) {
        this.first = first;
        this.last = last;
        this.age = age;
        this.city = city;
        this.id = id;
    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public int getAge() {
        return age;
    }

    public String getCity() {
        return city;
    }

    public Integer getId() {
        return id;
    }

    public String toString() {
        return "Person[ last= " + this.last + ", first= " + this.first + ", age= " +
                this.age + ", city= " + this.city + ", id= " + this.id;
    }
}
