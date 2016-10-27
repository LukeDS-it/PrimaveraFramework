package it.ldsoftware.primavera.entities.security;

import it.ldsoftware.primavera.entities.base.LookupTranslation;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by luca on 12/04/16.
 * Translation of the group
 */
@Entity
@Table(name = "fw_group_translation")
public class GroupTranslation extends LookupTranslation<Group> {
}
