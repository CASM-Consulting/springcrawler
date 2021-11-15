package com.casm.acled.entities.validation;

public final class FrontendValidators {

    public static final class Field {

        public static final String URL = "url";
        public static final String CHECK_ARTICLE_SOURCE_DOMAIN = "checkArticleSourceDomain";
        public static final String REQUIRED = "required";

        // Only actors with INTER fields of specific values can be associated with event type BATTLE
        public static final String ACTOR_BATTLE = "actorBattle";
        // Only associate actors with INTER fields of specific values can be associated with event type BATTLE
        public static final String ASSOC_ACTOR_BATTLE = "assocActorBattle";

        public static final String LAT_LON = "latLon";

        // Issue warning if field is less than 20 characters
        public static final String MIN_CHARS_20 = "minChars20";

        public static final String FATALITY = "fatality";

        // Issue warning if an INTER 1 actor is present in an event with a non-matching country
        public static final String FOREIGN_ACTOR = "foreignActor";

        // Flag as warning that field is empty
        public static final String EMPTY = "empty";

        // Flag as warning that certain embedded location fields are empty
        public static final String EMPTY_LOCATION_FIELDS = "emptyLocationFields";
    }
}
