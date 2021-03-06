package it.ldsoftware.primavera.vaadin.editors.people;

import com.vaadin.ui.Button;
import it.ldsoftware.primavera.presentation.people.PersonDTO;
import it.ldsoftware.primavera.model.people.Person;
import it.ldsoftware.primavera.i18n.LocalizationService;
import it.ldsoftware.primavera.services.interfaces.DatabaseService;
import it.ldsoftware.primavera.vaadin.layouts.AbstractDetailTab;
import it.ldsoftware.primavera.vaadin.util.PeopleAdder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luca on 02/05/16.
 * <p>
 * This tab is useful when an object has as a detail information about people.
 */
public class PeopleTab extends AbstractDetailTab<Person, PersonDTO> {

    private PeopleAdder peopleAdder;

    public PeopleTab(LocalizationService localizationService, DatabaseService databaseService, String labelCaption, PeopleAdder father) {
        super(localizationService, databaseService, labelCaption);
        this.peopleAdder = father;
        withBrowseButton();
    }

    @Override
    public void addToMaster(Button.ClickEvent e) {
        // Not used because this will be a "browse" style tab
    }

    @Override
    public void addListToMaster(List<PersonDTO> personDTOs) {
        peopleAdder.addPeople(personDTOs);
    }

    @Override
    public void removeFromMaster(List<PersonDTO> personDTOs) {
        peopleAdder.remPeople(personDTOs);
    }

    @Override
    public Class<Person> getEntityClass() {
        return Person.class;
    }

    @Override
    public Class<PersonDTO> getDTOClass() {
        return PersonDTO.class;
    }

    @Override
    public List<Person> getDetailChoice() {
        return new ArrayList<>(); // Not used because it is a "browse" style tab
    }

    @Override
    public String getCaption(Object o) {
        return ""; // Not used because it is a "browse" style tab
    }
}
