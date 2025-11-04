package com.unimagdalena.conectaCiudad.services.access;

import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.access.AccessSaveDto;
import com.unimagdalena.conectaCiudad.entities.Access;


public interface AccessService {
    AccessDto save(AccessSaveDto accessSaveDto);
    Access findById(Long id);
}
