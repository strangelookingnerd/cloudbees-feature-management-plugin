/*
 * The MIT License
 *
 * Copyright 2015-2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
    private User user = new User();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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
        public void setPicture(String picture) {
            this.picture = picture;
        }
    }
}
