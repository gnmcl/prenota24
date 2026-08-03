package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.IllegalStateTransitionException;
import com.prenota24.backend.domain.AppointmentAction;
import com.prenota24.backend.domain.AppointmentStatus;
import org.springframework.stereotype.Component;

@Component
public class AppointmentStateMachine {

    public AppointmentStatus transition(AppointmentStatus current, AppointmentAction action) {
        return switch (action) {
            case CONFIRM ->
                    (current == AppointmentStatus.REQUESTED || current == AppointmentStatus.PROPOSED_NEW_TIME)
                            ? AppointmentStatus.CONFIRMED
                            : invalid(current, action);
            case CANCEL ->
                    (current != AppointmentStatus.CANCELLED && current != AppointmentStatus.COMPLETED && current != AppointmentStatus.NO_SHOW)
                            ? AppointmentStatus.CANCELLED
                            : invalid(current, action);
            case COMPLETE ->
                    current == AppointmentStatus.CONFIRMED
                            ? AppointmentStatus.COMPLETED
                            : invalid(current, action);
            case NO_SHOW ->
                    current == AppointmentStatus.CONFIRMED
                            ? AppointmentStatus.NO_SHOW
                            : invalid(current, action);
            case PROPOSE_NEW_TIME ->
                    (current == AppointmentStatus.CONFIRMED || current == AppointmentStatus.REQUESTED)
                            ? AppointmentStatus.PROPOSED_NEW_TIME
                            : invalid(current, action);
            case ACCEPT_PROPOSAL ->
                    current == AppointmentStatus.PROPOSED_NEW_TIME
                            ? AppointmentStatus.CONFIRMED
                            : invalid(current, action);
            case REJECT_PROPOSAL ->
                    current == AppointmentStatus.PROPOSED_NEW_TIME
                            ? AppointmentStatus.CANCELLED
                            : invalid(current, action);
        };
    }

    private AppointmentStatus invalid(AppointmentStatus current, AppointmentAction action) {
        throw new IllegalStateTransitionException(
                "Transizione non valida: " + current + " → " + action
        );
    }
}
