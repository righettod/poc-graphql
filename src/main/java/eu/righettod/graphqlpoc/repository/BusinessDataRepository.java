package eu.righettod.graphqlpoc.repository;

import eu.righettod.graphqlpoc.types.Dog;
import eu.righettod.graphqlpoc.types.Veterinary;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the CRUD actions on business data at storage level.
 */
@Component
public class BusinessDataRepository {

    private Connection storageConnection;

    /**
     * Constructor in charge of made the DB connection available
     *
     * @throws Exception If DB connection cannot be opened
     */
    public BusinessDataRepository() throws Exception {
        //Load the SQLite driver
        Class.forName("org.sqlite.JDBC");
        File dbLocation = new File("target/poc.db");
        File dbSQLLocation = new File("target/db.sql");
        if(!dbLocation.getParentFile().exists()){
            dbLocation.getParentFile().mkdirs();
        }
        String url = "jdbc:sqlite:" + dbLocation.getAbsolutePath().replace('\\', '/');
        //Open the connection
        this.storageConnection = DriverManager.getConnection(url);
        //Init the DB because the driver do not allow execution on script at connection time
        InputStream is = this.getClass().getResourceAsStream("/db.sql");
        Files.copy(is,dbSQLLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
        List<String> instructions = Files.readAllLines(dbSQLLocation.toPath());
        instructions.forEach(sql -> {
            try {
                if (sql.trim().length() > 0) {
                    try (Statement stmt = this.storageConnection.createStatement()) {
                        stmt.execute(sql);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Get all dogs based on filtering options passed
     *
     * @param namePrefix Prefix applied on the name of the dog
     * @param limit      Max number of record wanted
     * @return List of DogResolver object
     * @throws Exception If DogResolver data cannot be retrieved
     */
    public List<Dog> findByNamePrefix(String namePrefix, int limit) throws Exception {
        List<Dog> dogs = null;
        String sqlDog = "SELECT ID, NAME, VETERINARY_ID FROM DOG WHERE NAME LIKE '" + namePrefix + "%' LIMIT ?";
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sqlDog)) {
            stmt.setInt(1, limit);
            try (ResultSet rst = stmt.executeQuery()) {
                while (rst.next()) {
                    if (dogs == null) {
                        dogs = new ArrayList<>();
                    }
                    dogs.add(new Dog(rst.getInt("ID"), rst.getString("NAME"), this.findVeterinaryById(rst.getInt("VETERINARY_ID"))));
                }
            }
        }
        return dogs;
    }

    /**
     * Get all dogs based on filtering options passed
     *
     * @param onlyFree Flag to indicate that we only want the dogs not associated to a Veterinary
     * @param limit    Max number of record wanted
     * @return List of DogResolver object
     * @throws Exception If DogResolver data cannot be retrieved
     */
    public List<Dog> findAllDogs(boolean onlyFree, int limit) throws Exception {
        List<Dog> dogs = null;
        String sqlDogAll = "SELECT ID, NAME, VETERINARY_ID FROM DOG LIMIT ?";
        String sqlDogOnlyFree = "SELECT ID, NAME FROM DOG WHERE VETERINARY_ID IS NULL LIMIT ?";
        String sqlDog = onlyFree ? sqlDogOnlyFree : sqlDogAll;
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sqlDog)) {
            stmt.setInt(1, limit);
            try (ResultSet rst = stmt.executeQuery()) {
                while (rst.next()) {
                    if (dogs == null) {
                        dogs = new ArrayList<>();
                    }
                    if (onlyFree) {
                        dogs.add(new Dog(rst.getInt("ID"), rst.getString("NAME"), null));
                    } else {
                        dogs.add(new Dog(rst.getInt("ID"), rst.getString("NAME"), this.findVeterinaryById(rst.getInt("VETERINARY_ID"))));
                    }
                }
            }
        }

        return dogs;
    }

    /**
     * Get Veterinary info base on his ID.
     *
     * @param veterinaryId Veterinary ID
     * @return Veterinary object
     * @throws Exception If Veterinary cannot be found
     */
    public Veterinary findVeterinaryById(int veterinaryId) throws Exception {
        Veterinary v = null;
        String sqlVet = "SELECT ID, NAME, POPULARITY FROM VETERINARY WHERE ID=?";
        String sqlDog = "SELECT ID, NAME FROM DOG WHERE VETERINARY_ID=?";
        //Find Vet info
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sqlVet)) {
            stmt.setInt(1, veterinaryId);
            try (ResultSet rst = stmt.executeQuery()) {
                while (rst.next()) {
                    List<Dog> dogs = null;
                    String vetName = rst.getString("NAME");
                    int vetPopularity = rst.getInt("POPULARITY");
                    //Find Vet's dog list
                    try (PreparedStatement stmt2 = this.storageConnection.prepareStatement(sqlDog)) {
                        stmt2.setInt(1, veterinaryId);
                        try (ResultSet rst2 = stmt2.executeQuery()) {
                            while (rst2.next()) {
                                if (dogs == null) {
                                    dogs = new ArrayList<>();
                                }
                                dogs.add(new Dog(rst2.getInt("ID"), rst2.getString("NAME"), null));
                            }
                        }
                    }
                    v = new Veterinary(veterinaryId, vetName, vetPopularity, dogs);
                }
            }

        }
        //Affect veto for each dog
        if (v != null && v.getDogs() != null) {
            List<Dog> l = v.getDogs();
            for (Dog d : l) {
                d.setVeterinary(v);
            }
        }

        return v;
    }

    /**
     * Get DogResolver info base on his ID
     *
     * @param dogId DogResolver ID
     * @return DogResolver object
     * @throws Exception If DogResolver cannot be found
     */
    public Dog findDogById(int dogId) throws Exception {
        Dog d = null;
        String sql = "SELECT ID, NAME, VETERINARY_ID FROM DOG WHERE ID = ?";
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sql)) {
            stmt.setInt(1, dogId);
            try (ResultSet rst = stmt.executeQuery()) {
                while (rst.next()) {
                    d = new Dog(rst.getInt("ID"), rst.getString("NAME"), this.findVeterinaryById(rst.getInt("VETERINARY_ID")));
                }
            }
        }
        return d;
    }

    /**
     * Associated a dog to a veterinary if the dog is free
     *
     * @param veterinaryId Veterinary ID
     * @param dogId        DogResolver ID
     * @return a DogResolver object updated if the association has been made, NULL otherwise.
     * @throws Exception If an error occur during the association
     */
    public Dog associatedDogToVeterinary(int veterinaryId, int dogId) throws Exception {
        Dog d = null;
        //Verify is the specified DogResolver is already associated
        String sql = "SELECT COUNT(ID) FROM DOG WHERE VETERINARY_ID IS NOT NULL and ID = ?";
        boolean isAssociated;
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sql)) {
            stmt.setInt(1, dogId);
            try (ResultSet rst = stmt.executeQuery()) {
                isAssociated = rst.getInt(1) > 0;
            }
        }
        //Associated it if it's free
        if (!isAssociated) {
            sql = "UPDATE DOG SET VETERINARY_ID = ? WHERE ID = ?";
            try (PreparedStatement stmt = this.storageConnection.prepareStatement(sql)) {
                stmt.setInt(1, veterinaryId);
                stmt.setInt(2, dogId);
                int count = stmt.executeUpdate();
                if (count != 1) {
                    throw new Exception("No data updated!");
                }
            }
            d = findDogById(dogId);
        }

        return d;
    }

    /**
     * Disassociated a dog from a veterinary if the dog is associated to the veterinary specified
     *
     * @param veterinaryId Veterinary ID
     * @param dogId        DogResolver ID
     * @return a DogResolver object updated if the association has been made, NULL otherwise.
     * @throws Exception If an error occur during the association
     */
    public Dog disassociatedDogFromVeterinary(int veterinaryId, int dogId) throws Exception {
        Dog d = null;
        //Verify is the specified DogResolver is already associated to the specified veterinary
        String sql = "SELECT COUNT(ID) FROM DOG WHERE VETERINARY_ID = ? and ID = ?";
        boolean isAssociated;
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sql)) {
            stmt.setInt(1, veterinaryId);
            stmt.setInt(2, dogId);
            try (ResultSet rst = stmt.executeQuery()) {
                isAssociated = rst.getInt(1) > 0;
            }
        }
        //Disassociated it if it's associated
        if (!isAssociated) {
            sql = "UPDATE DOG SET VETERINARY_ID = NULL WHERE VETERINARY_ID = ? AND ID = ?";
            try (PreparedStatement stmt = this.storageConnection.prepareStatement(sql)) {
                stmt.setInt(1, veterinaryId);
                stmt.setInt(2, dogId);
                int count = stmt.executeUpdate();
                if (count != 1) {
                    throw new Exception("No data updated!");
                }
            }
            d = findDogById(dogId);
        }

        return d;
    }

    /**
     * Load configuration parameter
     *
     * @param key Identification key of the parameter
     * @return The config value
     * @throws Exception If any error occurs
     */
    public String loadCfgParam(String key) throws Exception {
        String v = null;
        String sql = "SELECT CFGVALUE FROM CONFIG WHERE CFGKEY = ?";
        try (PreparedStatement stmt = this.storageConnection.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rst = stmt.executeQuery()) {
                while (rst.next()) {
                    v = rst.getString("CFGVALUE");
                }
            }
        }
        return v;
    }


}
