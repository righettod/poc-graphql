package eu.righettod.graphqlpoc.types;

import java.util.List;

/**
 * Hold the data of a Veterinary.
 */
public class Veterinary {

    //[VULN]: There an IDOR issue here because the identifier are simple sequential integer
    private int id;

    private String name;
    private List<Dog> dogs;

    //[VULN]: There an sensitive access issue here because the data mapping expose the "popularity" storage field and this one must not be seen by GraphQL client
    private int popularity;


    public Veterinary(int id, String name, int popularity, List<Dog> dogs) {
        this.id = id;
        this.name = name;
        this.popularity = popularity;
        this.dogs = dogs;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPopularity() {
        return popularity;
    }

    public List<Dog> getDogs() {
        return dogs;
    }

    @Override
    public String toString() {
        return "Veterinary{" +
                       "id=" + id +
                       ", name='" + name + '\'' +
                       ", dogs=" + dogs +
                       ", popularity=" + popularity +
                       '}';
    }
}
