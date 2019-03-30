package eu.righettod.graphqlpoc.types;

/**
 * Hold the data of a DogResolver.
 */
public class Dog {

    private int id;
    private String name;
    private Veterinary veterinary;

    public Dog(int id, String name, Veterinary veterinary) {
        this.id = id;
        this.name = name;
        this.veterinary = veterinary;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Veterinary getVeterinary() {
        return veterinary;
    }

    public void setVeterinary(Veterinary veterinary) {
        this.veterinary = veterinary;
    }

    @Override
    public String toString() {
        return "DogResolver{" +
                       "id=" + id +
                       ", name='" + name + '\'' +
                       ", veterinary=" + veterinary +
                       '}';
    }
}
