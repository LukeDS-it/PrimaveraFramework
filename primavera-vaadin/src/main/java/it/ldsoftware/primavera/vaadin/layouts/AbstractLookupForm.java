package it.ldsoftware.primavera.vaadin.layouts;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.ui.*;
import de.datenhahn.vaadin.componentrenderer.ComponentRenderer;
import it.ldsoftware.primavera.model.base.Lookup;
import it.ldsoftware.primavera.model.lang.ShortTranslation;
import it.ldsoftware.primavera.i18n.CommonLabels;
import it.ldsoftware.primavera.vaadin.util.SupportedLanguage;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import javax.validation.Validator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.vaadin.server.FontAwesome.TRASH_O;
import static com.vaadin.server.Sizeable.Unit.PERCENTAGE;
import static com.vaadin.ui.AbstractSelect.ItemCaptionMode.PROPERTY;
import static com.vaadin.ui.Alignment.MIDDLE_LEFT;
import static com.vaadin.ui.themes.ValoTheme.*;
import static it.ldsoftware.primavera.presentation.base.LookupDTO.FIELD_CODE;
import static it.ldsoftware.primavera.presentation.base.LookupDTO.FIELD_DESCRIPTION;
import static it.ldsoftware.primavera.presentation.lang.TranslatableDTO.FIELD_LANG;
import static it.ldsoftware.primavera.i18n.CommonErrors.MSG_INS_ERROR;
import static it.ldsoftware.primavera.i18n.CommonErrors.MSG_SAVE_ERROR;
import static it.ldsoftware.primavera.i18n.CommonLabels.*;
import static it.ldsoftware.primavera.i18n.LanguageUtils.getTextFieldName;
import static it.ldsoftware.primavera.vaadin.components.DTOGrid.COLUMN_DELETE;
import static it.ldsoftware.primavera.vaadin.i18n.CommonLabels.TAB_TRANSLATIONS;
import static it.ldsoftware.primavera.vaadin.theme.MetricConstants.COLUMN_XS;
import static it.ldsoftware.primavera.vaadin.theme.MetricConstants.FIELD_WIDTH;
import static it.ldsoftware.primavera.vaadin.util.NotificationBuilder.showNotification;
import static it.ldsoftware.primavera.vaadin.util.SupportedLanguage.supportedLanguages;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Created by luca on 20/04/16.
 * This class is the representation of a form for a lookup entity.
 * Can be used as-is for simple lookups or extended for more complex
 * lookups
 */
public abstract class AbstractLookupForm<L extends Lookup> extends TabbedForm<L> {
    private TextField code, ownLocale;
    private IndexedContainer container;

    public AbstractLookupForm(AbstractEditor parent) {
        super(parent);
        createTranslationsTab();
    }

    @Override
    public void addGeneralContent(VerticalLayout generalTab) {
        code = new MTextField(getTranslator().translate(getTextFieldName(FIELD_CODE))).withWidth(FIELD_WIDTH);
        code.addTextChangeListener(l -> signalChange());

        ownLocale = new MTextField(getTranslator().translate(getTextFieldName(FIELD_DESCRIPTION)))
                .withWidth(FIELD_WIDTH);

        ownLocale.addTextChangeListener(l -> signalChange());
        ownLocale.setRequired(true);
        ownLocale.addValidator(new NullValidator("", false));

        generalTab.addComponent(code);
        generalTab.addComponent(ownLocale);

        setStyles();
    }

    private void setStyles() {
        code.setStyleName(TEXTFIELD_TINY);
        ownLocale.setStyleName(TEXTFIELD_TINY);
    }

