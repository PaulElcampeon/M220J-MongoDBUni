package mflix.api.daos;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import javax.print.Doc;

@Configuration
public class UserDao extends AbstractMFlixDao {

  private final MongoCollection<User> usersCollection;
  //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
  //returns a Session object
  private final MongoCollection<Session> sessionsCollection;

  private final Logger log;

  @Autowired
  public UserDao(
      MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
    super(mongoClient, databaseName);
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
    log = LoggerFactory.getLogger(this.getClass());
    //TODO> Ticket: User Management - implement the necessary changes so that the sessions
    // collection returns a Session objects instead of Document objects.
    sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
  }

  /**
   * Inserts the `user` object in the `users` collection.
   *
   * @param user - User object to be added
   * @return True if successful, throw IncorrectDaoOperation otherwise
   */
  public boolean addUser(User user) throws MongoWriteException {
    //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!

    try {

      usersCollection.insertOne(user);

      return true;

    } catch (DuplicateKeyException e) {

      return false;
    }
    //TODO > Ticket: Handling Errors - make sure to only add new users
    // and not users that already exist.
  }

  /**
   * Creates session using userId and jwt token.
   *
   * @param userId - user string identifier
   * @param jwt - jwt string token
   * @return true if successful
   */
  public boolean createUserSession(String userId, String jwt) {
    //TODO> Ticket: User Management - implement the method that allows session information to be
    // stored in it's designated collection.
    Document queryDocument = new Document("jwt", jwt);

    long numberOfDocuments = sessionsCollection.countDocuments(queryDocument);

    if (numberOfDocuments == 0 ){

      Session createdSession = new Session();

      createdSession.setUserId(userId);

      createdSession.setJwt(jwt);

      sessionsCollection.insertOne(createdSession);

      return true;

    } else {

      return false;
    }
//    Document queryDocument = new Document("jwt", new Document("$exists", jwt));
//    sessionsCollection.find(queryDocument)

    //TODO > Ticket: Handling Errors - implement a safeguard against
    // creating a session with the same jwt token.
  }

  /**
   * Returns the User object matching the an email string value.
   *
   * @param email - email string to be matched.
   * @return User object or null.
   */
  public User getUser(String email) {
    User user = null;
    //TODO> Ticket: User Management - implement the query that returns the first User object.
    Document userQuery = new Document("email",email);
//    List<User> users = new ArrayList<>();
//    FindIterable<User> userDocuments = usersCollection.find(userQuery);
//    MongoCursor<User> userDocuments = usersCollection.find(userQuery).iterator();
    user = usersCollection.find(userQuery).limit(1).iterator().tryNext();
    return user;
  }

  /**
   * Given the userId, returns a Session object.
   *
   * @param userId - user string identifier.
   * @return Session object or null.
   */
  public Session getUserSession(String userId) {
    //TODO> Ticket: User Management - implement the method that returns Sessions for a given
    // userId
    Document filterQuery = new Document("user_id",userId);
    Session userSession = sessionsCollection
            .find(filterQuery)
            .limit(1)
            .iterator()
            .tryNext();
    return userSession;
  }

  public boolean deleteUserSessions(String userId) {
    //TODO> Ticket: User Management - implement the delete user sessions method
    Document queryDocument = new Document("userId", userId);
    DeleteResult result = sessionsCollection.deleteOne(queryDocument);
    return result.wasAcknowledged();
  }

  /**
   * Removes the user document that match the provided email.
   *
   * @param email - of the user to be deleted.
   * @return true if user successfully removed
   */
  public boolean deleteUser(String email) {
    // remove user sessions
    //TODO> Ticket: User Management - implement the delete user method
    //TODO > Ticket: Handling Errors - make this method more robust by
    Document deleteQueryUser = new Document("email", email);
    DeleteResult result = usersCollection.deleteOne(deleteQueryUser);

    Document deleteQuerySession = new Document("user_id", email);
    DeleteResult result1 = sessionsCollection.deleteOne(deleteQuerySession);
    // handling potential exceptions.
    return result.wasAcknowledged() && result1.wasAcknowledged();
  }

  /**
   * Updates the preferences of an user identified by `email` parameter.
   *
   * @param email - user to be updated email
   * @param userPreferences - set of preferences that should be stored and replace the existing
   *     ones. Cannot be set to null value
   * @return User object that just been updated.
   */
  public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
    //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
    // be updated.
    UpdateResult result;
    if(userPreferences!= null) {

      Document queryFilter = new Document("email", email);

      result = usersCollection.updateOne(queryFilter, set("preferences", userPreferences));

    } else {

      throw new IncorrectDaoOperation("User preferences cannot be null");
    }
    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions when updating an entry.
    return result.wasAcknowledged();
  }
}
