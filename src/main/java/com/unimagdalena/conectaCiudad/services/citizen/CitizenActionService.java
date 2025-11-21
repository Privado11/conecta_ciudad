package com.unimagdalena.conectaCiudad.services.citizen;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.CitizenActionRequest;


public interface CitizenActionService {


    ActionDto registerCitizenAction(CitizenActionRequest citizenRequest, String userEmail);
}