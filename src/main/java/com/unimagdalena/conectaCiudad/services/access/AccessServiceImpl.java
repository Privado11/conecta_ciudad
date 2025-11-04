package com.unimagdalena.conectaCiudad.services.access;

import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.access.AccessMapper;
import com.unimagdalena.conectaCiudad.Dto.access.AccessSaveDto;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.repositories.AccessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessServiceImpl implements AccessService {

    private final AccessRepository accessRepository;
    private final AccessMapper accessMapper;
    


    @Override
    public AccessDto save(AccessSaveDto accessSaveDto) {
        Access access = accessMapper.toEntity(accessSaveDto);
        return accessMapper.toDto(accessRepository.save(access));
    }

    @Override
    public Access findById(Long id) {
        return accessRepository.findById(id)
                .orElse(null);
    }

}
