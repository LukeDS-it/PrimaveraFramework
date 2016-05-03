package it.ldsoftware.commons.vaadin.editors.security;

import it.ldsoftware.commons.entities.security.Role;
import it.ldsoftware.commons.entities.security.RoleTranslation;
import it.ldsoftware.commons.vaadin.layouts.AbstractLookupForm;

/**
 * Created by luca on 03/05/16.
 * Form to create roles
 */
public class RoleForm extends AbstractLookupForm<Role, RoleTranslation> {
    @Override
    public RoleTranslation createTranslation(String text, String lang) {
        return (RoleTranslation) new RoleTranslation().withContent(text).withLang(lang);
    }
}
