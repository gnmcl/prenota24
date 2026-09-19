package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.prenota24.backend.domain.AppointmentAction;
import com.prenota24.backend.domain.AppointmentStatus;

class AppointmentStateMachineTest {

    private final AppointmentStateMachine stateMachine = new AppointmentStateMachine();

    @Test
    void rejectsProposalByReturningAppointmentToRequested() {
        var nextStatus = stateMachine.transition(
                AppointmentStatus.PROPOSED_NEW_TIME,
                AppointmentAction.REJECT_PROPOSAL);

        assertThat(nextStatus).isEqualTo(AppointmentStatus.REQUESTED);
    }
}
