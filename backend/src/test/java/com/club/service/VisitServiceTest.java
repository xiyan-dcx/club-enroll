package com.club.service;
import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;

public class VisitServiceTest {
    private final LocalDateTime t=LocalDateTime.of(2026,10,24,10,0);
    @Test public void cancellationCanRemainOpenAfterBookingCloses() {
        VisitService.validateWindow(t.plusHours(5),t.plusHours(6),t,t.plusHours(2),t.plusHours(4));
    }
    @Test public void rejectsImpossibleWindows() {
        assertThrows(IllegalArgumentException.class,()->VisitService.validateWindow(t,t.plusHours(1),t.minusHours(2),t.plusHours(1),t));
        assertThrows(IllegalArgumentException.class,()->VisitService.validateWindow(t,t,t.minusHours(2),t.minusHours(1),t));
        assertThrows(IllegalArgumentException.class,()->VisitService.validateWindow(t,t.plusHours(1),t.minusHours(2),t.minusHours(1),t.minusHours(2)));
    }
    @Test public void rejectsMalformedTime() { assertThrows(IllegalArgumentException.class,()->VisitService.time("2026-02-30T09:00:00")); }
}
