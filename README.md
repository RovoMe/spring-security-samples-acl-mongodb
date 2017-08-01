# Spring Security Contact Sample for MongoDB based ACL

This is a customized contact sample which originates from the [Spring Security](https://github.com/spring-projects/spring-security) project. This sample works on a MongoDB based ACL implementation which, in contrast to the original example, does persist and lookup ACL data from a single collection rather than 4 different tables as in the original ACL implementation.

Modifications on the original code where mainly on the introduction of an own `applicationContext-common-mongodb.xml` Spring configuration file and tweaking the `applicationContext-common-business.xml` configuration to inject the `AclRepository` into the modified `DataSourcePopulator` as ACL tables no longer are needed as this data will be persisted to a local running MongoDB.

## Run the sample

In order to run the sample, make sure you have check out [spring-security-acl-mongodb](https://github.com/RovoMe/spring-security-acl-mongodb) and installed its artifacts to your local Maven `.m2/repository` repository. Make sure a Gradle 3.5, MySQL 5.1.6+ and MongoDB 3.x are available on your system and both MySQL and MongoDB are up and running. MySQL should listen on port 3306 while MongoDB on port 27017.

Afterwards change to the `samples/xml/contacts` directory and enter `gradle tomcatStart` into your terminal. Soon an output similar to 

```
Started Tomcat Server
The Server is running at http://localhost:8080/sample
```

should be visible in your terminal. Now hit this URL in your browser and start playing around with the sample. After starting up the application, your MongoDB should contain an `ACL` collection with a couple of entries. The business logic relevant data should still be present in the MySQL database.