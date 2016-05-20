package it.ldsoftware.primavera.test;

import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.types.Predicate;
import it.ldsoftware.primavera.entities.people.Contact;
import it.ldsoftware.primavera.entities.people.Person;
import it.ldsoftware.primavera.entities.people.QContact;
import it.ldsoftware.primavera.entities.people.QPerson;
import it.ldsoftware.primavera.entities.security.Group;
import it.ldsoftware.primavera.entities.security.GroupTranslation;
import it.ldsoftware.primavera.query.Filter;
import it.ldsoftware.primavera.query.PredicateFactory;
import it.ldsoftware.primavera.services.interfaces.DatabaseService;
import it.ldsoftware.primavera.util.ContactType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static it.ldsoftware.primavera.query.FilterOperator.AND;
import static it.ldsoftware.primavera.util.ContactType.EMAIL;
import static it.ldsoftware.primavera.util.ContactType.PHONE;
import static java.util.Collections.singletonList;

/**
 * Created by luca on 21/04/16.
 * Class that tests database creation.
 * Manual inspection of the database is suggested for optimal results.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class DatabaseTest {

    @Autowired
    DatabaseService svc;

    @Test
    public void contextLoads() {

    }

    private static final String CAPTION_1 = "Caption 1", CAPTION_2 = "Caption 2";

    @Test
    public void languageTest() {
        Group g = new Group();
        g.addTranslation("it", (GroupTranslation) new GroupTranslation().withContent(CAPTION_1).withLang("it"));
        g.addTranslation("en", (GroupTranslation) new GroupTranslation().withContent(CAPTION_2).withLang("en"));
        svc.save(Group.class, g);

        Group up = svc.findOne(Group.class, 1);
        assert up.getTranslations().size() == 2;
        assert up.getTranslation("it").getDescription().equals(CAPTION_1);
        assert up.getTranslation("en").getDescription().equals(CAPTION_2);
    }

    @Test
    public void findPredicate() throws Exception {
        String cVal = "123456";

        Person person = new Person();
        person.addContact(new Contact().withContactType(PHONE).withValue("not cVal"));
        person.addContact(new Contact().withContactType(ContactType.EMAIL).withValue(cVal));
        person.setName("Luca");
        person.setSurname("Di Stefano");
        person.setFullName("Luca Di Stefano");

        svc.save(Person.class, person);

        QPerson qp = QPerson.person;
        QContact qc = QContact.contact;

        Predicate predicate1 = qp.isNotNull()
                .and(qp.contacts.any().in(
                        new JPASubQuery().from(qc)
                                .where(qc.contactType.eq(PHONE)
                                        .and(qc.contactValue.eq(cVal))).list(qc)));

        List<Person> manual = svc.findAll(Person.class, predicate1);

        Contact noResults = new Contact().withContactType(PHONE).withValue(cVal);
        Predicate predicate2 = PredicateFactory.createPredicate(Person.class, singletonList(new Filter("contacts", noResults, false, AND)));

        List<Person> auto = svc.findAll(Person.class, predicate2);

        Assert.assertEquals(manual.size(), auto.size());

        Contact results = new Contact().withContactType(EMAIL).withValue(cVal);
        auto = svc.findAll(Person.class, PredicateFactory.createPredicate(Person.class, singletonList(new Filter("contacts", results, false, AND))));

        Assert.assertEquals(1, auto.size());
    }

    public void testExample() throws Exception {
        Person p = new Person();
        PredicateFactory.getFiltersByEntity(Person.class, p);
    }

}
