package io.rollout.publicapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

/**
 * Sparsely defined model for the audit log API. We only use fields here we really care about.
 * eg data:
 * <pre>
   {
     "_id": "123456",
     "application": "1234567890",
     "experiment": "abcbef",
     "user": {
       "_id": "999876",
       "email": "user@email.com",
       "name": "Mr Jenkins",
       "picture": "https://secure.gravatar.com/avatar/jenkins"
     },
     "userName": "Mr Jenkins",
     "userEmail": "user@email.com",
     "action": "turned targeting on",
     "message": "Flag 'another flag' set to targeting on",
     "environmentId": "111111$",
     "creation_date": "2022-01-14T14:02:27.119Z",
     "__v": 0
   }
 * </pre>
 */
public class AuditLog {
    private String userName;
    private String userEmail;
    private String action;
    private String message;
    @JsonProperty("creation_date")
    private Date creationDate;

    public User getUser() {
        return user;
    }

    private User user;

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", action='" + action + '\'' +
                ", message='" + message + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }

    public static class User {
        private String name;
        private String email;
        private String picture;

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPicture() {
            return picture;
        }
    }
}
