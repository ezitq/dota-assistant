package com.itomagoi.dotaassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerProfile {

    private Profile profile;

    @JsonProperty("rank_tier")
    private Integer rankTier;

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public Integer getRankTier() { return rankTier; }
    public void setRankTier(Integer rankTier) { this.rankTier = rankTier; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        @JsonProperty("personaname")
        private String personaName;

        @JsonProperty("avatarfull")
        private String avatarFull;

        public String getPersonaName() { return personaName; }
        public void setPersonaName(String personaName) { this.personaName = personaName; }

        public String getAvatarFull() { return avatarFull; }
        public void setAvatarFull(String avatarFull) { this.avatarFull = avatarFull; }
    }
}