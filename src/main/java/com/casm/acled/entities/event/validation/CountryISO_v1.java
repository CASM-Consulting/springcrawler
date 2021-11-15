package com.casm.acled.entities.event.validation;//package com.casm.acled.entities.event.validation;
//
//import com.google.common.collect.ImmutableSet;
////import EntityFieldValidationException;
//import com.casm.acled.entities.validation.EntityFieldValidationException;
//import com.casm.acled.entities.validation.EntityFieldValidator;
//
//import java.util.Arrays;
//import java.util.Locale;
//
//public class CountryISO_v1 extends EntityFieldValidator<String> {
//
//    public static final ImmutableSet<String> ISOs;
//    static {
//        ISOs = ImmutableSet.<String>builder()
//                .addAll(Arrays.asList(Locale.getISOCountries()))
//                .build();
//    }
//
//
//    @Override
//    public void validate(String val) {
//        if(!ISOs.contains(val)) {
//            throw new EntityFieldValidationException(val + " not an accepted ISO code.");
//        }
//    }
//}
