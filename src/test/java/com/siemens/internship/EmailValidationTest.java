package com.siemens.internship;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class EmailValidationTest {

    private final Validator emailValidator= Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testInvalidEmail() {
        Item item=new Item(1L,"test","desc","NEW","invalid");

        var violations = emailValidator.validate(item);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v->v.getPropertyPath().toString().equals("email")));

        item=new Item(2L,"test","desc","NEW","invalid@");
        violations = emailValidator.validate(item);
        assertFalse(violations.isEmpty());

        item=new Item(3L,"test","desc","NEW","invalid@e.");
        violations = emailValidator.validate(item);
        assertFalse(violations.isEmpty());

        item=new Item(4L,"test","desc","NEW","invalid@.a");
        violations = emailValidator.validate(item);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidEmail() {
        Item item=new Item(1L,"test","desc","NEW","valid@a.com");
        var violations = emailValidator.validate(item);
        assertTrue(violations.isEmpty());

        item=new Item(2L,"test","desc","NEW","");
        violations = emailValidator.validate(item);
        assertTrue(violations.isEmpty());
    }
}
