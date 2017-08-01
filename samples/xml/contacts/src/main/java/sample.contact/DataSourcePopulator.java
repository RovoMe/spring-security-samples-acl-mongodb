/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.contact;

import java.util.Random;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.dao.AclRepository;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.MongoAcl;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * Populates the Contacts in-memory database with contact and ACL information.
 *
 * @author Ben Alex
 */
public class DataSourcePopulator implements InitializingBean {
	// ~ Instance fields
	// ================================================================================================

	@Resource
	AclRepository aclRepository;
	JdbcTemplate template;
	private MutableAclService mutableAclService;
	final Random rnd = new Random();
	TransactionTemplate tt;
	final String[] firstNames = { "Bob", "Mary", "James", "Jane", "Kristy", "Kirsty",
			"Kate", "Jeni", "Angela", "Melanie", "Kent", "William", "Geoff", "Jeff",
			"Adrian", "Amanda", "Lisa", "Elizabeth", "Prue", "Richard", "Darin",
			"Phillip", "Michael", "Belinda", "Samantha", "Brian", "Greg", "Matthew" };
	final String[] lastNames = { "Smith", "Williams", "Jackson", "Rictor", "Nelson",
			"Fitzgerald", "McAlpine", "Sutherland", "Abbott", "Hall", "Edwards", "Gates",
			"Black", "Brown", "Gray", "Marwell", "Booch", "Johnson", "McTaggart",
			"Parklin", "Findlay", "Robinson", "Giugni", "Lang", "Chi", "Carmichael" };
	private int createEntities = 50;

	// ~ Methods
	// ========================================================================================================

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(mutableAclService, "mutableAclService required");
		Assert.notNull(template, "dataSource required");
		Assert.notNull(aclRepository, "aclRepository required");
		Assert.notNull(tt, "platformTransactionManager required");

		// Gradle's compileJava step does not work with aclRepository.delete(aclRepository.findAll()); although delete
		// works on Iterable<? extends MongoAcl> and findAll returns a List object, which is a collection and therefore
		// also an iterable.
		for (MongoAcl toDelete : aclRepository.findAll()) {
			aclRepository.delete(toDelete);
		}

		// Set a user account that will initially own all the created data
		Authentication authRequest = new UsernamePasswordAuthenticationToken("rod",
				"koala", AuthorityUtils.createAuthorityList("ROLE_IGNORED"));
		SecurityContextHolder.getContext().setAuthentication(authRequest);

		try {
			template.execute("DROP TABLE IF EXISTS CONTACTS");
			template.execute("DROP TABLE IF EXISTS AUTHORITIES");
			template.execute("DROP TABLE IF EXISTS USERS");
		}
		catch (Exception e) {
			System.out.println("Failed to drop tables: " + e.getMessage());
		}

		template.execute("CREATE TABLE USERS(USERNAME VARCHAR(50) NOT NULL PRIMARY KEY,PASSWORD VARCHAR(500) NOT NULL,ENABLED BOOLEAN NOT NULL);");
		template.execute("CREATE TABLE AUTHORITIES(USERNAME VARCHAR(50) NOT NULL,AUTHORITY VARCHAR(50) NOT NULL,CONSTRAINT FK_AUTHORITIES_USERS FOREIGN KEY(USERNAME) REFERENCES USERS(USERNAME));");
		template.execute("CREATE UNIQUE INDEX IX_AUTH_USERNAME ON AUTHORITIES(USERNAME,AUTHORITY);");

		template.execute("CREATE TABLE CONTACTS(ID BIGINT NOT NULL PRIMARY KEY, CONTACT_NAME VARCHAR(50) NOT NULL, EMAIL VARCHAR(50) NOT NULL)");

