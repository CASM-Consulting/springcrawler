package com.casm.acled.entities.location;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class Location extends VersionedEntity<Location> {

    public static final String ISO = "ISO";
    public static final String COUNTRY = "COUNTRY";
    public static final String ADMIN1 = "ADMIN1";
    public static final String ADMIN2 = "ADMIN2";
    public static final String ADMIN3 = "ADMIN3";
    public static final String LOCATION = "LOCATION";
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String GEO_PRECISION = "GEO_PRECISION";
    public static final String ADM1_CAPITAL = "ADM1_CAPITAL";
    public static final String ADM2_CAPITAL = "ADM2_CAPITAL";
    public static final String ADM3_CAPITAL = "ADM3_CAPITAL";
    public static final String ALIAS = "ALIAS";
    public static final String ALIAS_NON_ENGLISH = "ALIAS_NON_ENGLISH";
    public static final String NOTES = "NOTES";
    public static final String VERIFIED = "VERIFIED";

    public Location(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id) {
        super(entitySpec, version, data, id);
    }
}
