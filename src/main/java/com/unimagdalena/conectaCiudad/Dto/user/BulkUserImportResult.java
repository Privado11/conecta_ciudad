package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

public record BulkUserImportResult(int totalProcessed,
                                    int successfulImports,
                                    int failedImports,
                                    List<UserImportError> errors,
                                    List<UserDto> importedUsers) {
    
}
