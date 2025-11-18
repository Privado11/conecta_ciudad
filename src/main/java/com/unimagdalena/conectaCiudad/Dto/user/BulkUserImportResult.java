package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

public record BulkUserImportResult(int totalRecords,
                                    int successCount,
                                    int failCount,
                                    List<UserImportError> errors,
                                    List<UserDto> importedUsers) {
    
}