		/*
		 * Passwords encoded using MD5, NOT in Base64 format, with null as salt Encoded
		 * password for rod is "koala" Encoded password for dianne is "emu" Encoded
		 * password for scott is "wombat" Encoded password for peter is "opal" (but user
		 * is disabled) Encoded password for bill is "wombat" Encoded password for bob is
		 * "wombat" Encoded password for jane is "wombat"
		 */
		template.execute("INSERT INTO USERS VALUES('rod','$2a$10$75pBjapg4Nl8Pzd.3JRnUe7PDJmk9qBGwNEJDAlA3V.dEJxcDKn5O',TRUE);");
		template.execute("INSERT INTO USERS VALUES('dianne','$2a$04$bCMEyxrdF/7sgfUiUJ6Ose2vh9DAMaVBldS1Bw2fhi1jgutZrr9zm',TRUE);");
		template.execute("INSERT INTO USERS VALUES('scott','$2a$06$eChwvzAu3TSexnC3ynw4LOSw1qiEbtNItNeYv5uI40w1i3paoSfLu',TRUE);");
		template.execute("INSERT INTO USERS VALUES('peter','$2a$04$8.H8bCMROLF4CIgd7IpeQ.tcBXLP5w8iplO0n.kCIkISwrIgX28Ii',FALSE);");
		template.execute("INSERT INTO USERS VALUES('bill','$2a$04$8.H8bCMROLF4CIgd7IpeQ.3khQlPVNWbp8kzSQqidQHGFurim7P8O',TRUE);");
		template.execute("INSERT INTO USERS VALUES('bob','$2a$06$zMgxlMf01SfYNcdx7n4NpeFlAGU8apCETz/i2C7VlYWu6IcNyn4Ay',TRUE);");
		template.execute("INSERT INTO USERS VALUES('jane','$2a$05$ZrdS7yMhCZ1J.AAidXZhCOxdjD8LO/dhlv4FJzkXA6xh9gdEbBT/u',TRUE);");
		template.execute("INSERT INTO AUTHORITIES VALUES('rod','ROLE_USER');");
		template.execute("INSERT INTO AUTHORITIES VALUES('rod','ROLE_SUPERVISOR');");
		template.execute("INSERT INTO AUTHORITIES VALUES('dianne','ROLE_USER');");
		template.execute("INSERT INTO AUTHORITIES VALUES('scott','ROLE_USER');");
		template.execute("INSERT INTO AUTHORITIES VALUES('peter','ROLE_USER');");
		template.execute("INSERT INTO AUTHORITIES VALUES('bill','ROLE_USER');");
		template.execute("INSERT INTO AUTHORITIES VALUES('bob','ROLE_USER');");
		template.execute("INSERT INTO AUTHORITIES VALUES('jane','ROLE_USER');");

		template.execute("INSERT INTO contacts VALUES (1, 'John Smith', 'john@somewhere.com');");
		template.execute("INSERT INTO contacts VALUES (2, 'Michael Citizen', 'michael@xyz.com');");
		template.execute("INSERT INTO contacts VALUES (3, 'Joe Bloggs', 'joe@demo.com');");
		template.execute("INSERT INTO contacts VALUES (4, 'Karen Sutherland', 'karen@sutherland.com');");
		template.execute("INSERT INTO contacts VALUES (5, 'Mitchell Howard', 'mitchell@abcdef.com');");
		template.execute("INSERT INTO contacts VALUES (6, 'Rose Costas', 'rose@xyz.com');");
		template.execute("INSERT INTO contacts VALUES (7, 'Amanda Smith', 'amanda@abcdef.com');");
		template.execute("INSERT INTO contacts VALUES (8, 'Cindy Smith', 'cindy@smith.com');");
		template.execute("INSERT INTO contacts VALUES (9, 'Jonathan Citizen', 'jonathan@xyz.com');");

		for (int i = 10; i < createEntities; i++) {
			String[] person = selectPerson();
			template.execute("INSERT INTO contacts VALUES (" + i + ", '" + person[2]
					+ "', '" + person[0].toLowerCase() + "@" + person[1].toLowerCase()
					+ ".com');");
		}

		// Create a domain object reference as well as the ACL record
		for (int i = 1; i < createEntities; i++) {
			final ObjectIdentity objectIdentity = new ObjectIdentityImpl(Contact.class, i);
			mutableAclService.createAcl(objectIdentity);
		}

