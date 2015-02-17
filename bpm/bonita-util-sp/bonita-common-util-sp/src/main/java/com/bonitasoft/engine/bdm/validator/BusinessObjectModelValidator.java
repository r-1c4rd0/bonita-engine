/*******************************************************************************
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.bdm.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bonitasoft.engine.bdm.model.BusinessObject;
import com.bonitasoft.engine.bdm.model.BusinessObjectModel;
import com.bonitasoft.engine.bdm.model.Index;
import com.bonitasoft.engine.bdm.model.Query;
import com.bonitasoft.engine.bdm.model.QueryParameter;
import com.bonitasoft.engine.bdm.model.UniqueConstraint;
import com.bonitasoft.engine.bdm.model.field.Field;
import com.bonitasoft.engine.bdm.validator.rule.BusinessObjectModelValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.BusinessObjectValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.FieldValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.IndexValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.QueryParameterValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.QueryValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.SimpleFieldValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.UniqueConstraintValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.ValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.composition.CyclicCompositionValidationRule;
import com.bonitasoft.engine.bdm.validator.rule.composition.UniquenessCompositionValidationRule;

/**
 * @author Romain Bioteau
 */
@Deprecated
public class BusinessObjectModelValidator {

    private final List<ValidationRule<?>> rules = new ArrayList<ValidationRule<?>>();

    public BusinessObjectModelValidator() {
        rules.add(new BusinessObjectModelValidationRule());
        rules.add(new BusinessObjectValidationRule());
        rules.add(new FieldValidationRule());
        rules.add(new SimpleFieldValidationRule());
        rules.add(new UniqueConstraintValidationRule());
        rules.add(new IndexValidationRule());
        rules.add(new QueryValidationRule());
        rules.add(new QueryParameterValidationRule());
        rules.add(new UniquenessCompositionValidationRule());
        rules.add(new CyclicCompositionValidationRule());
    }

    public ValidationStatus validate(final BusinessObjectModel bom) {
        final Set<Object> objectsToValidate = buildModelTree(bom);
        final ValidationStatus status = new ValidationStatus();
        for (final Object modelElement : objectsToValidate) {
            for (final ValidationRule<?> rule : rules) {
                if (rule.appliesTo(modelElement)) {
                    status.addValidationStatus(rule.checkRule(modelElement));
                }
            }
        }
        return status;
    }

    private Set<Object> buildModelTree(final BusinessObjectModel bom) {
        final Set<Object> objectsToValidate = new HashSet<Object>();
        objectsToValidate.add(bom);
        for (final BusinessObject bo : bom.getBusinessObjects()) {
            objectsToValidate.add(bo);
            for (final Field f : bo.getFields()) {
                objectsToValidate.add(f);
            }
            final List<UniqueConstraint> uniqueConstraints = bo.getUniqueConstraints();
            for (final UniqueConstraint uc : uniqueConstraints) {
                objectsToValidate.add(uc);
            }
            final List<Query> queries = bo.getQueries();
            for (final Query q : queries) {
                objectsToValidate.add(q);
                for (final QueryParameter p : q.getQueryParameters()) {
                    objectsToValidate.add(p);
                }
            }
            for (final Index index : bo.getIndexes()) {
                objectsToValidate.add(index);
            }
        }
        return objectsToValidate;
    }

    public List<ValidationRule<?>> getRules() {
        return Collections.unmodifiableList(rules);
    }

}
