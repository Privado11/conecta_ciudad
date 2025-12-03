package com.unimagdalena.conectaCiudad.services.admin.csv;

import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UserCsvService {

   
    BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException;

   
    byte[] exportUsersToCSV(List<UserDto> users) throws IOException;

    
    byte[] exportAllUsersToCSV() throws IOException;
}
