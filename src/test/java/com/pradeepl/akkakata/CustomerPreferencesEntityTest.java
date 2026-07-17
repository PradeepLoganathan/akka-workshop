package com.pradeepl.akkakata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import akka.javasdk.testkit.KeyValueEntityTestKit;

import com.pradeepl.akkakata.domain.commands.PreferencesCommands.SetPreferences;
import com.pradeepl.akkakata.domain.entities.CustomerPreferencesEntity;
import com.pradeepl.akkakata.domain.model.CustomerPreferences;

/**
 * Baseline test for CustomerPreferencesEntity (KVE) — Day 4 lab starting point.
 *
 * Uses {@link KeyValueEntityTestKit} to prove the "last write wins" semantics
 * of value entities without any events or replay.
 */
class CustomerPreferencesEntityTest {

    @Test
    void defaultsUntilSet() {
        var testKit = KeyValueEntityTestKit.of("c-1", CustomerPreferencesEntity::new);

        CustomerPreferences state = testKit.getState();

        // emptyState() returns CustomerPreferences.defaults()
        assertThat(state.theme()).isEqualTo("light");
        assertThat(state.locale()).isEqualTo("en-US");
        assertThat(state.marketingOptIn()).isFalse();
    }

    @Test
    void setOverwritesEntireState() {
        var testKit = KeyValueEntityTestKit.of("c-1", CustomerPreferencesEntity::new);

        var result = testKit.method(CustomerPreferencesEntity::set)
            .invoke(new SetPreferences("dark", "en-GB", true));

        assertThat(result.getReply()).isEqualTo("OK");
        assertThat(testKit.getState().theme()).isEqualTo("dark");
        assertThat(testKit.getState().locale()).isEqualTo("en-GB");
        assertThat(testKit.getState().marketingOptIn()).isTrue();
    }

    @Test
    void secondSetReplacesFirst() {
        var testKit = KeyValueEntityTestKit.of("c-1", CustomerPreferencesEntity::new);

        testKit.method(CustomerPreferencesEntity::set)
            .invoke(new SetPreferences("dark", "en-GB", true));
        testKit.method(CustomerPreferencesEntity::set)
            .invoke(new SetPreferences("light", "en-US", false));

        assertThat(testKit.getState().theme()).isEqualTo("light");
        assertThat(testKit.getState().marketingOptIn()).isFalse();
    }
}