		// Now grant some permissions
		grantPermissions(1, "rod", BasePermission.ADMINISTRATION);
		grantPermissions(2, "rod", BasePermission.READ);
		grantPermissions(3, "rod", BasePermission.READ);
		grantPermissions(3, "rod", BasePermission.WRITE);
		grantPermissions(3, "rod", BasePermission.DELETE);
		grantPermissions(4, "rod", BasePermission.ADMINISTRATION);
		grantPermissions(4, "dianne", BasePermission.ADMINISTRATION);
		grantPermissions(4, "scott", BasePermission.READ);
		grantPermissions(5, "dianne", BasePermission.ADMINISTRATION);
		grantPermissions(5, "dianne", BasePermission.READ);
		grantPermissions(6, "dianne", BasePermission.READ);
		grantPermissions(6, "dianne", BasePermission.WRITE);
		grantPermissions(6, "dianne", BasePermission.DELETE);
		grantPermissions(6, "scott", BasePermission.READ);
		grantPermissions(7, "scott", BasePermission.ADMINISTRATION);
		grantPermissions(8, "dianne", BasePermission.ADMINISTRATION);
		grantPermissions(8, "dianne", BasePermission.READ);
		grantPermissions(8, "scott", BasePermission.READ);
		grantPermissions(9, "scott", BasePermission.ADMINISTRATION);
		grantPermissions(9, "scott", BasePermission.READ);
		grantPermissions(9, "scott", BasePermission.WRITE);
		grantPermissions(9, "scott", BasePermission.DELETE);

		// Now expressly change the owner of the first ten contacts
		// We have to do this last, because "rod" owns all of them (doing it sooner would
		// prevent ACL updates)
		// Note that ownership has no impact on permissions - they're separate (ownership
		// only allows ACl editing)
		changeOwner(5, "dianne");
		changeOwner(6, "dianne");
		changeOwner(7, "scott");
		changeOwner(8, "dianne");
		changeOwner(9, "scott");

		String[] users = { "bill", "bob", "jane" }; // don't want to mess around with
													// consistent sample data
		Permission[] permissions = { BasePermission.ADMINISTRATION, BasePermission.READ,
				BasePermission.DELETE };

		for (int i = 10; i < createEntities; i++) {
			String user = users[rnd.nextInt(users.length)];
			Permission permission = permissions[rnd.nextInt(permissions.length)];
			grantPermissions(i, user, permission);

			String user2 = users[rnd.nextInt(users.length)];
			Permission permission2 = permissions[rnd.nextInt(permissions.length)];
			grantPermissions(i, user2, permission2);
		}

		SecurityContextHolder.clearContext();
	}

	private void changeOwner(int contactNumber, String newOwnerUsername) {
		AclImpl acl = (AclImpl) mutableAclService.readAclById(new ObjectIdentityImpl(Contact.class, contactNumber));
		acl.setOwner(new PrincipalSid(newOwnerUsername));
		updateAclInTransaction(acl);
	}

	public int getCreateEntities() {
		return createEntities;
	}

	private void grantPermissions(int contactNumber, String recipientUsername, Permission permission) {
		AclImpl acl = (AclImpl) mutableAclService.readAclById(new ObjectIdentityImpl(Contact.class, contactNumber));
		acl.insertAce(acl.getEntries().size(), permission, new PrincipalSid(recipientUsername), true);
		updateAclInTransaction(acl);
	}

	private String[] selectPerson() {
		String firstName = firstNames[rnd.nextInt(firstNames.length)];
		String lastName = lastNames[rnd.nextInt(lastNames.length)];

		return new String[] { firstName, lastName, firstName + " " + lastName };
	}

	public void setCreateEntities(int createEntities) {
		this.createEntities = createEntities;
	}

	public void setAclRepository(AclRepository aclRepository) {
		this.aclRepository = aclRepository;
	}

	public void setDataSource(DataSource dataSource) {
		this.template = new JdbcTemplate(dataSource);
	}

	public void setMutableAclService(MutableAclService mutableAclService) {
		this.mutableAclService = mutableAclService;
	}

	public void setPlatformTransactionManager(
			PlatformTransactionManager platformTransactionManager) {
		this.tt = new TransactionTemplate(platformTransactionManager);
	}

	private void updateAclInTransaction(final MutableAcl acl) {
		// MongoDB does not support transactions and hence has no transaction manager defined
		mutableAclService.updateAcl(acl);
	}
}