    private void createTranslationsTab() {
        VerticalLayout tTab = new MVerticalLayout().withFullWidth().withMargin(true);

        Grid tGrid = createGrid();

        tTab.addComponent(tGrid);

        Label label = new Label(getTranslator().translate(TXT_ADD_DESC));
        TextField translation = new MTextField().withWidth("250px");
        ComboBox language = new ComboBox();

        Collection<SupportedLanguage> langs = stream(supportedLanguages)
                .map(l -> l.translate(getTranslator()))
                .sorted((c1, c2) -> c1.getLangName().compareTo(c2.getLangName())).collect(toList());

        BeanItemContainer<SupportedLanguage> cont = new BeanItemContainer<>(SupportedLanguage.class, langs);
        language.setContainerDataSource(cont);

        language.setItemCaptionMode(PROPERTY);
        language.setItemCaptionPropertyId("langName");
        language.setItemIconPropertyId("icon");

        language.getItemIds().forEach(id -> language.setItemIcon(id, ((SupportedLanguage) id).getIcon()));

        CheckBox defaultLang = new CheckBox(getTranslator().translate(CommonLabels.DEFAULT));
        Button insert = new Button(getTranslator().translate(ADD));

        insert.addClickListener(l -> {
            String transl = translation.getValue();

            try {
                String lang = language.getValue().toString();
                getBean().addTranslation(lang, createTranslation(transl));
                if (defaultLang.getValue())
                    getBean().setDefaultLang(lang);
                signalChange();
                translation.focus();
                translation.setValue("");
                language.setValue(language.getNullSelectionItemId());
                getEditor().saveAction(null);
            } catch (IllegalArgumentException e) {
                showNotification(getTranslator().translate(TITLE_GENERIC_WARNING),
                        getTranslator().translate(e.getMessage()), NOTIFICATION_WARNING);
            } catch (NullPointerException e) {
                showNotification(getTranslator().translate(TITLE_SAVE_ERROR),
                        getTranslator().translate(MSG_SAVE_ERROR, getTranslator().translate(MSG_INS_ERROR)),
                        NOTIFICATION_ERROR);
            }
        });

        HorizontalLayout insertTranslation = new MHorizontalLayout()
                .with(label, translation, language, defaultLang, insert).withAlign(label, MIDDLE_LEFT)
                .withAlign(defaultLang, MIDDLE_LEFT);

        tTab.addComponent(insertTranslation);

        addTab(tTab, getTranslator().translate(TAB_TRANSLATIONS));
    }

    private Grid createGrid() {
        Grid tGrid = new Grid();
        tGrid.setWidth(100, PERCENTAGE);
        tGrid.setHeight("200px");

        container = new IndexedContainer();
        container.addContainerProperty(FIELD_LANG, String.class, null);
        container.addContainerProperty(FIELD_DESCRIPTION, String.class, null);

        GeneratedPropertyContainer gpc = new GeneratedPropertyContainer(container);

        gpc.addGeneratedProperty(COLUMN_DELETE, new PropertyValueGenerator<Component>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getValue(Item item, Object itemId, Object propertyId) {
                return new MButton(TRASH_O).withListener(l -> deleteTranslation(itemId))
                        .withStyleName(BUTTON_BORDERLESS);
            }

            @Override
            public Class<Component> getType() {
                return Component.class;
            }
        });

        tGrid.setContainerDataSource(gpc);
        tGrid.setColumnOrder(COLUMN_DELETE, FIELD_LANG, FIELD_DESCRIPTION);
        tGrid.getColumn(COLUMN_DELETE).setRenderer(new ComponentRenderer()).setWidth(COLUMN_XS).setHeaderCaption("");

        tGrid.getColumn(FIELD_DESCRIPTION).setHeaderCaption(getTranslator().translate(LABEL_DESCRIPTION));
        return tGrid;
    }

    @Override
    public void selectFirstField() {
        code.focus();
    }

    private void deleteTranslation(Object item) {
        getBean().removeTranslation(container.getItem(item)
                .getItemProperty(FIELD_LANG).getValue().toString());
        getEditor().saveAction(null);
    }

    @Override
    public L getBean() {
        Locale loc = UI.getCurrent().getLocale();

        L bean = super.getBean();

        ShortTranslation desc = bean.getTranslationForced(loc.getLanguage());
        if (desc == null) {
            desc = createTranslation(null);
            bean.addTranslation(loc.getLanguage(), desc);
        }

        desc.setDescription(ownLocale.getValue());
        bean.setDefaultLang(loc.getLanguage());

        return bean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setBean(L bean) {
        super.setBean(bean);
        Locale loc = UI.getCurrent().getLocale();

        ShortTranslation desc = bean.getTranslationForced(loc.getLanguage());
        if (desc == null) {
            desc = createTranslation(null);
            bean.addTranslation(loc.getLanguage(), desc);
        }

        ownLocale.setValue(desc.getDescription());

        container.removeAllItems();
        bean.getTranslations().forEach((k, v) -> {
            Item newItem = container.getItem(container.addItem());
            newItem.getItemProperty(FIELD_LANG).setValue(k);
            newItem.getItemProperty(FIELD_DESCRIPTION).setValue(v.getDescription());
        });
    }

    @Override
    public List<String> validate(Class<?>... validationGroups) {
        List<String> partial = super.validate(validationGroups);

        Validator v = getBeanValidator();

        getBean().getTranslations().values().stream()
                .flatMap((entry) -> v.validate(entry, validationGroups).stream())
                .map(cve -> getTranslator().getConstraintViolation(cve))
                .forEach(partial::add);

        return partial;
    }

    private ShortTranslation createTranslation(String text) {
        return new ShortTranslation().withContent(text);
    }
}
