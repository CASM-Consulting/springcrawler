package com.casm.acled.entities.event.eventtypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class EventType_v1 {

    public static final Map<String, Set<String>> TYPES = new ImmutableMap.Builder<String,Set<String>>()
            .put("BATTLE", ImmutableSet.of(
                "Government regains territory",
                "Non-state actor overtakes territory",
                "Armed clash"
            ))
            .put("EXPLOSION_REMOTE_VIOLENCE", ImmutableSet.of(
                "Chemical weapon",
                "Air/drone strike",
                "Suicide bomb",
                "Shelling/artillery/missile attack",
                "Remote explosive/landmine/IED",
                "Grenade"
            ))
            .put("RIOT", ImmutableSet.of(
                "Violent demonstration",
                "Mob violence"
            ))
            .put("PROTEST", ImmutableSet.of(
                "Excessive force against protesters",
                "Protest with intervention",
                "Peaceful protest"
            ))
            .put("VIOLENCE_AGAINST_CIVILIANS", ImmutableSet.of(
                "Sexual violence",
                "Attack",
                "Abduction/forced disappearance"
            ))
            .put("STRATEGIC_DEVELOPMENT", ImmutableSet.of(
                "Non-violent transfer of territory",
                "Agreement",
                "Headquarters or base established",
                "Disrupted weapons use",
                "Change to group/activity",
                "Looting/property destruction",
                "Arrests",
                "Other"
            ))
            .build();

    public static Map<String, String> SUBTYPE_TO_TYPE = ImmutableMap.<String, String>builder()
                .put("ARMED_CLASH", "BATTLE")
                .put("GOV_TER", "BATTLE")
                .put("NS_ACT_TER", "BATTLE")

                .put("CHEM", "VIOLENCE_EXP")
                .put("DRONE", "VIOLENCE_EXP")
                .put("SUICIDE", "VIOLENCE_EXP")
                .put("ARTILLERY", "VIOLENCE_EXP")
                .put("IED", "VIOLENCE_EXP")
                .put("GRENADE", "VIOLENCE_EXP")

                .put("SEXUAL_VIOLENCE", "VIOLENCE_CIV")
                .put("ATTACK", "VIOLENCE_CIV")
                .put("ABDUCTION", "VIOLENCE_CIV")

                .put("PEACEFUL", "PROTEST")
                .put("INTERVENTION", "PROTEST")
                .put("FORCE", "PROTEST")

                .put("VIOLENT", "RIOT")
                .put("MOB", "RIOT")

                .put("AGREEMENT", "STRATEGIC")
                .put("ARREST", "STRATEGIC")
                .put("GRP_CHANGE", "STRATEGIC")
                .put("WEAPON_DISRUPT", "STRATEGIC")
                .put("HQ_BASE", "STRATEGIC")
                .put("LOOTING", "STRATEGIC")
                .put("NON_VIOLENT", "STRATEGIC")
                .put("OTHER", "STRATEGIC")
            .build();

    public static String typeFromSubType(String subType){
        return SUBTYPE_TO_TYPE.get(subType);
    }

    public static String randomType() {
        return randomElement(TYPES.keySet());
    }

    public static String randomSubType(){
        return randomElement(SUBTYPE_TO_TYPE.keySet());
    }

    public static String randomSubType(String type) {
        return randomElement(TYPES.get(type));
    }

    public static String randomElement(Set<String> set) {
        int i = new Random().nextInt(set.size());
        int j = 0;
        String out;
        Iterator<String> itr = set.iterator();
        while(j < i) {
            itr.next();
            ++j;
        }

        out = itr.next();

        return out;
    }

}

