package com.halofuel.fuelbridge.platform.catalog.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests of the rating rules of the ProviderRating aggregate. */
class ProviderRatingTest {

    @Test
    void createsARatingWithinTheAllowedRange() {
        var rating = new ProviderRating(101L, 202L, 5);
        assertEquals(101L, rating.getCompanyId());
        assertEquals(202L, rating.getProviderId());
        assertEquals(5, rating.getRating());
    }

    @Test
    void rejectsRatingsOutsideOneToFiveOrMissing() {
        for (Integer invalid : new Integer[] {null, 0, 6, -1}) {
            var error = assertThrows(IllegalArgumentException.class, () -> new ProviderRating(101L, 202L, invalid));
            assertEquals("Rating must be between 1 and 5", error.getMessage());
        }
    }

    @Test
    void changeRatingUpdatesAValidValue() {
        var rating = new ProviderRating(101L, 202L, 3);
        rating.changeRating(1);
        assertEquals(1, rating.getRating());
    }

    @Test
    void changeRatingKeepsThePreviousValueWhenTheNewOneIsInvalid() {
        var rating = new ProviderRating(101L, 202L, 4);
        assertThrows(IllegalArgumentException.class, () -> rating.changeRating(7));
        assertEquals(4, rating.getRating());
    }
}
